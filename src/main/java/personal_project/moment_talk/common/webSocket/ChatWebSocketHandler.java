package personal_project.moment_talk.common.webSocket;

import com.vane.badwordfiltering.BadWordFiltering;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import personal_project.moment_talk.chat.service.ChatService;
import personal_project.moment_talk.chat.service.QueueService;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    /*
    QueueService -> 사용자 대기열을 관리하는 서비스, WebSocket 연결이 수립되거나 종료될 때 사용자 대기열에 추가 또는 제거
    ChatService -> 채팅 매칭 로직 담당
     */
    private final QueueService queueService;
    private final ChatService chatService;

    private final BadWordFiltering badWordFiltering;

    // 현재 활성화된 WebSocket 세션 관리.
    private final ConcurrentHashMap<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();

    /*
    클라이언트가 WebSocket 연결을 성공적으로 수립했을 때 호출 -> 사용자를 대기열에 추가하고, 채팅 매칭 시도
    1. sessionMap 에 sessionId PUT
    2. queueService.addToQueue -> 주어진 sessionId 를 Redis 의 큐(리스트)에 추가
    3. scheduleTimeOut(session, sessionId) -> 30초 동안 매칭되지 않을 경우 대기열에서 제거하고, 클라이언트에 "NO_MATCH_FOUND" 메시지 전송

    4. try {
        1. 매칭 시도
        2. 상대 sessionId 가 null 이 아니면 -> MATCH_SUCCESS 메시지 전송
        3. sessionMap 에서 상대 sessionId GET
        4. 상대 세션에도 MATCH_SUCCESS 메시지 전송
     */

    /*
    매칭 찾기 흐름
    A사용자가 매칭찾기 눌렀을 때에는 아무도 없으니깐 1초만 찾아보고 30초 되기전까지 대기인거고,
    다음에 B사용자가 매칭찾기를 눌렀을 때 대기열에 A사용자가 남아있으니깐 둘이 매칭
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessionMap.put(sessionId, session);
        queueService.addToQueue(sessionId);
        log.info("New connection: {}", sessionId);

        // 타임아웃 스케줄링
        scheduleTimeout(session, sessionId);

        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            try {
                String opponentUser = chatService.attemptChatMatch(sessionId);
                if (opponentUser != null) {
                    sendMessageSafely(session, "MATCH_SUCCESS:" + opponentUser);
                    WebSocketSession opponentSession = sessionMap.get(opponentUser);
                    sendMessageSafely(opponentSession, "MATCH_SUCCESS:" + sessionId);
                    log.info("Matched: {} with {}", sessionId, opponentUser);
                } else {
                    sendMessageSafely(session, "WAITING_FOR_MATCH");
                    log.info("Still waiting for a match: {}", sessionId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 1, TimeUnit.SECONDS);
    }



    /*
    30초 동안 매칭되지 않을 경우 대기열에서 제거하고, 클라이언트에 "NO_MATCH_FOUND" 메시지 전송
    Executors.newSingleThreadScheduledExecutor().schedule -> 하나의 스레드로 작업을 스케줄링할 수 있는 스레드 풀 생성
    try {
        만약 WebSocketSession 이 열려있지 않다면 -> return
        WebSocketSession 이 열려있으면 ->
        매치 해쉬맵에서 사용자 sessionId 로 검색했을 때 null 이고 && 사용자의 sessionId 가 Redis 의 큐에 존재하면
        ->
        1. Redis 의 큐에서 사용자 세션 아이디 제거
        2. WebSocketSession 에 NO_MATCH_FOUND 메시지 전송
        3. WebSocketSession 종료

        }

    .schedule(() -> 작업 }, 30, TimeUnit.SECONDS) -> 일정 시간이 지난 후 작업 실행

     */
    private void scheduleTimeout(WebSocketSession session, String sessionId) {
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            try {
                if (!session.isOpen()) {
                    log.info("Session already closed: {}", sessionId);
                    return;
                }

                if (chatService.getOpponentUser(sessionId) == null && queueService.isInQueue(sessionId)) {
                    queueService.removeFromQueue(sessionId); // 대기열에서 제거
                    session.sendMessage(new TextMessage("NO_MATCH_FOUND"));
                    session.close();
                    log.info("No match found for session: {}", sessionId);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, 30, TimeUnit.SECONDS); // 30초 타임아웃
    }

    /*
    WebSocket 세션에 메시지를 안전하게 전송하는 역할
    1. WebSocketSession 이 null 이 아니고, open 되어 있으면 -> session 에 WebSocket 프로토콜을 통해 문자열 메시지 전송
     */
    private synchronized void sendMessageSafely(WebSocketSession session, String message) {
        try {
            if (session != null && session.isOpen()) {
                log.info("Sending message to session: {} Message: {}", session.getId(), message);
                session.sendMessage(new TextMessage(message));
            } else {
                log.info("Failed to send message. Session is closed or null: " + (session != null ? session.getId() : "null"));
            }
        } catch (Exception e) {
            System.out.println("Error while sending message to session: " + (session != null ? session.getId() : "null"));
            e.printStackTrace();
        }
    }


    /*
    클라이언트로부터 WebSocket 메시지를 수신했을 때 호출 -> 메시지를 상대방에게 전달하거나, 상대방 세션이 닫힌 경우 처리

    1. 상재 유저가 null 이 아니고,
    2. 상대 세션이 null 이 아니고, 세션이 열려있으면 -> 메시지 전송
    3. 세션이 null 이거나, 세션이 닫혀있으면 -> activeMatches 에서 match 제거
    4. 대기열에 추가 -> 이거 없어도 될 것 같은데
     */
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String sessionId = session.getId();
        String opponentUser = chatService.getOpponentUser(sessionId);

        if (opponentUser != null) {
            WebSocketSession opponentSession = sessionMap.get(opponentUser);
            if (opponentSession != null && opponentSession.isOpen()) {
                if (!badWordFiltering.check(message.getPayload().toString())) {
                    sendMessageSafely(opponentSession, message.getPayload().toString());
                } else {
                    sendMessageSafely(opponentSession, "*****");
                }

            } else {
                log.info("Opponent session is closed. Removing match.");
                chatService.removeMatch(sessionId);
                queueService.addToQueue(sessionId);
                sendMessageSafely(session, "Opponent disconnected. Waiting for a new match...");
            }
        } else {
            sendMessageSafely(session, "Waiting for a match...");
        }
    }

    /*
    클라이언트와 WebSocket 연결이 종료되었을 때 호출 -> 세션 정보 삭제, 상대방에게 연결 종료 메시지 전송
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        sessionMap.remove(sessionId);
        queueService.removeFromQueue(sessionId);

        String opponentUser = chatService.getOpponentUser(sessionId);
        chatService.removeMatch(sessionId);

        if (opponentUser != null) {
            WebSocketSession opponentSession = sessionMap.get(opponentUser);
            sendMessageSafely(opponentSession, "USER_DISCONNECTED");
            log.info("Notified opponent about disconnection: {}", opponentUser);

            // 상대방을 대기열에 다시 추가
            queueService.addToQueue(opponentUser);
        }
        log.info("Session closed: {}", sessionId);
    }







}

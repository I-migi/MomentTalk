package personal_project.moment_talk.common.webSocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vane.badwordfiltering.BadWordFiltering;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import personal_project.moment_talk.chat.service.ChatService;
import personal_project.moment_talk.chat.service.QueueService;
import personal_project.moment_talk.user.repository.UserRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
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
    private final WebSocketSessionManager webSocketSessionManager;
    private final BadWordFiltering badWordFiltering;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

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
    public void afterConnectionEstablished(WebSocketSession webSocketSession) throws Exception {
        String httpSessionId = (String) webSocketSession.getAttributes().get("HTTP_SESSION_ID");
        webSocketSessionManager.addSession(httpSessionId, webSocketSession);
        queueService.addToQueue(httpSessionId);
        log.info("New connection: {}", httpSessionId);

        // 타임아웃 스케줄링
        scheduleTimeout(webSocketSession, httpSessionId);

        tryChatMatch(webSocketSession, httpSessionId);
    }

    private void tryChatMatch(WebSocketSession webSocketSession, String httpSessionId) {
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            try {
                String opponentHttpSessionId = chatService.attemptChatMatch(httpSessionId);
                if (opponentHttpSessionId != null) {
                    String userName = userRepository.findBySessionId(httpSessionId).get().getUserName();
                    String opponentName = userRepository.findBySessionId(opponentHttpSessionId).get().getUserName();
                    webSocketSession.sendMessage(new TextMessage("MATCH_SUCCESS:" + opponentHttpSessionId));
                    WebSocketSession opponentWebSocketSession = webSocketSessionManager.getSession(opponentHttpSessionId);
                    opponentWebSocketSession.sendMessage(new TextMessage("MATCH_SUCCESS:" + httpSessionId));
                    opponentWebSocketSession.sendMessage(new TextMessage(userName + "님이 채팅에 참가했습니다!"));
                    webSocketSession.sendMessage(new TextMessage(opponentName + "님이 채팅에 참가했습니다!"));
                    log.info("Matched: {} with {}", httpSessionId, opponentHttpSessionId);
                } else {
                    webSocketSession.sendMessage(new TextMessage("WAITING_FOR_MATCH"));
                    log.info("Still waiting for a match: {}", httpSessionId);
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
    private void scheduleTimeout(WebSocketSession webSocketSession, String httpSessionId) {
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            try {
                if (!webSocketSession.isOpen() || chatService.getOpponentHttpSessionId(httpSessionId) != null) {
                    return;
                }
                if (queueService.isInQueue(httpSessionId)) {
                    queueService.removeFromQueue(httpSessionId); // 대기열에서 제거
                    webSocketSession.sendMessage(new TextMessage("NO_MATCH_FOUND"));
                    webSocketSession.close();
                    log.info("No match found for session: {}", httpSessionId);
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
    private synchronized void sendMessageSafely(WebSocketSession webSocketSession, String message, String userName) throws IOException {

            if (webSocketSession != null && webSocketSession.isOpen()) {
                Map<String, Object> finalMessage = new HashMap<>();
                finalMessage.put("userName", userName);
                finalMessage.put("timestamp", Instant.now().toString());

                // message 를 JSON 으로 파싱
                Map<String, Object> messageMap = objectMapper.readValue(message, Map.class);
                String type = (String) messageMap.get("type");

                if ("file".equals(type)) {
                    // 파일 메시지 구조
                    finalMessage.put("type", "file");
                    finalMessage.put("fileName", messageMap.get("fileName"));
                    finalMessage.put("fileType", messageMap.get("fileType"));
                    finalMessage.put("size", messageMap.get("size"));
                } else {
                    // 텍스트 메시지 구조
                    finalMessage.put("type", "text");
                    finalMessage.put("content", messageMap.get("content"));
                }

                // JSON 직렬화 후 전송
                String jsonMessage = objectMapper.writeValueAsString(finalMessage);
                log.info("Sending JSON message: {}", jsonMessage);
                webSocketSession.sendMessage(new TextMessage(jsonMessage));
            } else {
                log.info("Failed to send message. Session is closed or null: " + (webSocketSession != null ? webSocketSession.getId() : "null"));
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
    public void handleMessage(WebSocketSession webSocketSession, WebSocketMessage<?> message) throws Exception {
        String httpSessionId = (String) webSocketSession.getAttributes().get("HTTP_SESSION_ID");
        String opponentHttpSessionId = chatService.getOpponentHttpSessionId(httpSessionId);
        WebSocketSession opponentWebSocketSession = webSocketSessionManager.getSession(opponentHttpSessionId);
        String userName = userRepository.findBySessionId(httpSessionId).get().getUserName();

        if (opponentHttpSessionId != null && opponentWebSocketSession.isOpen()) {
            if (message instanceof TextMessage) {
                handleTextMessage(webSocketSession, message, opponentWebSocketSession, userName);
            } else if (message instanceof BinaryMessage) {
                handleBinaryMessage(webSocketSession, (BinaryMessage) message, opponentWebSocketSession);
            }
        } else {
            opponentSessionClosed(webSocketSession, httpSessionId);
        }
    }

    private void opponentSessionClosed(WebSocketSession webSocketSession, String httpSessionId) throws IOException {

        chatService.removeMatch(httpSessionId);
        queueService.addToQueue(httpSessionId);
        webSocketSession.sendMessage(new TextMessage("Opponent disconnected. Waiting for a new match..."));
    }

    private static void handleBinaryMessage(WebSocketSession session, BinaryMessage message, WebSocketSession opponentSession) throws IOException {
        try {
            opponentSession.sendMessage(message); // 바이너리 데이터 전달
        } catch (Exception e) {
            log.error("Error processing binary message:", e);
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    private void handleTextMessage(WebSocketSession session, WebSocketMessage<?> message, WebSocketSession opponentSession, String userName) throws IOException {
        if (!badWordFiltering.check(message.getPayload().toString())) {
            String payload = message.getPayload().toString();
            sendMessageSafely(opponentSession, payload, userName);
        } else {
            opponentSession.sendMessage(new TextMessage("*******"));

        }
    }

    /*
    클라이언트와 WebSocket 연결이 종료되었을 때 호출 -> 세션 정보 삭제, 상대방에게 연결 종료 메시지 전송
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String httpSessionId = session.getAttributes().get("HTTP_SESSION_ID").toString();
        webSocketSessionManager.removeSession(httpSessionId);
        queueService.removeFromQueue(httpSessionId);

        String opponentHttpSessionId = chatService.getOpponentHttpSessionId(httpSessionId);
        chatService.removeMatch(opponentHttpSessionId);

        if (opponentHttpSessionId != null) {
            WebSocketSession opponentSession = webSocketSessionManager.getSession(opponentHttpSessionId);
            opponentSession.sendMessage(new TextMessage("상대방이 채팅에서 떠났습니다.."));
            log.info("Notified opponent about disconnection: {}", opponentHttpSessionId);

            // 상대방을 대기열에 다시 추가
            queueService.addToQueue(opponentHttpSessionId);
        }
        log.info("Session closed: {}", httpSessionId);
    }


}

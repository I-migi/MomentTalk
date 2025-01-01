package personal_project.moment_talk.common.webSocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vane.badwordfiltering.BadWordFiltering;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import personal_project.moment_talk.chat.service.ChatService;
import personal_project.moment_talk.chat.service.QueueService;
import personal_project.moment_talk.common.redis.GroupChatParticipants;
import personal_project.moment_talk.user.repository.UserRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {


    private final QueueService queueService;
    private final ChatService chatService;
    private final WebSocketSessionManager webSocketSessionManager;
    private final BadWordFiltering badWordFiltering;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final GroupChatParticipants groupChatParticipants;


    @Override
    public void afterConnectionEstablished(WebSocketSession webSocketSession) throws Exception {
        String path = webSocketSession.getUri().getPath();
        if (path.startsWith("/ws/connect")) {
            handleOneToOneConnection(webSocketSession);
        } else if (path.startsWith("/ws/group")) {
            String httpSessionId = getHttpSessionIdFromWebSocketSession(webSocketSession);
            webSocketSessionManager.addSession(httpSessionId, webSocketSession);
        }
    }

    private static String getHttpSessionIdFromWebSocketSession(WebSocketSession webSocketSession) {
        return (String) webSocketSession.getAttributes().get("HTTP_SESSION_ID");
    }

    private void handleOneToOneConnection(WebSocketSession webSocketSession) {

        String httpSessionId = getHttpSessionIdFromWebSocketSession(webSocketSession);
        webSocketSessionManager.addSession(httpSessionId, webSocketSession);
        queueService.addToQueue(httpSessionId);

        scheduleTimeout(webSocketSession, httpSessionId);

        tryChatMatch(webSocketSession, httpSessionId);
    }

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

    private void tryChatMatch(WebSocketSession webSocketSession, String httpSessionId) {
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            try {
                String opponentHttpSessionId = chatService.attemptChatMatch(httpSessionId);
                if (opponentHttpSessionId != null) {
                    initOneToOneChat(webSocketSession, httpSessionId, opponentHttpSessionId);

                } else {
                    webSocketSession.sendMessage(new TextMessage("WAITING_FOR_MATCH"));
                    log.info("Still waiting for a match: {}", httpSessionId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 1, TimeUnit.SECONDS);
    }

    private void initOneToOneChat(WebSocketSession webSocketSession, String httpSessionId, String opponentHttpSessionId) throws IOException {
        String userName = userRepository.findBySessionId(httpSessionId).get().getUserName();
        String opponentName = userRepository.findBySessionId(opponentHttpSessionId).get().getUserName();

        webSocketSession.sendMessage(new TextMessage("MATCH_SUCCESS:" + opponentHttpSessionId));

        WebSocketSession opponentWebSocketSession = webSocketSessionManager.getSession(opponentHttpSessionId);
        opponentWebSocketSession.sendMessage(new TextMessage("MATCH_SUCCESS:" + httpSessionId));

        opponentWebSocketSession.sendMessage(new TextMessage(userName + "님이 채팅에 참가했습니다!"));
        webSocketSession.sendMessage(new TextMessage(opponentName + "님이 채팅에 참가했습니다!"));

        log.info("Matched: {} with {}", httpSessionId, opponentHttpSessionId);
    }

    @Override
    public void handleMessage(WebSocketSession webSocketSession, WebSocketMessage<?> message) throws Exception {

        String httpSessionId = getHttpSessionIdFromWebSocketSession(webSocketSession);
        String userName = userRepository.findBySessionId(httpSessionId).get().getUserName();
        String path = webSocketSession.getUri().getPath();

        if (path.startsWith("/ws/connect")) {
            handleOneToOneMessage(webSocketSession, message, httpSessionId, userName);
        } else if (path.startsWith("/ws/group")) {
            handleGroupMessage(webSocketSession, message, path, httpSessionId, userName);

        }
    }

    private void handleOneToOneMessage(WebSocketSession webSocketSession, WebSocketMessage<?> message, String httpSessionId, String userName) throws IOException {
        String opponentHttpSessionId = chatService.getOpponentHttpSessionId(httpSessionId);
        WebSocketSession opponentWebSocketSession = webSocketSessionManager.getSession(opponentHttpSessionId);

        if (opponentHttpSessionId != null && opponentWebSocketSession.isOpen()) {
            if (message instanceof TextMessage) {
                handleTextMessageOneToOne(webSocketSession, message, opponentWebSocketSession, userName);
            } else if (message instanceof BinaryMessage) {
                handleBinaryMessageOneToOne(webSocketSession, (BinaryMessage) message, opponentWebSocketSession);
            }
        } else {
            opponentSessionClosed(webSocketSession, httpSessionId);
        }
    }

    private void handleGroupMessage(WebSocketSession webSocketSession, WebSocketMessage<?> message, String path, String httpSessionId, String userName) throws IOException {

        String[] segments = path.split("/");
        String roomId = segments[segments.length - 1];

        Set<Object> participantHttpSessionIds = groupChatParticipants.getParticipants(roomId);
        participantHttpSessionIds.remove(httpSessionId);

        List<WebSocketSession> participantWebSocketSessions = new ArrayList<>();
        for (Object participantSessionId : participantHttpSessionIds) {
            WebSocketSession participantWebSocketSession = webSocketSessionManager.getSession((String) participantSessionId);
            if (participantWebSocketSession != null) {
                participantWebSocketSessions.add(participantWebSocketSession);
            }
        }

        if (message instanceof TextMessage) {
            handleTextMessageGroup(message, userName, participantWebSocketSessions);
        } else if (message instanceof BinaryMessage) {
            handleBinaryMessageGroup(webSocketSession, message, participantWebSocketSessions);
        }
    }

    private static void handleBinaryMessageOneToOne(WebSocketSession session, BinaryMessage message, WebSocketSession opponentSession) throws IOException {
        try {
            opponentSession.sendMessage(message); // 바이너리 데이터 전달
        } catch (Exception e) {
            log.error("Error processing binary message:", e);
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    private void handleTextMessageOneToOne(WebSocketSession session, WebSocketMessage<?> message, WebSocketSession opponentSession, String userName) throws IOException {
        if (!badWordFiltering.check(message.getPayload().toString())) {
            String payload = message.getPayload().toString();
            sendMessageSafely(opponentSession, payload, userName);
        } else {
            opponentSession.sendMessage(new TextMessage("*******"));

        }
    }

    private static void handleBinaryMessageGroup(WebSocketSession webSocketSession, WebSocketMessage<?> message, List<WebSocketSession> participantWebSocketSessions) throws IOException {
        try {

            for (WebSocketSession opponentWebSocketSession : participantWebSocketSessions) {
                opponentWebSocketSession.sendMessage(message);
            }
        } catch (Exception e) {
            log.error("Error processing binary message:", e);
            webSocketSession.close(CloseStatus.SERVER_ERROR);
        }
    }

    private void handleTextMessageGroup(WebSocketMessage<?> message, String userName, List<WebSocketSession> participantWebSocketSessions) throws IOException {
        if (!badWordFiltering.check(message.getPayload().toString())) {
            String payload = message.getPayload().toString();
            for (WebSocketSession opponentWebSocketSession : participantWebSocketSessions) {
                sendMessageSafely(opponentWebSocketSession, payload, userName);
            }
        } else {
            for (WebSocketSession opponentWebSocketSession : participantWebSocketSessions) {
                opponentWebSocketSession.sendMessage(new TextMessage("*******"));
            }
        }
    }


    private synchronized void sendMessageSafely(WebSocketSession webSocketSession, String message, String userName) throws IOException {

            if (webSocketSession != null && webSocketSession.isOpen()) {
                Map<String, Object> finalMessage = new HashMap<>();
                finalMessage.put("userName", userName);
                finalMessage.put("timestamp", Instant.now().toString());

                Map<String, Object> messageMap = objectMapper.readValue(message, Map.class);
                String type = (String) messageMap.get("type");

                if ("file".equals(type)) {

                    finalMessage.put("type", "file");
                    finalMessage.put("fileName", messageMap.get("fileName"));
                    finalMessage.put("fileType", messageMap.get("fileType"));
                    finalMessage.put("size", messageMap.get("size"));
                } else {

                    finalMessage.put("type", "text");
                    finalMessage.put("content", messageMap.get("content"));
                }

                String jsonMessage = objectMapper.writeValueAsString(finalMessage);
                log.info("Sending JSON message: {}", jsonMessage);
                webSocketSession.sendMessage(new TextMessage(jsonMessage));
            } else {
                log.info("Failed to send message. Session is closed or null: " + (webSocketSession != null ? webSocketSession.getId() : "null"));
            }
    }

    private void opponentSessionClosed(WebSocketSession webSocketSession, String httpSessionId) throws IOException {

        chatService.removeMatch(httpSessionId);
        queueService.addToQueue(httpSessionId);
        webSocketSession.sendMessage(new TextMessage("Opponent disconnected. Waiting for a new match..."));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession webSocketSession, CloseStatus status) throws Exception {

        String path = webSocketSession.getUri().getPath();
        String httpSessionId = getHttpSessionIdFromWebSocketSession(webSocketSession);
        webSocketSessionManager.removeSession(httpSessionId);

        if (path.startsWith("/ws/connect")) {
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

}

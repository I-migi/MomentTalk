package personal_project.moment_talk.common.webSocket;

import com.vane.badwordfiltering.BadWordFiltering;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import personal_project.moment_talk.chat.service.ChatService;
import personal_project.moment_talk.chat.service.QueueService;
import personal_project.moment_talk.user.repository.UserRepository;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OneToOneWebSocketHelper {

    private final QueueService queueService;
    private final ChatService chatService;
    private final WebSocketSessionManager webSocketSessionManager;
    private final BadWordFiltering badWordFiltering;
    private final UserRepository userRepository;
    private final WebSocketHelper webSocketHelper;

    public void handleOneToOneMessage(WebSocketSession webSocketSession, WebSocketMessage<?> message, String httpSessionId, String userName) throws IOException {
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
            webSocketHelper.sendMessageSafely(opponentSession, payload, userName);
        } else {
            opponentSession.sendMessage(new TextMessage("*******"));

        }
    }

    private void opponentSessionClosed(WebSocketSession webSocketSession, String httpSessionId) throws IOException {

        chatService.removeMatch(httpSessionId);
        queueService.addToQueue(httpSessionId);
        webSocketSession.sendMessage(new TextMessage("Opponent disconnected. Waiting for a new match..."));
    }

    public void handleOneToOneConnection(WebSocketSession webSocketSession) {

        String httpSessionId = webSocketHelper.getHttpSessionIdFromWebSocketSession(webSocketSession);
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

}

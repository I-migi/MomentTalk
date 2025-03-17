package personal_project.moment_talk.common.webSocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import personal_project.moment_talk.chat.service.ChatService;
import personal_project.moment_talk.chat.service.QueueService;
import personal_project.moment_talk.user.repository.UserRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final QueueService queueService;
    private final ChatService chatService;
    private final WebSocketSessionManager webSocketSessionManager;
    private final UserRepository userRepository;
    private final OneToOneWebSocketHelper oneToOneWebSocketHelper;
    private final GroupWebSocketHelper groupWebSocketHelper;
    private final WebSocketHelper webSocketHelper;


    @Override
    public void afterConnectionEstablished(WebSocketSession webSocketSession) throws Exception {
        String path = webSocketSession.getUri().getPath();
        if (path.startsWith("/ws/connect")) {
            oneToOneWebSocketHelper.handleOneToOneConnection(webSocketSession);
        } else if (path.startsWith("/ws/group") || path.startsWith("/ws/music-game")) {
            String httpSessionId = webSocketHelper.getHttpSessionIdFromWebSocketSession(webSocketSession);
            webSocketSessionManager.addSession(httpSessionId, webSocketSession);
        }
    }

    @Override
    public void handleMessage(WebSocketSession webSocketSession, WebSocketMessage<?> message) throws Exception {

        String httpSessionId = webSocketHelper.getHttpSessionIdFromWebSocketSession(webSocketSession);
        String userName = userRepository.findBySessionId(httpSessionId).get().getUserName();
        String path = webSocketSession.getUri().getPath();

        if (path.startsWith("/ws/connect")) {
            oneToOneWebSocketHelper.handleOneToOneMessage(webSocketSession, message, httpSessionId, userName);
        } else if (path.startsWith("/ws/group")){
            groupWebSocketHelper.handleGroupMessage(webSocketSession, message, path, httpSessionId, userName);
        } else if (path.startsWith("/ws/music-game")) {
            groupWebSocketHelper.handleMusicGameMessage(webSocketSession, message, path, httpSessionId, userName);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession webSocketSession, CloseStatus status) throws Exception {

        String path = webSocketSession.getUri().getPath();
        String httpSessionId = webSocketHelper.getHttpSessionIdFromWebSocketSession(webSocketSession);
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

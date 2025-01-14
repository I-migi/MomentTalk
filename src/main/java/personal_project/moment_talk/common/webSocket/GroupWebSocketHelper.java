package personal_project.moment_talk.common.webSocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vane.badwordfiltering.BadWordFiltering;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import personal_project.moment_talk.common.redis.GroupChatParticipants;

import java.io.IOException;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class GroupWebSocketHelper {

    private final WebSocketSessionManager webSocketSessionManager;
    private final BadWordFiltering badWordFiltering;
    private final GroupChatParticipants groupChatParticipants;
    private final WebSocketHelper webSocketHelper;
    private final ObjectMapper objectMapper;

    private void handleTextMessageGroup(WebSocketMessage<?> message, String userName, List<WebSocketSession> participantWebSocketSessions) throws IOException {
        String payload = message.getPayload().toString();

        if (!badWordFiltering.check(payload)) {
            // 금칙어가 없는 경우 정상 메시지 전송
            for (WebSocketSession opponentWebSocketSession : participantWebSocketSessions) {
                webSocketHelper.sendMessageSafely(opponentWebSocketSession, payload, userName);
            }
        } else {
            // 금칙어가 있는 경우 "*******"를 JSON 형식으로 전송
            Map<String, Object> filteredMessage = new HashMap<>();
            filteredMessage.put("type", "text");
            filteredMessage.put("content", "*******"); // 금칙어 대체 문자열
            filteredMessage.put("userName", userName);

            String jsonMessage = objectMapper.writeValueAsString(filteredMessage);

            for (WebSocketSession opponentWebSocketSession : participantWebSocketSessions) {
                opponentWebSocketSession.sendMessage(new TextMessage(jsonMessage));
                log.info("Filtered message sent: {}", jsonMessage);
            }
        }
    }

    public void handleGroupMessage(WebSocketSession webSocketSession, WebSocketMessage<?> message, String path, String httpSessionId, String userName) throws IOException {

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
}

package personal_project.moment_talk.common.webSocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vane.badwordfiltering.BadWordFiltering;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import personal_project.moment_talk.common.redis.GroupChatParticipants;
import personal_project.moment_talk.user.repository.UserRepository;

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
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;

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

    public void handleMusicGameMessage(WebSocketSession webSocketSession, WebSocketMessage<?> message, String path, String httpSessionId, String userName) throws IOException {

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
        handleTextMessageGroup(message, userName, participantWebSocketSessions);

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

    public void handleGroupConnection(WebSocketSession webSocketSession) throws IOException {
        String httpSessionId = webSocketHelper.getHttpSessionIdFromWebSocketSession(webSocketSession);
        String userName = userRepository.findBySessionId(httpSessionId).get().getUserName();

        // redis 에서 해당 채팅방의 모든 유저들의 webSocketSession 리스트를 구해서 메시지 전송
        String roomId = (String) redisTemplate.opsForHash().get("chat:session_to_room", httpSessionId);
        if (roomId != null) {
            roomId = roomId.replace("\"","");
            Set<Object> members = redisTemplate.opsForSet().members("chat:room:" + roomId + ":participants");
            List<Object> participants = new ArrayList<>(members);

                for (Object participant : participants) {
                    if (!participant.equals(httpSessionId)) {
                        WebSocketSession participantWebsocketSession = webSocketSessionManager.getSession((String) participant);
                        if (participantWebsocketSession != null) {
                            participantWebsocketSession.sendMessage(new TextMessage(""));

                            Map<String, Object> message = new HashMap<>();
                            message.put("type", "text");
                            message.put("content", userName + " 이 입장했습니다"); // 금칙어 대체 문자열
                            message.put("userName", userName);
                            String jsonMessage = objectMapper.writeValueAsString(message);
                            log.info("Filtered message sent: {}", jsonMessage);

                            participantWebsocketSession.sendMessage(new TextMessage(jsonMessage));
                        }
                }
            }

        }


    }
}

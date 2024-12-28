package personal_project.moment_talk.common.webSocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vane.badwordfiltering.BadWordFiltering;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import personal_project.moment_talk.common.redis.GroupChatParticipants;
import personal_project.moment_talk.user.repository.UserRepository;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class GroupChatWebSocketHandler extends TextWebSocketHandler {

    private final GroupChatParticipants groupChatParticipants;
    private final WebSocketSessionManager webSocketSessionManager;
    private final BadWordFiltering badWordFiltering;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public void handleCreateRoom(String roomId, String roomName) {
        groupChatParticipants.createGroupChatRoom(roomId, roomName);
    }

    private void brodCastMessage(String roomId, String message) {
        Set<Object> participants = groupChatParticipants.getParticipants(roomId);

        for (Object participantHttpSessionId : participants) {
            WebSocketSession webSocketSession = webSocketSessionManager.getSession((String)participantHttpSessionId);
            try {
                if (webSocketSession.isOpen()) {
                    webSocketSession.sendMessage(new TextMessage(message));
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
    }

    public void handleJoinRoom(String roomId, String httpSessionId) {
        groupChatParticipants.addParticipantToGroupChatRoom(roomId, httpSessionId);
        brodCastMessage(roomId, "User joined: " + httpSessionId);
    }

    public void handleLeaveRoom(String roomId, String httpSessionId) {
        groupChatParticipants.removeParticipant(roomId, httpSessionId);
        brodCastMessage(roomId, "User left: " + httpSessionId);
    }


}

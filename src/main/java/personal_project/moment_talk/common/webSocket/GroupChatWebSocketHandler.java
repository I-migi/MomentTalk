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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    //TODO: 생성을 하고 해당 채팅방 페이지로 이동, 해당 그룹 채팅방의 웹소켓 배열에 사용자의 웹소켓 ID 추가 필요.
    public void handleCreateRoom(String roomName, String httpSessionId) {
        groupChatParticipants.createGroupChatRoom(roomName, httpSessionId);
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

    //TODO: 해당 채팅방 페이지로 이동, 해당 그룹 채팅방에 해당하는 웹소켓 배열에 사용자의 웹소켓 ID 추가
    public void handleJoinRoom(String roomId, String httpSessionId) {
        groupChatParticipants.addParticipantToGroupChatRoom(roomId, httpSessionId);
        brodCastMessage(roomId, "User joined: " + httpSessionId);
    }

    //TODO: 해당 채팅방 페이지에서 나오고, 그룹 Chat 페이지로 이동, 해당 웹소켓 배열에서 사용자의 웹소켓 ID 제거
    public void handleLeaveRoom(String roomId, String httpSessionId) {
        groupChatParticipants.removeParticipant(roomId, httpSessionId);
        brodCastMessage(roomId, "User left: " + httpSessionId);
    }


}

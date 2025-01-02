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

    public void handleCreateRoom(String roomName, String httpSessionId) {
        groupChatParticipants.createGroupChatRoom(roomName, httpSessionId);
    }

    private void brodCastMessage(String roomId, String messageType, String messageContent) {
        Set<Object> participants = groupChatParticipants.getParticipants(roomId);

        for (Object participantHttpSessionId : participants) {
            WebSocketSession webSocketSession = webSocketSessionManager.getSession((String) participantHttpSessionId);
            try {
                if (webSocketSession.isOpen()) {
                    // 메시지를 JSON 형식으로 변환
                    Map<String, Object> messageMap = new HashMap<>();
                    messageMap.put("type", messageType); // 메시지 유형 (예: "user-joined", "user-left")
                    messageMap.put("content", messageContent); // 메시지 내용
                    messageMap.put("roomId", roomId); // 방 ID
                    messageMap.put("timestamp", System.currentTimeMillis()); // 타임스탬프 (선택)

                    String jsonMessage = new ObjectMapper().writeValueAsString(messageMap);
                    webSocketSession.sendMessage(new TextMessage(jsonMessage));
                }
            } catch (Exception e) {
                log.info("아무 회원도 존재하지 않습니다");
            }
        }
    }

    // 사용자 입장 처리
    public void handleJoinRoom(String roomId, String httpSessionId) {
        groupChatParticipants.addParticipantToGroupChatRoom(roomId, httpSessionId);
    }

    // 사용자 퇴장 처리
    public void handleLeaveRoom(String roomId, String httpSessionId) {
        groupChatParticipants.removeParticipant(roomId, httpSessionId);
    }


}

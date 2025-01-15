package personal_project.moment_talk.common.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class GroupChatParticipants {

    private final RedisTemplate<String , Object> redisTemplate;
    private static final String ROOM_KEY = "chat:rooms";
    private static final String PARTICIPANTS_KEY_PREFIX = "chat:room:";
    private static final String PRIVATE_ROOM_ID = "chat:session_to_room";

    // 그룹 채팅 생성
    public void createGroupChatRoom(String roomName, String httpSessionId) {
        String roomId = UUID.randomUUID().toString(); // 방 ID 생성
        redisTemplate.opsForHash().put(ROOM_KEY, roomName, roomId); // Redis에 저장
        redisTemplate.expire(PARTICIPANTS_KEY_PREFIX + roomId + ":participants", 1, TimeUnit.DAYS);

        List<String> participants = new ArrayList<>();
        participants.add(httpSessionId);

        redisTemplate.opsForHash().put(PRIVATE_ROOM_ID, httpSessionId, roomId);
    }

    // 그룹 채팅에서 나가기
    public boolean leaveGroupChatRoom(String httpSessionId) {
        String roomId = (String) redisTemplate.opsForHash().get("chat:session_to_room", httpSessionId);
        if (roomId == null || roomId.isEmpty()) {
            return false;
        }
        removeParticipant(roomId, httpSessionId);
        return true;
        }

    private void removeParticipant(String roomId, String httpSessionId) {
        String key = PARTICIPANTS_KEY_PREFIX + roomId + ":participants";
        redisTemplate.opsForSet().remove(key, httpSessionId);
        redisTemplate.opsForHash().delete(PRIVATE_ROOM_ID, httpSessionId);

        if (Boolean.TRUE.equals(redisTemplate.opsForSet().size(key) == 0)) {
            redisTemplate.delete(key);

            Map<Object, Object> entries = redisTemplate.opsForHash().entries(ROOM_KEY);
            for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                if (roomId.equals(entry.getValue())) {
                    redisTemplate.opsForHash().delete(ROOM_KEY, entry.getKey());
                    break;
                }
            }

            redisTemplate.opsForHash().delete(ROOM_KEY, roomId);
        }
    }

    // 그룹 채팅 참가
    public void joinGroupChatRoom(String roomId, String httpSessionId) {
        redisTemplate.opsForSet().add(PARTICIPANTS_KEY_PREFIX + roomId + ":participants", httpSessionId);
        redisTemplate.opsForHash().put(PRIVATE_ROOM_ID, httpSessionId, roomId);
    }

    public Set<Object> getParticipants(String roomId) {
        return redisTemplate.opsForSet().members(PARTICIPANTS_KEY_PREFIX + roomId + ":participants");
    }

    public  List<Map<String, String>> getAllGroupChatRooms() {
        Map<Object, Object> rooms = redisTemplate.opsForHash().entries(ROOM_KEY);

        List<Map<String, String>> result = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : rooms.entrySet()) {
            String roomName = (String) entry.getKey();
            String roomId = (String) entry.getValue();

            Map<String, String> roomData = new HashMap<>();
            roomData.put("id", roomId);
            roomData.put("name", roomName);

            result.add(roomData);
        }
        return result;
    }
}

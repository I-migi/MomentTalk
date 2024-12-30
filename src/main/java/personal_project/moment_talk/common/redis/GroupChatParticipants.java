package personal_project.moment_talk.common.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class GroupChatParticipants {

    private final RedisTemplate<String , Object> redisTemplate;
    private static final String ROOM_KEY = "chat:rooms";
    private static final String PARTICIPANTS_KEY_PREFIX = "chat:room:";

    public void createGroupChatRoom(String roomId, String roomName) {
        redisTemplate.opsForHash().put(ROOM_KEY, roomId, roomName);
        redisTemplate.expire(PARTICIPANTS_KEY_PREFIX + roomId + ":participants", 1, TimeUnit.DAYS);
    }

    public void deleteGroupChatRoom(String roomId) {
        redisTemplate.opsForHash().delete(ROOM_KEY, roomId);
        redisTemplate.delete(PARTICIPANTS_KEY_PREFIX + roomId + ":participants");
    }

    public void removeParticipant(String roomId, String httpSessionId) {
        redisTemplate.opsForSet().remove(PARTICIPANTS_KEY_PREFIX + roomId + ":participants", httpSessionId);
    }


    public void addParticipantToGroupChatRoom(String roomId, String httpSessionId) {
        redisTemplate.opsForSet().add(PARTICIPANTS_KEY_PREFIX + roomId + ":participants", httpSessionId);
    }

    public Set<Object> getParticipants(String roomId) {
        return redisTemplate.opsForSet().members(PARTICIPANTS_KEY_PREFIX + roomId + ":participants");
    }

    public Map<String, Map<String, String>> getAllGroupChatRooms() {
        Map<Object, Object> rooms = redisTemplate.opsForHash().entries(ROOM_KEY);
        Map<String, Map<String, String>> returnRooms = new HashMap<>();

        for (Map.Entry<Object, Object> entry : rooms.entrySet()) {
            String roomName = (String) entry.getKey();
            String roomId = (String) entry.getValue();

            Map<String, String> roomInfo = new HashMap<>();
            roomInfo.put("id", roomId);
            roomInfo.put("name", roomName);

            returnRooms.put(roomName, roomInfo);
        }
        return returnRooms;
    }
}

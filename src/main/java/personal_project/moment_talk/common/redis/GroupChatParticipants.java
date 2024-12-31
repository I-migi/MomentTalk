package personal_project.moment_talk.common.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class GroupChatParticipants {

    private final RedisTemplate<String , Object> redisTemplate;
    private static final String ROOM_KEY = "chat:rooms";
    private static final String PARTICIPANTS_KEY_PREFIX = "chat:room:";
    private final Map<String, List<String>> chatParticipants = new ConcurrentHashMap<>();


    public void createGroupChatRoom(String roomName, String httpSessionId) {
        String roomId = UUID.randomUUID().toString();
        redisTemplate.opsForHash().put(ROOM_KEY, roomName, roomId);
        redisTemplate.expire(PARTICIPANTS_KEY_PREFIX + roomId + ":participants", 1, TimeUnit.DAYS);

        List<String> participants = new ArrayList<>();
        participants.add(httpSessionId);
        chatParticipants.put(roomId, participants);
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

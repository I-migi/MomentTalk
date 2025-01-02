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
    private static final String PRIVATE_ROOM_ID = "chat:session_to_room";

    public void createGroupChatRoom(String roomName, String httpSessionId) {
        String roomId = UUID.randomUUID().toString(); // 방 ID 생성
        redisTemplate.opsForHash().put(ROOM_KEY, roomName, roomId); // Redis에 저장


        String savedRoomId = (String) redisTemplate.opsForHash().get(ROOM_KEY, roomName);
        System.out.println("Saved Room ID: " + savedRoomId);

        redisTemplate.expire(PARTICIPANTS_KEY_PREFIX + roomId + ":participants", 1, TimeUnit.DAYS);

        List<String> participants = new ArrayList<>();
        participants.add(httpSessionId);
//        chatParticipants.put(roomId, participants);

        redisTemplate.opsForHash().put(PRIVATE_ROOM_ID, httpSessionId, roomId);
    }

    public void deleteGroupChatRoom(String roomId) {
        redisTemplate.opsForHash().delete(ROOM_KEY, roomId);
        redisTemplate.delete(PARTICIPANTS_KEY_PREFIX + roomId + ":participants");
    }

    public void removeParticipant(String roomId, String httpSessionId) {
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

    public void addParticipantToGroupChatRoom(String roomId, String httpSessionId) {
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

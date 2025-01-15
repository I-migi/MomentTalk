package personal_project.moment_talk.common.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MusicGameService {
    private final RedisTemplate<String , Object> redisTemplate;
    private static final String ROOM_KEY = "chat:musicGameRooms";
    private static final String PARTICIPANTS_KEY_PREFIX = "chat:room:";
    private static final String PRIVATE_ROOM_ID = "chat:session_to_room";

    public void joinMusicGame(String roomId, String httpSessionId) {

    }

    public List<Map<String, String>> getAllMusicGameRooms() {
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

    public void createMusicGameRoom(String roomName, String httpSessionId) {
        String roomId = UUID.randomUUID().toString();
        redisTemplate.opsForHash().put(ROOM_KEY, roomName, roomId);
        redisTemplate.expire(PARTICIPANTS_KEY_PREFIX + roomId + ":participants", 1, TimeUnit.DAYS);

        List<String> participants = new ArrayList<>();
        participants.add(httpSessionId);

        redisTemplate.opsForHash().put(PRIVATE_ROOM_ID, httpSessionId, roomId);
    }
}

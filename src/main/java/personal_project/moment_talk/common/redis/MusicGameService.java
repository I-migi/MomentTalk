package personal_project.moment_talk.common.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import personal_project.moment_talk.common.exception.ParticipantLimitException;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MusicGameService {
    private final RedisTemplate<String , Object> redisTemplate;
    private static final String ROOM_KEY = "chat:musicGameRooms";
    private static final String PARTICIPANTS_KEY_PREFIX = "chat:room:";
    private static final String PRIVATE_ROOM_ID = "chat:session_to_room";

    // 그룹 채팅 참가
    public void joinMusicGame(String roomId, String httpSessionId){
        if ( redisTemplate.opsForSet().size(PARTICIPANTS_KEY_PREFIX + roomId + ":participants") == 4 ) {
            throw new ParticipantLimitException();
        }
        if (!redisTemplate.opsForHash().hasKey(PRIVATE_ROOM_ID, httpSessionId)) {
            redisTemplate.opsForSet().add(PARTICIPANTS_KEY_PREFIX + roomId + ":participants", httpSessionId);
            redisTemplate.opsForHash().put(PRIVATE_ROOM_ID, httpSessionId, roomId);
        }
    }

    public List<Map<String, String>> getAllMusicGameRooms() {
        Map<Object, Object> rooms = redisTemplate.opsForHash().entries(ROOM_KEY);

        List<Map<String, String>> result = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : rooms.entrySet()) {
            String roomName = (String) entry.getKey();
            String roomId = (String) entry.getValue();

            Long participantCount = redisTemplate.opsForSet().size(PARTICIPANTS_KEY_PREFIX + roomId + ":participants");

            Map<String, String> roomData = new HashMap<>();
            roomData.put("id", roomId);
            roomData.put("name", roomName);
            roomData.put("participation", String.valueOf(participantCount != null ? participantCount : 0));
            roomData.put("maxParticipation", "4");


            result.add(roomData);
        }
        return result;
    }

    public List<String> getParticipants(String roomId) {
        String participantsKey = PARTICIPANTS_KEY_PREFIX + roomId + ":participants";
        Set<Object> participantsSet = redisTemplate.opsForSet().members(participantsKey);

        if (participantsSet != null) {
            return participantsSet.stream().map(Object::toString).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public void createMusicGameRoom(String roomName, String httpSessionId) {
        String roomId = UUID.randomUUID().toString();
        redisTemplate.opsForHash().put(ROOM_KEY, roomName, roomId);
        redisTemplate.expire(PARTICIPANTS_KEY_PREFIX + roomId + ":participants", 1, TimeUnit.DAYS);

        List<String> participants = new ArrayList<>();
        participants.add(httpSessionId);

        redisTemplate.opsForHash().put(PRIVATE_ROOM_ID, httpSessionId, roomId);
        redisTemplate.opsForSet().add(PARTICIPANTS_KEY_PREFIX + roomId + ":participants", httpSessionId);
    }

    public boolean leaveMusicGame(String httpSessionId) {
        String roomId = (String) redisTemplate.opsForHash().get(PRIVATE_ROOM_ID, httpSessionId);
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
}

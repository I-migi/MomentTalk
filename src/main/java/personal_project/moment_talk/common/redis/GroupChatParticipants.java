package personal_project.moment_talk.common.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

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

    public Map<Object , Object> getAllGroupChatRooms() {
        return redisTemplate.opsForHash().entries(ROOM_KEY);
    }
}

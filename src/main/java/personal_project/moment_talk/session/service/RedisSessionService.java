package personal_project.moment_talk.session.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisSessionService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void saveSession(String sessionId, Long userId) {
        String key = "session:" + sessionId;
        redisTemplate.opsForHash().put(key, "userId", userId);
        redisTemplate.opsForHash().put(key, "createdAt", LocalDateTime.now());
        redisTemplate.expire(key, Duration.ofMinutes(60));
    }

    public void refreshSession(String sessionId) {
        String key = "session:" + sessionId;
        Long timeout = 3600L;

        if (redisTemplate.hasKey(key)) {
            redisTemplate.expire(key, timeout, TimeUnit.SECONDS);
        }
    }

    public Long getUserIdFromSession(String sessionId) {
        String key = "session:" + sessionId;
        return (Long) redisTemplate.opsForHash().get(key, "userId");
    }

    public void deleteSession(String sessionId) {
        String key = "session:" + sessionId;
        redisTemplate.delete(key);
    }
}

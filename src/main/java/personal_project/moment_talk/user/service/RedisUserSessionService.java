package personal_project.moment_talk.user.service;


import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisUserSessionService {

    private static final long SESSION_TIMEOUT_MINUTES = 60;

    // Redis 와 데이터를 주고 받을 때 사용되는 도구
    private final RedisTemplate<String, Object> redisTemplate;

    /*
    1. 레디스에 저장할 key = "session:" + sessionId
    2. value 로 userId, createdAt 저장
    3. 캐시 만료 기한 설정: 60분
    opsForHash() ->
    key:
    userId -> userId 값
    와 같은 자료구조 저장
     */
    public void saveSession(String sessionId, Long userId) {
        String key = "session:" + sessionId;
        redisTemplate.opsForHash().put(key, "userId", userId);
        redisTemplate.opsForHash().put(key, "createdAt", LocalDateTime.now());
        redisTemplate.expire(key, Duration.ofMinutes(SESSION_TIMEOUT_MINUTES));
    }

    /*
    1. sessionId 를 받고 key 값을 생성
    2. redisTemplate 를 사용해서 해당 Key 값을 캐시에서 가지고 있다면
    3. 캐시의 만료시간 업데이트
     */
    public void refreshSession(String sessionId) {
        String key = "session:" + sessionId;

        if (redisTemplate.hasKey(key)) {
            redisTemplate.expire(key, SESSION_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        }
    }

    /*
    redisTemplate 를 사용해 현재 Redis 에서 해당 sessionId 를 가지고 있는지 boolean 값으로 return
     */
    public boolean isSessionValid(String sessionId) {
        String key = "session:" + sessionId;
        return redisTemplate.hasKey(key);
    }

}

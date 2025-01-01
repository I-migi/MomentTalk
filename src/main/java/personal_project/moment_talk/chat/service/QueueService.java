package personal_project.moment_talk.chat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import personal_project.moment_talk.common.webSocket.WebSocketSessionManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueService {

    private final RedisTemplate<String, String> redisTemplate;
    private final WebSocketSessionManager webSocketSessionManager;
    private static final String WAITING_QUEUE_KEY = "waiting_queue";

    public synchronized void addToQueue(String httpSessionId) {
        if (!isInQueue(httpSessionId)) {
            redisTemplate.opsForList().leftPush(WAITING_QUEUE_KEY, httpSessionId);
            log.info("Session added to queue: {}", httpSessionId);
            return;
        }

        log.info("Session already in queue: {}", httpSessionId);

    }

    public synchronized String getFromQueue() {
        while (true) {
            String httpSessionId = redisTemplate.opsForList().rightPop(WAITING_QUEUE_KEY);

            if (httpSessionId == null) {
                log.info("Queue is empty.");
                return null;
            }

            // 유효성 검증: 닫힌 세션 제거
            if (!webSocketSessionManager.isSessionValid(httpSessionId)) {
                log.info("Invalid or closed session found in queue: {}", httpSessionId);
                removeFromQueue(httpSessionId);
                continue; // 다음 값으로 넘어감
            }

            log.info("Popped from queue: {}", httpSessionId);
            return httpSessionId;
        }
    }


    public boolean isInQueue(String httpSessionId) {
        return redisTemplate.opsForList().range(WAITING_QUEUE_KEY, 0, -1).contains(httpSessionId);
    }


    public void removeFromQueue(String httpSessionId) {
        redisTemplate.opsForList().remove(WAITING_QUEUE_KEY, 1, httpSessionId);
        log.info("Removed from queue: {}", httpSessionId);
    }


}

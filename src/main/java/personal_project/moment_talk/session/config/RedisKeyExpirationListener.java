package personal_project.moment_talk.session.config;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import personal_project.moment_talk.user.repository.UserRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisKeyExpirationListener implements MessageListener {

    private final UserRepository userRepository;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        log.info("Expired key");
        String expiredKey = message.toString(); // 만료된 Redis 키
        handleExpiredKey(expiredKey);
    }

    private void handleExpiredKey(String key) {

        // Redis 키에서 User 관련 데이터를 추출 (예: user:sessionId 형식의 키를 사용하는 경우)
        String sessionId = extractSessionIdFromKey(key);

        // UserRepository를 사용하여 isActive를 false로 업데이트
        userRepository.findBySessionId(sessionId).ifPresent(user -> {
            user.expireSession();
            userRepository.save(user);
        });
    }

    private String extractSessionIdFromKey(String key) {
        // 키 파싱 로직 (예: "user:sessionId" 형식일 경우 sessionId 추출)
        return key.replace("session:", "");
    }
}

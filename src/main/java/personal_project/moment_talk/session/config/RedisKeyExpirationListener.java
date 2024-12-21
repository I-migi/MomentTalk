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
        String expiredKey = message.toString();
        handleExpiredKey(expiredKey);
    }

    private void handleExpiredKey(String key) {

        String sessionId = extractSessionIdFromKey(key);

        userRepository.findBySessionId(sessionId).ifPresent(user -> {
            user.expireSession();
            userRepository.save(user);
        });
    }

    private String extractSessionIdFromKey(String key) {
        return key.replace("session:", "");
    }
}

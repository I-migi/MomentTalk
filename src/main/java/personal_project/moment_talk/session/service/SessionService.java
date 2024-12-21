package personal_project.moment_talk.session.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import personal_project.moment_talk.user.entity.User;
import personal_project.moment_talk.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final UserRepository userRepository;
    private final RedisSessionService redisSessionService;

    public void checkSession(String sessionId) {
        if (userRepository.findBySessionId(sessionId).isPresent()) return;
        String userName = "Anonymous_" + sessionId.substring(0, 8);
        User user = new User(userName, sessionId);
        userRepository.save(user);
        redisSessionService.saveSession(sessionId, user.getId());
    }

}

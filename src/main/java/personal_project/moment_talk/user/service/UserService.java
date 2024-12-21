package personal_project.moment_talk.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import personal_project.moment_talk.user.entity.User;
import personal_project.moment_talk.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public void checkUserIsExists(String sessionId) {
        User user = userRepository.findBySessionId(sessionId).orElse(null);

        if ( user == null) {
            String userName = "Anonymous_" + sessionId.substring(0, 8);
            user = new User(userName, sessionId);
            userRepository.save(user);
        }
    }
}

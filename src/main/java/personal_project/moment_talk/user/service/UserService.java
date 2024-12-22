package personal_project.moment_talk.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import personal_project.moment_talk.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public long getActiveUserCount() {
        return userRepository.countActiveUsers();
    }


}

package personal_project.moment_talk.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import personal_project.moment_talk.user.entity.User;
import personal_project.moment_talk.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserSessionService {

    private final UserRepository userRepository;
    private final RedisUserSessionService redisUserSessionService;

    /*
    1.파라미터로 받은 httpSessionId 가 userRepository 에 존재하는지 확인해 존재하면 return
    2. 존재하지 않는다면 새로운 User 객체 생성
    3. userRepository 에 저장
    4. redisSessionService 에 세션 저장
     */
    public void checkSession(String httpSessionId) {
        if (userRepository.findBySessionId(httpSessionId).isPresent()) return;
        String userName = "Anonymous_" + httpSessionId.substring(0, 8);
        User user = new User(userName, httpSessionId);
        userRepository.save(user);
        redisUserSessionService.saveSession(httpSessionId, user.getId());
    }

}

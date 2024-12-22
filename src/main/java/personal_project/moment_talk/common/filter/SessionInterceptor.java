package personal_project.moment_talk.common.filter;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import personal_project.moment_talk.user.repository.UserRepository;
import personal_project.moment_talk.user.service.RedisUserSessionService;

import java.io.IOException;

/*
SessionInterceptor 인터셉터 정의 -> 클라이언트 요청이 컨트롤러에 도달하기 전에 특정 작업 수행
 */

@Component
@RequiredArgsConstructor
public class SessionInterceptor implements HandlerInterceptor {

    private final RedisUserSessionService redisUserSessionService;
    private final UserRepository userRepository;

    /*
    preHandle : 컨트롤러 실행 전에 호출
    postHandle : 컨트롤러 실행 후, 뷰 렌더링 전에 호출
    afterCompletion : 요청 처리 완료 후 호출
     */

    /*
    HttpServletRequest : 클라이언트 요청 정보 객체
    HttpServletResponse : 서버의 응답 객체
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        /*
        1. 요청 정보 객체에 있는 모든 쿠키를 배열로 GET
        2. 만약 쿠키 배열이 null 이 아니면 -> for 문으로 JSESSIONID 와 이름이 같다면 String sessionId = cookie 의 getValue()
        3. redisUserSessionService.refreshSession()
         */
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("JSESSIONID".equals(cookie.getName())) {
                    String sessionId = cookie.getValue();

                    /*
                    if(세션 ID 가 redis 캐시에 저장되어 있지 않다면) {
                        1. 클라이언트의 쿠키 삭제 + redirect("/")
                        2. User 객체 비활성화
                        3. return false;
                        }
                     if(저장되어 있으면) {
                        세션 TTL 초기화 1시간으로
                        }
                     */
                    if (!redisUserSessionService.isSessionValid(sessionId) ) {
                        handleInvalidSession(response);
                        userRepository.findBySessionId(sessionId).get().expireSession();
                        return false;
                    } else  {
                        redisUserSessionService.refreshSession(sessionId);

                    }
                }
            }
        }
        return true;
    }

    private void handleInvalidSession(HttpServletResponse response) throws IOException {
        Cookie expiredCookie = new Cookie("JSESSIONID", null);
        expiredCookie.setMaxAge(0);
        expiredCookie.setPath("/");
        response.addCookie(expiredCookie);

        response.sendRedirect("/");
    }
}

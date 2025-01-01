package personal_project.moment_talk.user.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import personal_project.moment_talk.user.service.UserService;
import personal_project.moment_talk.user.service.UserSessionService;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserSessionService userSessionService;
    private final UserService userService;

    /*
    1. HttpSession 을 받아 sessionId GET
    2. sessionService 에서 checkSession 실행
     */
    @GetMapping("/")
    public String mainPage(HttpSession httpSession) {

        if (httpSession.getAttribute("HTTP_SESSION_ID") == null) {
            httpSession.setAttribute("HTTP_SESSION_ID", httpSession.getId());
        }

        String httpSessionId = httpSession.getId();
        userSessionService.checkSession(httpSessionId);

        return "mainPage";
    }
    
    @ResponseBody
    @GetMapping("/active-count")
    public long getActiveUserCount() {
        return userService.getActiveUserCount();
    }
}

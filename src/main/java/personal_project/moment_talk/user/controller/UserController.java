package personal_project.moment_talk.user.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import personal_project.moment_talk.session.service.SessionService;
import personal_project.moment_talk.user.service.UserService;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final SessionService sessionService;

    @GetMapping("/")
    public String mainPage(HttpSession session) {

        String sessionId = session.getId();
        sessionService.checkSession(sessionId);

        return "mainPage";
    }

    @GetMapping("/select-category")
    public String selectCategory() {
        return "selectCategory";
    }
}

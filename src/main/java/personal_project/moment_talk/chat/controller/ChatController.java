package personal_project.moment_talk.chat.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChatController {

    @GetMapping("/instant-connect")
    public String instantConnect() {
        return "instant-connect";
    }
}

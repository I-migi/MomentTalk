package personal_project.moment_talk.chat.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import personal_project.moment_talk.chat.service.DeepLTranslationService;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final DeepLTranslationService deepLTranslationService;

    @GetMapping("/1-to-1-chat")
    public String chat() {
        return "chat";
    }

    @ResponseBody
    @PostMapping("/translate")
    public Map<String, String> translate(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        log.info("Translate text : {}", text);

        String translatedText = deepLTranslationService.translate(text);

        return Map.of("translatedText", translatedText);
    }
}

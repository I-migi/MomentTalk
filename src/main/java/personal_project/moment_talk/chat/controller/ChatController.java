package personal_project.moment_talk.chat.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import personal_project.moment_talk.chat.service.DeepLTranslationService;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final DeepLTranslationService deepLTranslationService;

    @GetMapping("/instant-connect")
    public String instantConnect() {
        return "instant-connect";
    }

    /*
    클라이언트가 보낸 JSON 데이터를 Map<String, String> 형태로 자동 변환
    translatedText(번역된 text) = deepLTranslationService.translate(text);
     */
    @ResponseBody
    @PostMapping("/translate")
    public Map<String, String> translate(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        log.info("Translate text : {}", text);

        String translatedText = deepLTranslationService.translate(text);

        return Map.of("translatedText", translatedText);
    }
}

package personal_project.moment_talk.chat.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import personal_project.moment_talk.chat.dto.CreateGroupRequest;
import personal_project.moment_talk.chat.service.DeepLTranslationService;
import personal_project.moment_talk.common.redis.GroupChatParticipants;
import personal_project.moment_talk.common.webSocket.GroupChatWebSocketHandler;

import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final DeepLTranslationService deepLTranslationService;
    private final GroupChatParticipants groupChatParticipants;
    private final GroupChatWebSocketHandler groupChatWebSocketHandler;

    @GetMapping("/1-to-1-chat")
    public String chat() {
        return "chat";
    }

    @GetMapping("/group-chat")
    public String groupChat() {
        return "group-chat";
    }

    @ResponseBody
    @GetMapping("/group-chat/rooms")
    public List<Map<String, String>> groupChatRooms() {
        return groupChatParticipants.getAllGroupChatRooms();
    }

    @PostMapping("/group-chat/rooms")
    @ResponseBody
    public Map<String, String> createGroupChatRoom(@RequestBody CreateGroupRequest request, HttpSession httpSession) {
        groupChatWebSocketHandler.handleCreateRoom(request.name(), httpSession.getId());
        return Map.of("message", "Group chat room created successfully", "name", request.name());
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

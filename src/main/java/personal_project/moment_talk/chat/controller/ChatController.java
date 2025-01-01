package personal_project.moment_talk.chat.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
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
    private final RedisTemplate redisTemplate;

    @GetMapping("/1-to-1-chat")
    public String chat() {
        return "chat";
    }

    @GetMapping("/group-chat")
    public String groupChat() {
        return "group-chat";
    }


    @PostMapping("/join")
    @ResponseBody
    public ResponseEntity<Map<String, String>> joinRoom(@RequestBody Map<String, String> request, HttpSession httpSession) {
        String roomId = request.get("roomId");
        String httpSessionId = httpSession.getId();

        if (roomId == null || roomId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid room ID!"));
        }

        // 그룹 채팅방 참가 로직
        groupChatWebSocketHandler.handleJoinRoom(roomId, httpSessionId);

        return ResponseEntity.ok(Map.of("message", "User joined the room", "roomId", roomId));
    }



    @ResponseBody
    @GetMapping("/group-chat/rooms")
    public List<Map<String, String>> groupChatRooms() {
        return groupChatParticipants.getAllGroupChatRooms();
    }

    @PostMapping("/group-chat/rooms")
    @ResponseBody
    public Map<String, String> createGroupChatRoom(@RequestBody CreateGroupRequest request, HttpSession httpSession) {
        String roomName = request.name();
        String httpSessionId = httpSession.getId();

        log.info("Creating room: {} by session: {}", roomName, httpSessionId);

        groupChatWebSocketHandler.handleCreateRoom(roomName, httpSessionId);

        String roomId = (String) redisTemplate.opsForHash().get("chat:rooms", roomName);
        log.info("Created room ID: {}", roomId);

        // 응답 데이터 확인
        return Map.of("message", "Group chat room created successfully", "id", roomId, "name", roomName);
    }

//    @GetMapping("/room")
//    public ResponseEntity<Map<String , String >> getUserRoom(HttpSession httpSession) {
//        String httpSessionId = httpSession.getId();
//        String roomId = (String) redisTemplate.opsForHash().get("chat:session_to_room", httpSessionId);
//
//        if (roomId == null) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                    .body(Map.of("message", "User is not part of any room"));
//        }
//        return ResponseEntity.ok(Map.of("roomId", roomId));
//    }

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

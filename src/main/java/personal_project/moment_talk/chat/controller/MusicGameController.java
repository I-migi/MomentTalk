package personal_project.moment_talk.chat.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import personal_project.moment_talk.chat.dto.CreateGroupRequest;
import personal_project.moment_talk.common.redis.GroupChatParticipants;
import personal_project.moment_talk.common.redis.MusicGameService;
import personal_project.moment_talk.user.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MusicGameController {

    private final GroupChatParticipants groupChatParticipants;
    private final MusicGameService musicGameService;
    private final RedisTemplate redisTemplate;
    private final UserRepository userRepository;

    @GetMapping("/music-game")
    public String musicGame() {
        return "music-game";
    }

    @PostMapping("/music-game/join")
    @ResponseBody
    public ResponseEntity<Map<String, String>> joinMusicGame(@RequestBody Map<String, String> request, HttpSession httpSession) {
        String roomId = request.get("roomId");
        String httpSessionId = httpSession.getId();
        if (roomId == null || roomId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid room ID!"));
        }
        groupChatParticipants.joinGroupChatRoom(roomId, httpSessionId);
        return ResponseEntity.ok(Map.of("message", "User joined the room", "roomId", roomId));
    }

    @ResponseBody
    @PostMapping("/music-game/rooms")
    public Map<String, String> createMusicGameRoom(@RequestBody CreateGroupRequest createGroupRequest, HttpSession httpSession) {
        String roomName = createGroupRequest.name();
        String httpSessionId = httpSession.getId();

        musicGameService.createMusicGameRoom(roomName, httpSessionId);

        String roomId = (String) redisTemplate.opsForHash().get("chat:musicGameRooms", roomName);
        return Map.of("message", "Music Game room created", "id", roomId, "name", roomName);
    }

    @ResponseBody
    @GetMapping("/music-game/rooms")
    public List<Map<String, String>> musicGameRooms() {
        return musicGameService.getAllMusicGameRooms();
    }

    @PostMapping("/music-game/leave")
    public ResponseEntity<String> leaveMusicGame(HttpSession httpSession) {
        if (!musicGameService.leaveMusicGame(httpSession.getId())) {
            return ResponseEntity.ok("단체 채팅방에 들어가 있지 않습니다");
        }
        return ResponseEntity.ok("단체 채팅방에서 나갔습니다");
    }

    @ResponseBody
    @GetMapping("/music-game/participants")
    public Map<String, List<String>> getParticipants(@RequestParam String roomId, HttpSession httpSession) {
        List<String> participants = musicGameService.getParticipants(roomId);
        List<String> participantsUserName = new ArrayList<>();
        String httpSessionId = httpSession.getId();
        for (String participant : participants) {
            userRepository.findBySessionId(participant).ifPresent(user -> {
                if (httpSessionId.equals(participant)) {
                    participantsUserName.add("You");
                } else {
                    participantsUserName.add(user.getUserName());
                }
            });
        }

        return Map.of("participants", participantsUserName);
    }



}

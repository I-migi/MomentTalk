package personal_project.moment_talk.chat.controller;

import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import personal_project.moment_talk.chat.dto.GuessRequest;
import personal_project.moment_talk.chat.service.YoutubeService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class YoutubeController {

    @Value("${YOUTUBE_KEY}")
    private String apiKey;

    private final YoutubeService youtubeService;
    private static final String YOUTUBE_API_URL = "https://www.googleapis.com/youtube/v3/playlistItems";


    @GetMapping
    public ResponseEntity<String> searchVideo(@RequestParam String keyword) throws IOException {
        String result = youtubeService.searchVideo(keyword);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/playlist/{playlistId}")
    public ResponseEntity<?> getPlaylistVideos(@PathVariable String playlistId) {
        String url = UriComponentsBuilder.fromHttpUrl(YOUTUBE_API_URL)
                .queryParam("part", "snippet")
                .queryParam("playlistId", playlistId)
                .queryParam("maxResults", 50)
                .queryParam("key", apiKey)
                .toUriString();

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        return ResponseEntity.ok(response.getBody());
    }

    @PostMapping("/validate-guess")
    public ResponseEntity<?> validateGuess(@RequestBody GuessRequest request) {
        String videoTitle = youtubeService.getVideoTitle(request.videoId());

        // 유저 입력과 제목을 정리: 대소문자 무시, 공백 제거
        String normalizedTitle = videoTitle.replaceAll("\\s+", "").toLowerCase();
        String normalizedGuess = request.userGuess().replaceAll("\\s+", "").toLowerCase();

        boolean isCorrect = normalizedTitle.equals(normalizedGuess); // 정확히 비교

        Map<String, Object> response = new HashMap<>();
        response.put("correct", isCorrect);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/video-title/{videoId}")
    public ResponseEntity<?> getVideoTitle(@PathVariable String videoId) {
        String videoTitle = youtubeService.getVideoTitle(videoId);
        Map<String, String> response = new HashMap<>();
        response.put("title", videoTitle);

        return ResponseEntity.ok(response);
    }

}

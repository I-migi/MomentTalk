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

    private final YoutubeService youtubeService;



    @GetMapping("/playlist/{playlistId}")
    public ResponseEntity<?> getPlaylistVideos(@PathVariable String playlistId) {
        String url = youtubeService.getPlayListVideosUrl(playlistId);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        return ResponseEntity.ok(response.getBody());
    }

    @PostMapping("/validate-guess")
    public ResponseEntity<?> validateGuess(@RequestBody GuessRequest request) {
        boolean isCorrect = youtubeService.validateVideoTitle(request.videoId(), request.userGuess());

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

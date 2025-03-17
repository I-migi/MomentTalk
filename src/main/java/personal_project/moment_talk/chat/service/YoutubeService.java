package personal_project.moment_talk.chat.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import personal_project.moment_talk.chat.dto.YouTubeApiResponse;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class YoutubeService {

    @Value("${YOUTUBE_KEY}")
    private String apiKey;

    private static final String YOUTUBE_API_URL = "https://www.googleapis.com/youtube/v3/playlistItems";


    private final Map<String, String> videoTitleMap;

    public YoutubeService(ObjectMapper objectMapper) {
        Map<String, String > tempMap;
        try {
            File file = new File("src/main/resources/videoTitleMap.json");
            tempMap = objectMapper.readValue(file, new TypeReference<Map<String, String>>() {});
        } catch (IOException e) {
            e.printStackTrace();
            tempMap = Collections.emptyMap(); // 파일 읽기에 실패하면 빈 맵으로 초기화
        }
        this.videoTitleMap = tempMap;
    }

    public String getVideoTitle(String videoId) {
        return videoTitleMap.get(videoId);
    }

    public boolean validateVideoTitle(String videoId, String guessTitle) {
        String videoTitle = getVideoTitle(videoId);

        // 유저 입력과 제목을 정리: 대소문자 무시, 공백 제거
        String normalizedTitle = videoTitle.replaceAll("\\s+", "").toLowerCase();
        String normalizedGuess = guessTitle.replaceAll("\\s+", "").toLowerCase();

        return normalizedTitle.equals(normalizedGuess); // 정확히 비교
    }

    public String getPlayListVideosUrl(String playlistId) {
        return UriComponentsBuilder.fromHttpUrl(YOUTUBE_API_URL)
                .queryParam("part", "snippet")
                .queryParam("playlistId", playlistId)
                .queryParam("maxResults", 50)
                .queryParam("key", apiKey)
                .toUriString();
    }
}


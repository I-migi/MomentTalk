package personal_project.moment_talk.chat.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YouTubeApiResponse {
    private List<VideoItem> items;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VideoItem {
        private Snippet snippet;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Snippet {
        private String title;
    }
}
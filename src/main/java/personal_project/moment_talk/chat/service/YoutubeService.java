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

    private final ObjectMapper objectMapper;
    @Value("${YOUTUBE_KEY}")
    private String apiKey;

    private final Map<String, String> videoTitleMap;

    public YoutubeService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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


    public String searchVideo(String query) throws IOException {
        // JSON 데이터를 처리하기 위한 JsonFactory 객체 생성
        JsonFactory jsonFactory = new JacksonFactory();

        // YouTube 객체를 빌드하여 API에 접근할 수 있는 YouTube 클라이언트 생성
        YouTube youtube = new YouTube.Builder(
                new com.google.api.client.http.javanet.NetHttpTransport(),
                jsonFactory,
                request -> {})
                .build();

        // YouTube Search API를 사용하여 동영상 검색을 위한 요청 객체 생성
        YouTube.Search.List search = youtube.search().list(Collections.singletonList("id,snippet"));

        // API 키 설정
        search.setKey(apiKey);

        // 검색어 설정
        search.setQ(query);

        // 검색 요청 실행 및 응답 받아오기
        SearchListResponse searchResponse = search.execute();

        // 검색 결과에서 동영상 목록 가져오기
        List<SearchResult> searchResultList = searchResponse.getItems();

        if (searchResultList != null && searchResultList.size() > 0) {
            //검색 결과 중 첫 번째 동영상 정보 가져오기
            SearchResult searchResult = searchResultList.get(0);

            // 동영상의 ID와 제목 가져오기
            String videoId = searchResult.getId().getVideoId();
            String videoTitle = searchResult.getSnippet().getTitle();

            return "Title: " + videoTitle + "\nURL: https://www.youtube.com/watch?v=" + videoId;
        }
        return "검색 결과가 없습니다";
    }

    public String getVideoTitle(String videoId) {

        return videoTitleMap.get(videoId);
//        String url = "https://www.googleapis.com/youtube/v3/videos?part=snippet&id=" + videoId + "&key=" + apiKey;

//        RestTemplate restTemplate = new RestTemplate();
//
//        YouTubeApiResponse response = restTemplate.getForObject(url, YouTubeApiResponse.class);
//
//        if (response != null && !response.getItems().isEmpty()) {
//            String fullTitle = response.getItems().get(0).getSnippet().getTitle();
//            return extractSongTitle(fullTitle);
//
//        }
//
//        return null;
    }

//    public static String extractSongTitle(String fullTitle) {
//        // 1. 괄호 안에 한글 제목이 있는 경우 우선적으로 추출
//        String koreanInBrackets = extractKoreanInBrackets(fullTitle);
//        if (koreanInBrackets != null) {
//            return koreanInBrackets.trim();
//        }
//
//        // 2. 괄호와 괄호 안의 부가 정보 제거
//        String title = fullTitle.replaceAll("\\s*\\([^)]*\\)", "").trim();
//
//        // 3. " - " 또는 " feat." 이후 제거
//        title = title.split(" - ")[0].split("feat\\.", 2)[0].trim();
//
//        // 4. 영어/한글이 섞여 있을 경우 한글을 우선적으로 선택
//        String koreanPart = extractKoreanPart(title);
//        if (!koreanPart.isEmpty()) {
//            title = koreanPart.trim();
//        }
//
//        // 5. 불필요한 특수 문자 제거 (, ! ? 등)
//        title = title.replaceAll("[,!?\\)]", "").trim(); // 불필요한 닫는 괄호 제거
//
//        return title;
//    }
//
//    private static String extractKoreanInBrackets(String text) {
//        // 괄호 안에 한글을 우선적으로 추출
//        String regex = "\\(([^\\)\\(\\u0000-\\u007F]*)\\)";
//        Matcher matcher = Pattern.compile(regex).matcher(text);
//        if (matcher.find()) {
//            return matcher.group(1);
//        }
//        return null;
//    }
//
//    private static String extractKoreanPart(String text) {
//        // 문자열에서 한글만 추출
//        String regex = "[\\u3131-\\uD79D]+"; // 한글 범위
//        Matcher matcher = Pattern.compile(regex).matcher(text);
//        StringBuilder koreanBuilder = new StringBuilder();
//        while (matcher.find()) {
//            koreanBuilder.append(matcher.group()).append(" ");
//        }
//        return koreanBuilder.toString().trim();
//    }
}


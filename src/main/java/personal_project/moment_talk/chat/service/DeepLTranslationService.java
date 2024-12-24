package personal_project.moment_talk.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeepLTranslationService {

    private final RestTemplate restTemplate;

    @Value("${DEEPL_API_KEY}")
    private String apiKey;

    @Value("${DEEPL_API_END_POINT}")
    private String endPoint;

    private static final Map<String, String > fixedTranslations = new HashMap<>();

    static {
        fixedTranslations.put("hello", "안녕하세요");
    }


    public String translate(String text) {

        if (fixedTranslations.containsKey(text.toLowerCase())) {
            return fixedTranslations.get(text.toLowerCase());
        }

        String detectedLanguage = detectLanguage(text); // 텍스트가 한글인지 영어인지 감지
        String targetLanguage = detectedLanguage.equals("ko") ? "en" : "ko";

        try {
            HttpEntity<MultiValueMap<String, String>> requestEntity = makeDeepLPostEntity(text, detectedLanguage, targetLanguage);

            /* DeepL API 호출
            POST 요청을 실행하고, DeepL API 의 응답을 ResponseEntity 객체로 받음.
            요청 대상: endPoint
            요청 본문: requestEntity
            응답 형식: String
             */
            ResponseEntity<String> response = restTemplate.postForEntity(endPoint, requestEntity, String.class);

            log.info("Raw Response: {}", response.getBody());

            // ObjectMapper: Jackson 라이브러리를 사용해 JSON 문자열을 Java 객체로 변환
            ObjectMapper objectMapper = new ObjectMapper();
            // DeepL 의 JSON 응답을 Map<String, Object> 형태로 변환
            Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);

            if (responseMap.containsKey("translations")) {
                List<Map<String, String>> translations = (List<Map<String, String>>) responseMap.get("translations");
                String translatedText = translations.get(0).get("text");

                // URL 디코딩 처리
                String decodedText = translatedText;
                if (translatedText.contains("%")) {
                    decodedText = URLDecoder.decode(translatedText, StandardCharsets.UTF_8);
                }

                log.info("Translated Text (Decoded): {}", decodedText);
                return decodedText;
            }
        } catch (Exception e) {
            log.error("Error while translating text", e);
        }

        // 번역 실패 시 null 반환
        return null;
    }

    private HttpEntity<MultiValueMap<String, String>> makeDeepLPostEntity(String text, String detectedLanguage, String targetLanguage) {
        // POST 요청 본문을 생성하기 위해 사용 -> key-value 형태로 데이터를 구성
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("auth_key", apiKey);
        body.add("text", text);
        body.add("source_lang", detectedLanguage.toUpperCase());
        body.add("target_lang", targetLanguage.toUpperCase());

        // 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // HHTP 요청 본문(body) 와 헤더 결합하여 하나의 요청 엔티티 객체로 생성
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);
        return requestEntity;
    }

    // 텍스트가 한글인지 영어인지 감지
    public String detectLanguage(String text) {
        if (text.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*")) { // 한글 포함 여부 확인
            return "ko";
        }
        return "en"; // 기본적으로 영어로 간주
    }
}

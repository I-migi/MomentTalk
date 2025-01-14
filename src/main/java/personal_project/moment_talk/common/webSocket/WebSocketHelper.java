package personal_project.moment_talk.common.webSocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHelper {

    private final ObjectMapper objectMapper;

    public String getHttpSessionIdFromWebSocketSession(WebSocketSession webSocketSession) {
        return (String) webSocketSession.getAttributes().get("HTTP_SESSION_ID");
    }

    public synchronized void sendMessageSafely(WebSocketSession webSocketSession, String message, String userName) throws IOException {

        if (webSocketSession != null && webSocketSession.isOpen()) {
            Map<String, Object> finalMessage = new HashMap<>();
            finalMessage.put("userName", userName);
            finalMessage.put("timestamp", Instant.now().toString());

            Map<String, Object> messageMap = objectMapper.readValue(message, Map.class);
            String type = (String) messageMap.get("type");

            if ("file".equals(type)) {

                finalMessage.put("type", "file");
                finalMessage.put("fileName", messageMap.get("fileName"));
                finalMessage.put("fileType", messageMap.get("fileType"));
                finalMessage.put("size", messageMap.get("size"));
            } else {

                finalMessage.put("type", "text");
                finalMessage.put("content", messageMap.get("content"));
            }

            String jsonMessage = objectMapper.writeValueAsString(finalMessage);
            log.info("Sending JSON message: {}", jsonMessage);
            webSocketSession.sendMessage(new TextMessage(jsonMessage));
        } else {
            log.info("Failed to send message. Session is closed or null: " + (webSocketSession != null ? webSocketSession.getId() : "null"));
        }
    }
}

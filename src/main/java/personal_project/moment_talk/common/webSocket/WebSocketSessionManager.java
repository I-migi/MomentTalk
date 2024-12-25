package personal_project.moment_talk.common.webSocket;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionManager {

    // 현재 활성화된 WebSocket 세션 관리.
    private final ConcurrentHashMap<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();

    public void addSession(String sessionId, WebSocketSession session) {
        sessionMap.put(sessionId, session);
    }

    public void removeSession(String sessionId) {
        sessionMap.remove(sessionId);
    }

    public boolean isSessionValid(String sessionId) {
        WebSocketSession session = sessionMap.get(sessionId);
        return session != null && session.isOpen();
    }

    public WebSocketSession getSession(String sessionId) {
        return sessionMap.get(sessionId);
    }

}

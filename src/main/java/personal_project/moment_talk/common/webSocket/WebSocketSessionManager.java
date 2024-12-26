package personal_project.moment_talk.common.webSocket;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import personal_project.moment_talk.user.repository.UserRepository;

import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class WebSocketSessionManager {

    // 현재 활성화된 WebSocket 세션 관리.
    private final ConcurrentHashMap<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();

    public void addSession(String httpSessionId, WebSocketSession webSocketSession) {
        sessionMap.put(httpSessionId, webSocketSession);
    }

    public void removeSession(String httpSessionId) {
        sessionMap.remove(httpSessionId);
    }

    public boolean isSessionValid(String httpSessionId) {
        WebSocketSession webSocketSession = sessionMap.get(httpSessionId);
        return webSocketSession != null && webSocketSession.isOpen();
    }

    public WebSocketSession getSession(String httpSessionId) {
        return sessionMap.get(httpSessionId);
    }

}

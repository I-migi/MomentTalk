package personal_project.moment_talk.common.webSocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/*
EnableWebSocket -> WebSocket 을 활성화하기 위해 사용
WebSocketConfigurer -> WebSocket 핸들러를 등록하기 위해 Spring 에서 제공하는 인터페이스
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    /*
    WebSocket -> 클라이언트와 서버 간에 양방향 통신을 가능하게 하는 네트워크 프로토콜

    1. 양방향 통신
    2. 지속적 연결
    3. HTTP HandShake 사용


    ChatWebSocketHandler -> WebSocket 요청을 처리할 핸들러
    registry.addHandler(chatWebsocketHandler, "/ws/**")
    -> /ws/** 경로로 들어오는 WebSocket 요청을 chatWebsocketHandler 로 처리

    setAllowedOrigins("*") -> 모든 도메인에서의 WebSocket 요청을 허용
    운영 환경에서는 특정 도메인한 허용되도록 설정 필요

    1. 클라이언트는 /ws/.. 경로로 WebSocket 연결 시도
    2. 연결이 성공하면 ChatWebSocketHandler 가 WebSocket 세션을 관리하고 메시지 처리

    ChatWebSocketHandler 클래스에서 WebSocket 메시지 처리 구현 필요.

     */
    private final ChatWebSocketHandler chatWebsocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebsocketHandler, "/ws/**").setAllowedOrigins("*");
    }
}

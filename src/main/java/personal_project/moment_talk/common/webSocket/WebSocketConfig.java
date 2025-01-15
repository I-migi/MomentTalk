package personal_project.moment_talk.common.webSocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

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
        registry.addHandler(chatWebsocketHandler, "/ws/**").setAllowedOrigins("*")
                .setHandshakeHandler(new DefaultHandshakeHandler())
                .addInterceptors(new HttpSessionHandshakeInterceptor());

        registry.addHandler(chatWebsocketHandler, "/group-chat/**").setAllowedOrigins("*")
                .setHandshakeHandler(new DefaultHandshakeHandler())
                .addInterceptors(new HttpSessionHandshakeInterceptor());

        registry.addHandler(chatWebsocketHandler, "/music-game/**").setAllowedOrigins("*")
                .setHandshakeHandler(new DefaultHandshakeHandler())
                .addInterceptors(new HttpSessionHandshakeInterceptor());
    }

    /*
    WebSocketHandler 를 커스터마이징 하기 위한 Decorator Factory
    afterConnectionEstablished -> WebSocket 연결이 성공적으로 수립된 이후 실행
    setBinaryMessageSizeLimit -> 바이너리 메시지(파일, 이미지, 영상)의 최대 크기 제한
    setTextMessageSizeLimit -> 테스트 메시지(JSON, 문자열)의 최대 크기 제한
     */
    @Bean
    public WebSocketHandlerDecoratorFactory webSocketHandlerDecoratorFactory() {
        return (handler) -> new WebSocketHandlerDecorator(handler) {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                session.setBinaryMessageSizeLimit(100 * 1024 * 1024); // 10MB
                session.setTextMessageSizeLimit(100 * 1024 * 1024); // 10MB
                super.afterConnectionEstablished(session);
            }
        };
    }

    /*
    WebSocket 서버 컨테이너의 동작 구성
    Spring 의 WebSocket 설정은 Tomcat 컨테이너 위에서 동작하기 때문에 해당 컨테이너 수준에서의 메시지 크기 제어
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(200 * 1024);  // 200KB
        container.setMaxBinaryMessageBufferSize( 100 * 1024 * 1024);
        return container;
    }


}

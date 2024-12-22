package personal_project.moment_talk.common.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisEventListenerConfig {

    /*
    Redis 에서 발생하는 이번트를 감지하기 위한 설정
    RedisMessageListenerContainer : Redis Pub/Sub(Message Publishing and Subscribing) 를 구현하기 위한 컨테이너

    container.setConnectionFactory(connectionFactory); -> Redis 서버와의 연결을 생성하는 팩토리 설정
    container.addMessageListener(listenerAdapter, new PatternTopic("__keyevent@*__:expired")); -> 특정 메시지를 처리할 리스너와 메시지 주체 연결
    "__keyevent@*__:expired" -> Redis 키 만료 이벤트
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter listenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listenerAdapter, new PatternTopic("__keyevent@*__:expired"));
        return container;
    }

    @Bean
    public MessageListenerAdapter listenerAdapter(RedisKeyExpirationListener listener) {
        return new MessageListenerAdapter(listener);
    }
}

package personal_project.moment_talk.common;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import personal_project.moment_talk.common.filter.SessionInterceptor;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final SessionInterceptor sessionInterceptor;

    /*
    sessionInterceptor 를 모든 요청 처리에 체인 추가

    체인에 추가한다 : Spring MVC 의 요청 처리 흐름에서 여러 개의 작업을 순서대로 연결하는 구조
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sessionInterceptor)
                .addPathPatterns("/**");
    }


    /*
    RestTemplate 는 Spring 에서 HTTP 요청을 보내고 응답을 처리하는 데 사용되는 클래스
    RestTemplate 객체를 스프링 빈으로 등록해 애플리케이션 전역에서 주입받아 사용할 수 있게 함
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

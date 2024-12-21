package personal_project.moment_talk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class MomentTalkApplication {

    public static void main(String[] args) {
        SpringApplication.run(MomentTalkApplication.class, args);
    }

}

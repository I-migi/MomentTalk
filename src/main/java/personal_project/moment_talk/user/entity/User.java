package personal_project.moment_talk.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import personal_project.moment_talk.common.AbstractBaseTime;

@Entity
@NoArgsConstructor
@Getter
public class User extends AbstractBaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Anonymous + sessionId (익명)
    private String userName;

    // 클라이언트 쿠키에 JSESSIONID 로 저장
    @Column(unique = true)
    private String sessionId;

    // 활성화 여부 -> 현재 접속중인 유저로 사용할 예정
    private boolean isActive;

    public User(String userName, String sessionId) {
        this.userName = userName;
        this.sessionId = sessionId;
        this.isActive = true;
    }

    // 비활성화
    public void expireSession() {
        this.isActive = false;
    }
}

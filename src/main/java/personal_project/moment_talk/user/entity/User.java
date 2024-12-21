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

    private String userName;

    @Column(unique = true)
    private String sessionId;

    private boolean isActive;

    public User(String userName, String sessionId) {
        this.userName = userName;
        this.sessionId = sessionId;
        this.isActive = true;
    }

    public void expireSession() {
        this.isActive = false;
    }
}

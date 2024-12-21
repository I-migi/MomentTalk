package personal_project.moment_talk.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import personal_project.moment_talk.user.entity.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findBySessionId(String sessionId);

    Long countByIsActive(boolean isActive);
}

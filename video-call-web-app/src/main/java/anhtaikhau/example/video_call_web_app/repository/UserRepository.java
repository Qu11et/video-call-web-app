package anhtaikhau.example.video_call_web_app.repository;

import anhtaikhau.example.video_call_web_app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
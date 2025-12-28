package anhtaikhau.example.video_call_web_app.repository;

import anhtaikhau.example.video_call_web_app.model.ParticipantHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantHistoryRepository extends JpaRepository<ParticipantHistory, Long> {
}
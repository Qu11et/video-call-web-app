package anhtaikhau.example.video_call_web_app.repository;

import anhtaikhau.example.video_call_web_app.model.MeetingLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingLogRepository extends JpaRepository<MeetingLog, Long> {
}
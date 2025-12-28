package anhtaikhau.example.video_call_web_app.repository;

import anhtaikhau.example.video_call_web_app.model.Meeting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MeetingRepository extends MongoRepository<Meeting, String> {

    // 1. Tìm cuộc họp theo Room ID
    List<Meeting> findByRoomId(String roomId);

    // 2. Lấy danh sách cuộc họp mới nhất (Phân trang)
    // Dùng cho trang Admin Dashboard
    Page<Meeting> findAllByOrderByStartTimeDesc(Pageable pageable);

    // 3. (Tùy chọn) Tìm cuộc họp trong khoảng thời gian
    List<Meeting> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);
}
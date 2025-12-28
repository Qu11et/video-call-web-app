package anhtaikhau.example.video_call_web_app.repository;

import anhtaikhau.example.video_call_web_app.model.ParticipantHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantHistoryRepository extends JpaRepository<ParticipantHistory, Long> {
    
    // Tìm theo User ID, sắp xếp và phân trang
    // JPA tự động hiểu User_Id là tìm theo field id của object User
    Page<ParticipantHistory> findByUser_Id(Long userId, Pageable pageable);
}
package anhtaikhau.example.video_call_web_app.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "participant_history")
public class ParticipantHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // KHÓA NGOẠI (Foreign Key) trỏ đến bảng User
    // Quan hệ: Nhiều lịch sử thuộc về 1 User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore // Tránh vòng lặp vô hạn khi serializing JSON
    private User user;

    // KHÓA NGOẠI trỏ đến bảng MeetingLog
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_log_id")
    @JsonIgnore
    private MeetingLog meetingLog;

    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
}
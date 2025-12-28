package anhtaikhau.example.video_call_web_app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "meeting_log")
public class MeetingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String roomId; // Mã phòng (ví dụ: my-room-1)

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationSeconds;

    // Quan hệ: Một cuộc họp có nhiều người tham gia
    @OneToMany(mappedBy = "meetingLog", cascade = CascadeType.ALL)
    private List<ParticipantHistory> histories;
}
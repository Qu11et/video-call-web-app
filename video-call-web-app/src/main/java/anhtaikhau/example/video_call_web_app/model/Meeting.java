package anhtaikhau.example.video_call_web_app.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "meetings") // Tên collection trong MongoDB
public class Meeting {

    @Id
    private String id; // MongoDB tự sinh ObjectID

    @Indexed // Đánh index để tìm kiếm theo roomId nhanh hơn
    private String roomId;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    private Long durationSeconds; // Thời lượng cuộc họp (giây)

    // Danh sách người tham gia (Lưu mảng object con)
    private List<ParticipantInfo> participants;

    // --- Inner Class: Thông tin người tham gia ---
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantInfo {
        private String identity; // User ID hoặc Email
        private String name;     // Tên hiển thị
        private LocalDateTime joinedAt;
        private LocalDateTime leftAt;
    }
}
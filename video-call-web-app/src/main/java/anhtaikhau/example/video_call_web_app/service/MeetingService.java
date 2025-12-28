package anhtaikhau.example.video_call_web_app.service;

import anhtaikhau.example.video_call_web_app.dto.webhook.LiveKitWebhookEvent;
import anhtaikhau.example.video_call_web_app.model.Meeting;
import anhtaikhau.example.video_call_web_app.repository.MeetingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingService {

    private final MeetingRepository meetingRepository;

    /**
     * Xử lý sự kiện khi phòng họp kết thúc
     */
    public void handleRoomFinished(LiveKitWebhookEvent event, ParticipantTracker tracker) {
        log.info("Đang xử lý room_finished cho phòng: {}", event.getRoom().getName());

        try {
            LocalDateTime start = unixToLocal(event.getRoom().getCreationTime());
            Long endTimeUnix = event.getRoom().getEndTime();
            LocalDateTime end = endTimeUnix != null ? unixToLocal(endTimeUnix) : LocalDateTime.now();
            
            Long duration = null;
            if (event.getRoom().getCreationTime() != null && endTimeUnix != null) {
                duration = endTimeUnix - event.getRoom().getCreationTime();
            } else if (event.getRoom().getCreationTime() != null) {
                duration = Instant.now().getEpochSecond() - event.getRoom().getCreationTime();
            }

            // ✅ LẤY PARTICIPANTS TỪ TRACKER
            List<ParticipantTracker.ParticipantData> trackedParticipants = 
                tracker.getAndRemoveParticipants(event.getRoom().getName());
            
            List<Meeting.ParticipantInfo> participants = new ArrayList<>();
            for (var p : trackedParticipants) {
                participants.add(new Meeting.ParticipantInfo(
                    p.getIdentity(),
                    p.getName(),
                    unixToLocal(p.getJoinedAt()),
                    unixToLocal(p.getLeftAt())
                ));
            }

            Meeting meeting = Meeting.builder()
                    .roomId(event.getRoom().getName())
                    .startTime(start)
                    .endTime(end)
                    .durationSeconds(duration)
                    .participants(participants)
                    .build();

            meetingRepository.save(meeting);
            log.info("✅ Đã lưu lịch sử cuộc họp vào MongoDB. ID: {}, Duration: {}s, Participants: {}", 
                     meeting.getId(), duration, participants.size());

        } catch (Exception e) {
            log.error("❌ Lỗi khi lưu meeting history: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save meeting history", e);
        }
    }

    /**
     * Lấy danh sách cuộc họp có phân trang
     */
    public Page<Meeting> getAllMeetings(Pageable pageable) {
        // Hàm này đã có sẵn trong MeetingRepository (nếu bạn kế thừa MongoRepository)
        // Nếu muốn sắp xếp mặc định mới nhất, dùng findAllByOrderByStartTimeDesc như đã khai báo ở GĐ2
        return meetingRepository.findAllByOrderByStartTimeDesc(pageable);
    }

    /**
     * Xem chi tiết một cuộc họp
     */
    public Meeting getMeetingById(String id) {
        return meetingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Meeting not found with ID: " + id));
    }

    // Helper: Convert Unix Timestamp (giây) sang Java LocalDateTime
    private LocalDateTime unixToLocal(Long timestamp) {
        if (timestamp == null) return null;
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault());
    }
}
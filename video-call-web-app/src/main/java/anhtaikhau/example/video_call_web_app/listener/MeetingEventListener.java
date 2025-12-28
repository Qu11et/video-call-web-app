package anhtaikhau.example.video_call_web_app.listener;

import anhtaikhau.example.video_call_web_app.dto.webhook.LiveKitWebhookEvent;
import anhtaikhau.example.video_call_web_app.event.MeetingEndedEvent;
import anhtaikhau.example.video_call_web_app.model.Meeting;
import anhtaikhau.example.video_call_web_app.model.MeetingLog;
import anhtaikhau.example.video_call_web_app.model.ParticipantHistory;
import anhtaikhau.example.video_call_web_app.model.User;
import anhtaikhau.example.video_call_web_app.repository.MeetingLogRepository;
import anhtaikhau.example.video_call_web_app.repository.MeetingRepository;
import anhtaikhau.example.video_call_web_app.repository.ParticipantHistoryRepository;
import anhtaikhau.example.video_call_web_app.repository.UserRepository;
import anhtaikhau.example.video_call_web_app.service.ParticipantTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class MeetingEventListener {

    private final MeetingRepository mongoRepository;       // Mongo
    private final MeetingLogRepository postgresRepository; // Postgres (MeetingLog)
    private final ParticipantHistoryRepository historyRepo;// Postgres (History)
    private final UserRepository userRepository;           // Postgres (User)
    private final ParticipantTracker tracker;              // Redis

    @Async // Chạy ở thread riêng
    @EventListener
    public void handleMeetingEnded(MeetingEndedEvent event) {
        LiveKitWebhookEvent data = event.getWebhookData();
        String roomId = data.getRoom().getName();
        
        log.info("⚡ EventListener received room_finished: {}", roomId);

        try {
            // 1. Lấy danh sách participants từ Redis (Lấy 1 lần dùng cho cả 2 DB)
            List<ParticipantTracker.ParticipantData> trackedParticipants = 
                tracker.getAndRemoveParticipants(roomId);

            // 2. Xử lý thời gian
            LocalDateTime start = unixToLocal(data.getRoom().getCreationTime());
            Long endTimeUnix = data.getRoom().getEndTime();
            LocalDateTime end = endTimeUnix != null ? unixToLocal(endTimeUnix) : LocalDateTime.now();
            Long duration = null;
            if (data.getRoom().getCreationTime() != null && endTimeUnix != null) {
                duration = endTimeUnix - data.getRoom().getCreationTime();
            } else if (data.getRoom().getCreationTime() != null) {
                duration = Instant.now().getEpochSecond() - data.getRoom().getCreationTime();
            }
            // Long duration = (data.getRoom().getCreationTime() != null && endTimeUnix != null) 
            //                 ? endTimeUnix - data.getRoom().getCreationTime() 
            //                 : 0L;

            // --- A. LƯU MONGODB (Chi tiết) ---
            List<Meeting.ParticipantInfo> mongoParticipants = new ArrayList<>();
            for (var p : trackedParticipants) {
                mongoParticipants.add(new Meeting.ParticipantInfo(
                    p.getIdentity(), p.getName(), unixToLocal(p.getJoinedAt()), unixToLocal(p.getLeftAt())
                ));
            }

            Meeting mongoMeeting = Meeting.builder()
                    .roomId(roomId)
                    .startTime(start).endTime(end).durationSeconds(duration)
                    .participants(mongoParticipants)
                    .build();
            mongoRepository.save(mongoMeeting);
            log.info("✅ Saved to MongoDB");

            // --- B. LƯU POSTGRESQL (Quan hệ) ---
            MeetingLog pgLog = MeetingLog.builder()
                    .roomId(roomId)
                    .startTime(start).endTime(end).durationSeconds(duration)
                    .build();
            MeetingLog savedPgLog = postgresRepository.save(pgLog);

            List<ParticipantHistory> histories = new ArrayList<>();
            for (var p : trackedParticipants) {
                // Chỉ lưu nếu tìm thấy User trong DB (Thành viên chính thức)
                Optional<User> userOpt = userRepository.findByEmail(p.getIdentity());
                if (userOpt.isPresent()) {
                    histories.add(ParticipantHistory.builder()
                            .user(userOpt.get())
                            .meetingLog(savedPgLog)
                            .joinedAt(unixToLocal(p.getJoinedAt()))
                            .leftAt(unixToLocal(p.getLeftAt()))
                            .build());
                }
            }
            if (!histories.isEmpty()) {
                historyRepo.saveAll(histories);
                log.info("✅ Saved {} records to PostgreSQL", histories.size());
            }

        } catch (Exception e) {
            log.error("❌ Error in MeetingEventListener: {}", e.getMessage(), e);
        }
    }

    private LocalDateTime unixToLocal(Long timestamp) {
        if (timestamp == null) return null;
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault());
    }
}
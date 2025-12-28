package anhtaikhau.example.video_call_web_app.controller;

import anhtaikhau.example.video_call_web_app.model.MeetingLog;
import anhtaikhau.example.video_call_web_app.model.ParticipantHistory;
import anhtaikhau.example.video_call_web_app.model.User;
import anhtaikhau.example.video_call_web_app.repository.UserRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserRepository userRepository;

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getMyProfile(@AuthenticationPrincipal User userDetails) {
        // Lấy thông tin mới nhất từ DB (vì userDetails có thể cũ)
        User user = userRepository.findById(userDetails.getId()).orElseThrow();

        // Hibernate sẽ tự động JOIN bảng participant_history nhờ quan hệ @OneToMany
        List<MeetingHistoryDto> historyDtos = user.getMeetingHistories().stream()
                .map(h -> MeetingHistoryDto.builder()
                        .roomId(h.getMeetingLog().getRoomId())
                        .joinedAt(h.getJoinedAt())
                        .durationSeconds(h.getMeetingLog().getDurationSeconds())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(UserProfileResponse.builder()
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .history(historyDtos)
                .build());
    }

    @Data
    @Builder
    static class UserProfileResponse {
        private String email;
        private String fullName;
        private String role;
        private List<MeetingHistoryDto> history;
    }

    @Data
    @Builder
    static class MeetingHistoryDto {
        private String roomId;
        private LocalDateTime joinedAt;
        private Long durationSeconds;
    }
}
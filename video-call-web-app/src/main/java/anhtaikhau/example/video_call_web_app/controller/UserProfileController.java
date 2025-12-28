package anhtaikhau.example.video_call_web_app.controller;

import anhtaikhau.example.video_call_web_app.model.MeetingLog;
import anhtaikhau.example.video_call_web_app.model.ParticipantHistory;
import anhtaikhau.example.video_call_web_app.model.User;
import anhtaikhau.example.video_call_web_app.repository.UserRepository;
import anhtaikhau.example.video_call_web_app.repository.ParticipantHistoryRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserRepository userRepository;
    private final ParticipantHistoryRepository historyRepository; 

     // 1. API Lấy thông tin cơ bản (Bỏ list history đi cho nhẹ)
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getMyProfile(@AuthenticationPrincipal User userDetails) {
        User user = userRepository.findById(userDetails.getId()).orElseThrow();

        return ResponseEntity.ok(UserProfileResponse.builder()
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build());
    }

    // 2. API MỚI: Lấy lịch sử phân trang
    // GET /api/v1/users/history?page=0&size=5
    @GetMapping("/history")
    public ResponseEntity<Page<MeetingHistoryDto>> getMyHistory(
            @AuthenticationPrincipal User userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        // Lấy lịch sử của chính user đang đăng nhập
        // Sort mặc định: Mới nhất lên đầu (joinedAt DESC)
        Page<ParticipantHistory> pageResult = historyRepository.findByUser_Id(
                userDetails.getId(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "joinedAt"))
        );

        // Convert sang DTO
        Page<MeetingHistoryDto> dtoPage = pageResult.map(h -> MeetingHistoryDto.builder()
                .roomId(h.getMeetingLog().getRoomId())
                .joinedAt(h.getJoinedAt())
                .durationSeconds(h.getMeetingLog().getDurationSeconds())
                .build());

        return ResponseEntity.ok(dtoPage);
    }

    @Data
    @Builder
    static class UserProfileResponse {
        private String email;
        private String fullName;
        private String role;
        //private List<MeetingHistoryDto> history;
    }

    @Data
    @Builder
    static class MeetingHistoryDto {
        private String roomId;
        private LocalDateTime joinedAt;
        private Long durationSeconds;
    }
}
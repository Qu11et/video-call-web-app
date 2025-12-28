package anhtaikhau.example.video_call_web_app.controller;

import anhtaikhau.example.video_call_web_app.model.Meeting;
import anhtaikhau.example.video_call_web_app.service.MeetingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/meetings") // Prefix dành riêng cho Admin
@RequiredArgsConstructor
public class AdminMeetingController {

    private final MeetingService meetingService;

    /**
     * GET /api/v1/admin/meetings?page=0&size=10
     * Lấy danh sách lịch sử cuộc họp
     */
    @GetMapping
    public ResponseEntity<Page<Meeting>> getAllMeetings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        // Mặc định sort theo startTime giảm dần (mới nhất lên đầu)
        // Lưu ý: Nếu trong repository dùng findAllByOrderByStartTimeDesc thì không cần Sort ở đây nữa
        // Nhưng để linh hoạt, ta dùng PageRequest chuẩn
        return ResponseEntity.ok(
            meetingService.getAllMeetings(PageRequest.of(page, size))
        );
    }

    /**
     * GET /api/v1/admin/meetings/{id}
     * Xem chi tiết 1 cuộc họp
     */
    @GetMapping("/{id}")
    public ResponseEntity<Meeting> getMeetingDetails(@PathVariable String id) {
        return ResponseEntity.ok(meetingService.getMeetingById(id));
    }
}
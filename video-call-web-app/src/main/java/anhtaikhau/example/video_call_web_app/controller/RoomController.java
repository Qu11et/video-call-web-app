package anhtaikhau.example.video_call_web_app.controller;

import anhtaikhau.example.video_call_web_app.service.RoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms") 
public class RoomController {

    private static final Logger logger = LoggerFactory.getLogger(RoomController.class);
    
    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> createRoom() {
        // (Trong thực tế có thể muốn kiểm tra xem ID đã tồn tại chưa)
        String newRoomId = UUID.randomUUID().toString().substring(0, 8);

        roomService.createNewRoom(newRoomId);
        
        logger.info("Tạo phòng mới với ID: {}", newRoomId);

        return ResponseEntity.ok(Map.of("roomId", newRoomId));
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<Map<String, Object>> getRoomInfo(@PathVariable String roomId) {
        Set<String> participants = roomService.getParticipants(roomId);

        if (participants == null) {
            logger.warn("GET /api/rooms/{}: Phòng không tìm thấy", roomId);
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> roomInfo = Map.of(
            "roomId", roomId,
            "participantCount", participants.size()
        );
        return ResponseEntity.ok(roomInfo);
    }

    @PostMapping("/{roomId}/join")
    public ResponseEntity<Map<String, String>> canJoinRoom(@PathVariable String roomId) {
        if (roomService.roomExists(roomId)) {
            logger.info("POST /api/rooms/{}/join: Phòng hợp lệ", roomId);
            return ResponseEntity.ok(Map.of("status", "Phòng hợp lệ"));
        } else {
            logger.warn("POST /api/rooms/{}/join: Phòng không tìm thấy", roomId);
            return ResponseEntity.notFound().build(); 
        }
    }
}
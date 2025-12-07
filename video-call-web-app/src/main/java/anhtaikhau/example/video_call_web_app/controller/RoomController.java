package anhtaikhau.example.video_call_web_app.controller;

import anhtaikhau.example.video_call_web_app.service.RoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import anhtaikhau.example.video_call_web_app.service.LiveKitService;

@RestController
@RequestMapping("/api/rooms") 
public class RoomController {

    private static final Logger logger = LoggerFactory.getLogger(RoomController.class);
    
    private final RoomService roomService;
    private final LiveKitService liveKitService; // <--- Inject Service mới

    public RoomController(RoomService roomService, LiveKitService liveKitService) {
        this.roomService = roomService;
        this.liveKitService = liveKitService;
    }

   /**
     * API Tạo phòng: POST /api/rooms/create
     * Body: { "type": "P2P" } hoặc { "type": "GROUP" }
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> createRoom(@RequestBody Map<String, String> payload) {
        String newRoomId = UUID.randomUUID().toString().substring(0, 8);
        
        // Lấy loại phòng từ request, mặc định là P2P nếu không gửi
        String type = payload.getOrDefault("type", "P2P");
        
        // Gọi service tạo phòng
        roomService.createNewRoom(newRoomId, type);
        
        return ResponseEntity.ok(Map.of(
            "roomId", newRoomId,
            "type", type
        ));
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

    /**
     * API Kiểm tra/Join phòng: POST /api/rooms/{id}/join
     * Trả về thêm "type" để Frontend biết đường điều hướng
     */
    @PostMapping("/{roomId}/join")
    public ResponseEntity<Map<String, Object>> canJoinRoom(@PathVariable String roomId) {
        if (roomService.roomExists(roomId)) {
            String type = roomService.getRoomType(roomId);
            
            return ResponseEntity.ok(Map.of(
                "status", "Phòng hợp lệ",
                "type", type != null ? type : "P2P" // Trả về loại phòng
            ));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * API MỚI: Lấy token để join vào LiveKit Room (SFU)
     * POST /api/rooms/{roomId}/token
     * Body: { "userId": "...", "name": "..." }
     */
    @PostMapping("/{roomId}/token")
    public ResponseEntity<Map<String, String>> getLiveKitToken(
            @PathVariable String roomId,
            @RequestBody Map<String, String> payload
    ) {
        String userId = payload.get("userId");
        String name = payload.getOrDefault("name", "User-" + userId);

        // 1. Kiểm tra phòng có tồn tại và đúng là loại GROUP không
        if (!roomService.roomExists(roomId)) {
             return ResponseEntity.notFound().build();
        }
        
        // (Optional) Kiểm tra loại phòng
        // if (!"GROUP".equals(roomService.getRoomType(roomId))) { ... }

        // 2. Tạo Token
        String token = liveKitService.createToken(roomId, userId, name);

        // 3. Trả về token và URL của LiveKit Server (để Frontend kết nối)
        return ResponseEntity.ok(Map.of(
            "token", token,
            "serverUrl", "ws://localhost:7880" // Địa chỉ LiveKit Docker
        ));
    }
}
package anhtaikhau.example.video_call_web_app.controller;

import anhtaikhau.example.video_call_web_app.service.RoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.Objects;

@Controller
public class SignalController {

    private static final Logger logger = LoggerFactory.getLogger(SignalController.class);

    private final RoomService roomService;
    private final SimpMessageSendingOperations messagingTemplate;

    public SignalController(RoomService roomService, SimpMessageSendingOperations messagingTemplate) {
        this.roomService = roomService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/join")
    public void joinRoom(@Payload Map<String, String> message, SimpMessageHeaderAccessor headerAccessor) {
        // --- SỬA ĐỔI: Lấy ID từ Frontend gửi lên thay vì Session ID của WebSocket ---
        // Frontend gửi: { roomId: "...", myID: "..." }
        String userId = message.get("myID"); 
        String roomId = message.get("roomId");

        // Fallback: Nếu không có myID thì mới dùng Session ID (đề phòng)
        if (userId == null) userId = headerAccessor.getSessionId();

        if (roomId == null || roomId.isEmpty()) return;

        roomService.joinRoom(roomId, userId);

        // Thông báo user-joined với đúng ID mà frontend biết
        Map<String, String> anouncement = Map.of(
            "type", "user-joined",
            "sessionId", userId 
        );

        messagingTemplate.convertAndSend("/topic/room/" + roomId, anouncement);
        logger.info("User {} ({}) đã tham gia phòng {}", userId, headerAccessor.getSessionId(), roomId);
    }

    @MessageMapping("/signal")
    public void sendSignal(@Payload Map<String, Object> message, SimpMessageHeaderAccessor headerAccessor) {
        // --- SỬA ĐỔI: Không ghi đè senderSessionId bằng WebSocket ID nữa ---
        // Frontend đã tự gửi "senderSessionId" trong body rồi.
        
        String senderId = (String) message.get("senderSessionId");
        
        // Chỉ lấy roomId để biết gửi đi đâu
        String roomId = roomService.getRoomForUser(senderId); // Lưu ý: RoomService cần update để map theo ID này
        
        // Nếu RoomService đang map theo WebSocketSessionId thì ta cần sửa logic một chút.
        // Để đơn giản cho demo: Ta vẫn dùng WebSocketSessionId để tìm phòng, 
        // nhưng message gửi đi thì giữ nguyên ID của frontend.
        String wsSessionId = headerAccessor.getSessionId();
        if (roomId == null) {
            // Thử tìm bằng WS Session ID (vì RoomService.joinRoom ở trên có thể đã dùng userId)
            // Trong bản sửa joinRoom ở trên, ta đã dùng userId để join, nên:
            roomId = roomService.getRoomForUser(userIdFromMessage(message, wsSessionId));
        }

        if (roomId != null) {
            logger.debug("Chuyển tiếp tín hiệu từ {} trong phòng {}: {}", senderId, roomId, message.get("type"));
            messagingTemplate.convertAndSend("/topic/room/" + roomId, message);
        }
    }
    
    private String userIdFromMessage(Map<String, Object> message, String defaultId) {
        String id = (String) message.get("senderSessionId");
        return id != null ? id : defaultId;
    }
}
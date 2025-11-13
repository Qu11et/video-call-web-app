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
        String sessionId = headerAccessor.getSessionId();
        String roomId = message.get("roomId");

        if (roomId == null || roomId.isEmpty()) {
            logger.warn("User {}_cố gắng tham gia phòng rỗng", sessionId);
            return;
        }

        roomService.joinRoom(roomId, sessionId);

        Map<String, String> anouncement = Map.of(
            "type", "user-joined",
            "sessionId", sessionId
        );

        messagingTemplate.convertAndSend("/topic/room/" + roomId, anouncement);
        logger.info("Đã thông báo user-joined cho phòng {}", roomId);

        
        // TODO (Giai đoạn 2 - tiếp theo): 
        // 1. Thêm user này vào một phòng
        // 2. Gửi tin nhắn cho những người khác trong phòng
    }

    @MessageMapping("/signal")
    public void sendSignal(@Payload Map<String, Object> message, SimpMessageHeaderAccessor headerAccessor) {
        String senderSessionId = headerAccessor.getSessionId();
        String roomId = roomService.getRoomForUser(senderSessionId);

        if (roomId == null) {
            logger.warn("Tín hiệu bị bỏ qua: User {}_không ở trong phòng nào", senderSessionId);
            return;
        }

        message.put("senderSessionId", senderSessionId);

        logger.debug("Chuyển tiếp tín hiệu từ {}_trong phòng {}: {}", senderSessionId, roomId, message.get("type"));
        messagingTemplate.convertAndSend("/topic/room/" + roomId, message);

        // TODO (Giai đoạn 4):
        // 1. Phân tích tin nhắn
        // 2. Chuyển tiếp tin nhắn này đến user cụ thể (peer)      
    }
}

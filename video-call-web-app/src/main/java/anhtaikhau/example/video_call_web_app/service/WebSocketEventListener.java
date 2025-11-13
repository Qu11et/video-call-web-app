package anhtaikhau.example.video_call_web_app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

@Component 
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final RoomService roomService;
    private final SimpMessageSendingOperations messagingTemplate; 

    public WebSocketEventListener(RoomService roomService, SimpMessageSendingOperations messagingTemplate) {
        this.roomService = roomService;
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        String sessionId = event.getMessage().getHeaders().get("simpSessionId").toString();
        logger.info("[SỰ KIỆN] User MỚI đã kết nối: {}", sessionId);
        
        // TODO (Giai đoạn 2 - tiếp theo):
        // Lưu lại user này (ví dụ: trong một Map<SessionId, UserInfo>)
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        logger.warn("[SỰ KIỆN] User đã ngắt kết nối: {}", sessionId);
        
        String roomId = roomService.leaveRoom(sessionId);

        if (roomId != null) {
            Map<String, String> message = Map.of(
                "type", "user-left",
                "sessionId", sessionId
            );
            
            messagingTemplate.convertAndSend("/topic/room/" + roomId, message);
        }
    }
}

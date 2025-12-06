package anhtaikhau.example.video_call_web_app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomService {

    private static final Logger logger = LoggerFactory.getLogger(RoomService.class);

    private final Map<String, Set<String>> rooms = new ConcurrentHashMap<>();
    private final Map<String, String> userRoomMap = new ConcurrentHashMap<>();

    public void createNewRoom(String roomId) {
        rooms.put(roomId, ConcurrentHashMap.newKeySet());
        logger.info("Đã khởi tạo phòng trống với ID: {}", roomId);
    }

    public void joinRoom(String roomId, String sessionId) {
        Set<String> participants = rooms.computeIfAbsent(
            roomId, 
            key -> ConcurrentHashMap.newKeySet()
        );
        
        participants.add(sessionId);
        
        userRoomMap.put(sessionId, roomId);

        logger.info("User {}_đã tham gia phòng {}. Tổng số người: {}", sessionId, roomId, participants.size());
    }

    public String leaveRoom(String sessionId) {
        String roomId = userRoomMap.remove(sessionId);
        if (roomId != null) {
            Set<String> participants = rooms.get(roomId);
            if (participants != null) {
                participants.remove(sessionId);
                logger.info("User {}_đã rời phòng {}", sessionId, roomId);
                if (participants.isEmpty()) {
                    rooms.remove(roomId);
                    logger.info("Phòng {}_hiện trống và đã bị xóa.", roomId);
                }
            }
        }
        return roomId; 
    }

    public String getRoomForUser(String sessionId) {
        return userRoomMap.get(sessionId);
    }

    public Set<String> getParticipants(String roomId) {
        return rooms.get(roomId);
    }

    public boolean roomExists(String roomId) {
        return rooms.containsKey(roomId);
    }
}

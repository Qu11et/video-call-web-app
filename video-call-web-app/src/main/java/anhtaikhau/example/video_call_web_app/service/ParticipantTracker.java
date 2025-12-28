package anhtaikhau.example.video_call_web_app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParticipantTracker {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String KEY_PREFIX = "meeting:participants:";
    private static final long TTL_HOURS = 24;

    // Thêm tham số 'sid'
    public void addParticipant(String roomId, String sid, String identity, String name, Long joinedAt) {
        try {
            String key = KEY_PREFIX + roomId;
            // Lưu sid vào object để sau này lấy ra dùng
            ParticipantData data = new ParticipantData(sid, identity, name, joinedAt, null);
            
            String json = objectMapper.writeValueAsString(data);
            
            // Dùng Hash: Field là SID -> Dễ dàng update đúng người, đúng session
            redisTemplate.opsForHash().put(key, sid, json);
            redisTemplate.expire(key, Duration.ofHours(TTL_HOURS));
            
            log.info("✅ Redis tracked join: {} (SID: {}) in room {}", identity, sid, roomId);
        } catch (Exception e) {
            log.error("Error adding participant", e);
        }
    }

    // Thêm tham số 'sid' để tìm chính xác record cần update
    public void removeParticipant(String roomId, String sid, Long leftAt) {
        try {
            String key = KEY_PREFIX + roomId;
            
            // 1. Lấy dữ liệu cũ bằng SID (O(1) - cực nhanh)
            Object rawJson = redisTemplate.opsForHash().get(key, sid);
            
            if (rawJson != null) {
                ParticipantData data = objectMapper.readValue(rawJson.toString(), ParticipantData.class);
                
                // 2. Cập nhật thời gian rời
                // Nếu leftAt từ webhook bị null (đôi khi xảy ra), ta dùng thời gian hiện tại
                data.setLeftAt(leftAt != null && leftAt > 0 ? leftAt : System.currentTimeMillis() / 1000);
                
                // 3. Lưu đè lại vào Redis
                redisTemplate.opsForHash().put(key, sid, objectMapper.writeValueAsString(data));
                
                log.info("✅ Redis tracked left: {} (SID: {})", data.getIdentity(), sid);
            } else {
                log.warn("⚠️ Cannot find participant session to update leftAt: {}", sid);
            }
        } catch (Exception e) {
            log.error("Error removing participant", e);
        }
    }

    public List<ParticipantData> getAndRemoveParticipants(String roomId) {
        String key = KEY_PREFIX + roomId;
        List<ParticipantData> results = new ArrayList<>();
        
        try {
            // Lấy tất cả các entries trong Hash
            Map<Object, Object> rawMap = redisTemplate.opsForHash().entries(key);
            
            for (Object rawJson : rawMap.values()) {
                try {
                    results.add(objectMapper.readValue(rawJson.toString(), ParticipantData.class));
                } catch (Exception e) {
                    log.error("Error parsing participant data", e);
                }
            }
            
            // Xóa key sau khi lấy xong
            if (!results.isEmpty()) {
                redisTemplate.delete(key);
            }
        } catch (Exception e) {
            log.error("Error retrieving participants", e);
        }
        
        log.info("📋 Retrieved {} sessions from Redis for room {}", results.size(), roomId);
        return results;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantData {
        private String sid;      // Session ID
        private String identity; // User ID
        private String name;     // Tên hiển thị
        private Long joinedAt;   // Unix timestamp
        private Long leftAt;     // Unix timestamp
    }
}
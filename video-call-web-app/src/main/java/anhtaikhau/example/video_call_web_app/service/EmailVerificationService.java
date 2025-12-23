package anhtaikhau.example.video_call_web_app.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final StringRedisTemplate redisTemplate;
    
    // Prefix cho key trong Redis để dễ quản lý
    private static final String KEY_PREFIX = "verify-email:";
    private static final long EXPIRATION_HOURS = 24;

    public String generateVerificationToken(String email) {
        String token = UUID.randomUUID().toString();
        // Lưu: verify-email:123-abc -> user@gmail.com
        redisTemplate.opsForValue().set(
            KEY_PREFIX + token, 
            email, 
            Duration.ofHours(EXPIRATION_HOURS)
        );
        return token;
    }

    public String validateToken(String token) {
        String email = redisTemplate.opsForValue().get(KEY_PREFIX + token);
        if (email == null) {
            throw new RuntimeException("Token không hợp lệ hoặc đã hết hạn");
        }
        // Xóa token sau khi dùng xong (One-time use)
        redisTemplate.delete(KEY_PREFIX + token);
        return email;
    }
}
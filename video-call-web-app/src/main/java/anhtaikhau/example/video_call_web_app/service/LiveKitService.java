package anhtaikhau.example.video_call_web_app.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class LiveKitService {

    // Lấy từ file livekit.yaml: keys: devkey: secret
    private final String API_KEY = "devkey";
    // Phải khớp y hệt với livekit.yaml
    private final String API_SECRET = "my_super_secure_secret_key_for_dev_only_123"; 

    public String createToken(String roomId, String userId, String participantName) {
        Algorithm algorithm = Algorithm.HMAC256(API_SECRET);

        // Các quyền hạn (Video grants)
        Map<String, Object> videoGrants = new HashMap<>();
        videoGrants.put("roomJoin", true);
        videoGrants.put("room", roomId);
        videoGrants.put("canPublish", true);
        videoGrants.put("canSubscribe", true);

        // Tạo JWT Token
        String token = JWT.create()
                .withIssuer(API_KEY)
                .withExpiresAt(new Date(System.currentTimeMillis() + 60 * 60 * 1000)) // Hết hạn sau 1 giờ
                .withSubject(userId)
                .withClaim("video", videoGrants)
                .withClaim("name", participantName) // Tên hiển thị trong cuộc họp
                .sign(algorithm);

        return token;
    }
}

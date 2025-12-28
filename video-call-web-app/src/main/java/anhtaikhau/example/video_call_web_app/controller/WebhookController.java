package anhtaikhau.example.video_call_web_app.controller;

import anhtaikhau.example.video_call_web_app.dto.webhook.LiveKitWebhookEvent;
import anhtaikhau.example.video_call_web_app.event.MeetingEndedEvent;
import anhtaikhau.example.video_call_web_app.service.ParticipantTracker;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    // Không cần MeetingService nữa vì EventListener sẽ lo việc đó
    private final ObjectMapper objectMapper;
    private final ParticipantTracker participantTracker;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${livekit.api.key:devkey}")
    private String API_KEY;
    
    @Value("${livekit.api.secret:my_super_secure_secret_key_for_dev_only_123}")
    private String API_SECRET;

    @PostMapping("/livekit")
    public ResponseEntity<String> receiveWebhook(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody String rawBody
    ) {
        // Log vừa phải để tránh spam console khi chạy production
        log.info("📥 Webhook received. Auth: {}", authHeader != null ? "Present" : "Missing");

        if (!isValidWebhook(authHeader, rawBody)) {
            log.warn("❌ Webhook authentication failed!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Signature");
        }

        try {
            LiveKitWebhookEvent event = objectMapper.readValue(rawBody, LiveKitWebhookEvent.class);
            String eventType = event.getEvent();
            String roomId = event.getRoom().getName();

            log.info("✅ Event: {} | Room: {}", eventType, roomId);

            // --- XỬ LÝ LOGIC ---
            
            // 1. Nếu là kết thúc phòng -> Bắn Async Event để xử lý nặng (Lưu DB)
            if ("room_finished".equals(eventType)) {
                log.info("🚀 Publishing MeetingEndedEvent for room: {}", roomId);
                eventPublisher.publishEvent(new MeetingEndedEvent(this, event));
                
                // Trả về ngay lập tức, không chờ DB lưu xong
                return ResponseEntity.ok("Webhook Received - Processing Async");
            }

            // 2. Các sự kiện tham gia/rời -> Xử lý nhanh vào Redis (ParticipantTracker)
            // Vì thao tác Redis rất nhanh (ms) nên có thể để trong luồng chính
            switch (eventType) {
                case "participant_joined":
                    if (event.getParticipant() != null) {
                        participantTracker.addParticipant(
                            roomId,
                            event.getParticipant().getSid(),
                            event.getParticipant().getIdentity(),
                            event.getParticipant().getName(),
                            event.getParticipant().getJoinedAt()
                        );
                    }
                    break;
                    
                case "participant_left":
                    if (event.getParticipant() != null) {
                        Long leftAt = event.getParticipant().getLeftAt();
                        // Fallback nếu LiveKit không gửi leftAt
                        if (leftAt == null || leftAt == 0) {
                            leftAt = event.getCreatedAt(); 
                        }

                        participantTracker.removeParticipant(
                            roomId,
                            event.getParticipant().getSid(),
                            leftAt
                        );
                    }
                    break;
                    
                default:
                    // Các event khác (track_published, etc.) bỏ qua để đỡ rác log
                    log.debug("ℹ️ Ignored event type: {}", eventType);
            }

            return ResponseEntity.ok("Webhook Received");

        } catch (Exception e) {
            log.error("❌ Error processing webhook: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error processing webhook");
        }
    }

    private boolean isValidWebhook(String authHeader, String body) {
        if (authHeader == null || body == null) return false;

        try {
            String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;

            JWTVerifier verifier = JWT.require(Algorithm.HMAC256(API_SECRET))
                    .withIssuer(API_KEY)
                    .build();
            
            DecodedJWT jwt = verifier.verify(token);
            String sha256Claim = jwt.getClaim("sha256").asString();
            String hashBody = sha256(body);

            if (!sha256Claim.equals(hashBody)) {
                log.warn("❌ Body hash mismatch. Expected: {}, Computed: {}", sha256Claim, hashBody);
                return false;
            }
            return true;

        } catch (Exception e) {
            log.error("❌ Verify Exception: {}", e.getMessage());
            return false;
        }
    }

    private String sha256(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}
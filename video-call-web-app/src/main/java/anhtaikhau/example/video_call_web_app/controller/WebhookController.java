package anhtaikhau.example.video_call_web_app.controller;

import anhtaikhau.example.video_call_web_app.dto.webhook.LiveKitWebhookEvent;
import anhtaikhau.example.video_call_web_app.service.MeetingService;
import anhtaikhau.example.video_call_web_app.service.ParticipantTracker;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    private final MeetingService meetingService;
    private final ObjectMapper objectMapper;
    private final ParticipantTracker participantTracker;

    @Value("${livekit.api.key:devkey}")
    private String API_KEY;
    
    @Value("${livekit.api.secret:my_super_secure_secret_key_for_dev_only_123}")
    private String API_SECRET;

    @PostMapping("/livekit")
    public ResponseEntity<String> receiveWebhook(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody String rawBody
    ) {
        log.info("=== WEBHOOK RECEIVED ===");
        log.info("Authorization Header: {}", authHeader);
        log.info("Body length: {}", rawBody != null ? rawBody.length() : 0);
        log.info("📄 Raw Body: {}", rawBody);

        if (!isValidWebhook(authHeader, rawBody)) {
            log.warn("❌ Webhook xác thực thất bại!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Signature");
        }

        try {
            LiveKitWebhookEvent event = objectMapper.readValue(rawBody, LiveKitWebhookEvent.class);
            log.info("✅ Webhook Event: {}, Room: {}", event.getEvent(), event.getRoom().getName());

            String roomId = event.getRoom().getName();

            // XỬ LÝ CÁC EVENT
            switch (event.getEvent()) {
                case "room_finished":
                    log.info("🎯 Processing room_finished event");
                    meetingService.handleRoomFinished(event, participantTracker);
                    break;
                    
                case "participant_joined":
                    if (event.getParticipant() != null) {
                        participantTracker.addParticipant(
                            roomId,
                            event.getParticipant().getSid(), // <--- Lấy SID từ Event
                            event.getParticipant().getIdentity(),
                            event.getParticipant().getName(),
                            event.getParticipant().getJoinedAt()
                        );
                    }
                    break;
                    
                case "participant_left":
                    if (event.getParticipant() != null) {
                        // Lưu ý: Event participant_left có thể chứa leftAt hoặc không
                        // Nếu không có, ta dùng createdAt của webhook làm thời gian rời
                        Long leftAt = event.getParticipant().getLeftAt();
                        if (leftAt == null || leftAt == 0) {
                            leftAt = event.getCreatedAt(); // Dùng thời gian sự kiện
                        }

                        participantTracker.removeParticipant(
                            roomId,
                            event.getParticipant().getSid(), // <--- Dùng SID để tìm đúng session
                            leftAt
                        );
                    }
                    break;
                    
                // case "track_unpublished":
                //     log.info("📹 Track unpublished in room: {}", roomId);
                //     break;
                    
                default:
                    log.info("ℹ️ Unhandled event type: {}", event.getEvent());
            }

            return ResponseEntity.ok("Webhook Received");

        } catch (Exception e) {
            log.error("❌ Lỗi xử lý webhook: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error processing webhook");
        }
    }

    private boolean isValidWebhook(String authHeader, String body) {
        if (authHeader == null || body == null) {
            log.warn("❌ Missing authHeader or body");
            return false;
        }

        try {
            String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;

            log.info("🔑 Verifying JWT with API_KEY: {}", API_KEY);
            
            JWTVerifier verifier = JWT.require(Algorithm.HMAC256(API_SECRET))
                    .withIssuer(API_KEY)
                    .build();
            
            DecodedJWT jwt = verifier.verify(token);
            log.info("✅ JWT verified successfully");

            // Kiểm tra hash body
            String sha256Claim = jwt.getClaim("sha256").asString();
            String hashBody = sha256(body);

            log.info("🔍 Comparing hashes:");
            log.info("   Expected (JWT): {}", sha256Claim);
            log.info("   Computed:       {}", hashBody);

            if (!sha256Claim.equals(hashBody)) {
                log.warn("❌ Body hash mismatch");
                return false;
            }

            log.info("✅ Body hash verified");
            return true;

        } catch (Exception e) {
            log.error("❌ Verify Webhook Exception: {}", e.getMessage(), e);
            return false;
        }
    }

    private String sha256(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        
        // ✅ SỬA: Dùng Base64 Standard (có padding "=") thay vì URL-safe
        // LiveKit JWT claim dùng standard Base64
        return Base64.getEncoder().encodeToString(hash);
    }
}
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
        // ✅ THÊM: Log dòng phân cách để dễ đọc
        log.info("=".repeat(80));
        log.info("📡 LIVEKIT WEBHOOK RECEIVED");
        log.info("Timestamp: {}", System.currentTimeMillis());
        log.info("Auth Header: {}", authHeader != null ? "Present ✅" : "Missing ❌");
        log.info("Body Length: {} bytes", rawBody != null ? rawBody.length() : 0);
        
        // ✅ THÊM: Log raw body (chỉ trong dev mode)
        if (log.isDebugEnabled()) {
            log.debug("Raw Body: {}", rawBody);
        }

        if (!isValidWebhook(authHeader, rawBody)) {
            log.error("❌ WEBHOOK AUTHENTICATION FAILED!");
            log.error("=".repeat(80));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Signature");
        }

        try {
            LiveKitWebhookEvent event = objectMapper.readValue(rawBody, LiveKitWebhookEvent.class);
            String eventType = event.getEvent();
            String roomId = event.getRoom().getName();

            // ✅ THÊM: Log chi tiết event
            log.info("📋 EVENT DETAILS:");
            log.info("   Type: {}", eventType);
            log.info("   Room ID: {}", roomId);
            log.info("   Room SID: {}", event.getRoom().getSid());
            log.info("   Created At: {}", event.getCreatedAt());
            
            // ✅ THÊM: Log participant info (nếu có)
            if (event.getParticipant() != null) {
                log.info("👤 PARTICIPANT INFO:");
                log.info("   Identity: {}", event.getParticipant().getIdentity());
                log.info("   Name: {}", event.getParticipant().getName());
                log.info("   SID: {}", event.getParticipant().getSid());
                log.info("   State: {}", event.getParticipant().getState());
                log.info("   Joined At: {}", event.getParticipant().getJoinedAt());
                
                if (event.getParticipant().getLeftAt() != null && event.getParticipant().getLeftAt() > 0) {
                    long duration = event.getParticipant().getLeftAt() - event.getParticipant().getJoinedAt();
                    log.info("   Left At: {} (Duration: {}s)", 
                            event.getParticipant().getLeftAt(), 
                            duration / 1000);
                }
            }
            
            // ✅ THÊM: Log track info (nếu có)
            // if (event.getTrack() != null) {
            //     log.info("🎥 TRACK INFO:");
            //     log.info("   Track SID: {}", event.getTrack().getSid());
            //     log.info("   Type: {}", event.getTrack().getType());
            //     log.info("   Source: {}", event.getTrack().getSource());
            //     log.info("   Muted: {}", event.getTrack().isMuted());
            // }

            // --- XỬ LÝ LOGIC ---
            
            // 1. Room finished event
            if ("room_finished".equals(eventType)) {
                log.info("🏁 ROOM FINISHED - Publishing async event");
                log.info("   Room: {}", roomId);
                
                eventPublisher.publishEvent(new MeetingEndedEvent(this, event));
                
                log.info("✅ Event published successfully");
                log.info("=".repeat(80));
                return ResponseEntity.ok("Webhook Received - Processing Async");
            }

            // 2. Participant events
            switch (eventType) {
                case "participant_joined":
                    log.info("👋 PARTICIPANT JOINED EVENT");
                    if (event.getParticipant() != null) {
                        log.info("   Adding to tracker...");
                        participantTracker.addParticipant(
                            roomId,
                            event.getParticipant().getSid(),
                            event.getParticipant().getIdentity(),
                            event.getParticipant().getName(),
                            event.getParticipant().getJoinedAt()
                        );
                        log.info("   ✅ Participant added to tracker");
                        
                        // ✅ THÊM: Log số lượng participants hiện tại
                        // (Giả sử bạn có method này trong ParticipantTracker)
                        // int count = participantTracker.getParticipantCount(roomId);
                        // log.info("   Current participants in room: {}", count);
                    }
                    break;
                    
                case "participant_left":
                    log.info("👋 PARTICIPANT LEFT EVENT");
                    if (event.getParticipant() != null) {
                        Long leftAt = event.getParticipant().getLeftAt();
                        if (leftAt == null || leftAt == 0) {
                            leftAt = event.getCreatedAt();
                            log.warn("   ⚠️ leftAt is null, using createdAt as fallback");
                        }

                        log.info("   Removing from tracker...");
                        participantTracker.removeParticipant(
                            roomId,
                            event.getParticipant().getSid(),
                            leftAt
                        );
                        log.info("   ✅ Participant removed from tracker");
                    }
                    break;
                
                // ✅ THÊM: Log track events
                // case "track_published":
                //     log.info("📤 TRACK PUBLISHED");
                //     log.info("   Participant: {}", event.getParticipant() != null ? event.getParticipant().getIdentity() : "N/A");
                //     log.info("   Track Type: {}", event.getTrack() != null ? event.getTrack().getType() : "N/A");
                //     break;
                
                // case "track_unpublished":
                //     log.info("📥 TRACK UNPUBLISHED");
                //     log.info("   Participant: {}", event.getParticipant() != null ? event.getParticipant().getIdentity() : "N/A");
                //     break;
                    
                default:
                    log.debug("ℹ️ Event type '{}' - no specific handling", eventType);
            }

            log.info("✅ WEBHOOK PROCESSED SUCCESSFULLY");
            log.info("=".repeat(80));
            return ResponseEntity.ok("Webhook Received");

        } catch (Exception e) {
            log.error("=".repeat(80));
            log.error("❌ ERROR PROCESSING WEBHOOK");
            log.error("Error Type: {}", e.getClass().getSimpleName());
            log.error("Error Message: {}", e.getMessage());
            log.error("Stack Trace:", e);
            log.error("=".repeat(80));
            return ResponseEntity.internalServerError().body("Error processing webhook");
        }
    }

    private boolean isValidWebhook(String authHeader, String body) {
        if (authHeader == null || body == null) {
            log.warn("❌ Validation failed: authHeader or body is null");
            return false;
        }

        try {
            String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
            
            // ✅ THÊM: Log token preview
            log.debug("🔐 Validating webhook signature...");
            log.debug("   Token preview: {}...", token.substring(0, Math.min(20, token.length())));

            JWTVerifier verifier = JWT.require(Algorithm.HMAC256(API_SECRET))
                    .withIssuer(API_KEY)
                    .build();
            
            DecodedJWT jwt = verifier.verify(token);
            String sha256Claim = jwt.getClaim("sha256").asString();
            String hashBody = sha256(body);

            // ✅ THÊM: Log hash comparison
            log.debug("   Expected SHA256: {}", sha256Claim);
            log.debug("   Computed SHA256: {}", hashBody);

            if (!sha256Claim.equals(hashBody)) {
                log.error("❌ BODY HASH MISMATCH!");
                log.error("   Expected: {}", sha256Claim);
                log.error("   Computed: {}", hashBody);
                return false;
            }
            
            log.debug("✅ Signature validation successful");
            return true;

        } catch (Exception e) {
            log.error("❌ Verify Exception: {}", e.getMessage());
            log.error("Exception Type: {}", e.getClass().getSimpleName());
            return false;
        }
    }

    private String sha256(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}
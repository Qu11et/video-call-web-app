package anhtaikhau.example.video_call_web_app.dto.webhook;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LiveKitWebhookEvent {

    private String event;
    private RoomInfo room;
    
    // ✅ Thêm field participant (số ít) cho các event như participant_left
    private ParticipantInfo participant;
    
    // ✅ Field participants (số nhiều) cho room_finished
    private List<ParticipantInfo> participants;
    
    // ✅ THÊM FIELD NÀY
    @JsonAlias("createdAt")
    private Long createdAt;
    
    private String id;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RoomInfo {
        private String sid;
        private String name;
        
        @JsonAlias("creation_time")
        private Long creationTime;
        
        @JsonAlias("end_time")
        private Long endTime;
        
        @JsonAlias("empty_timeout")
        private Integer emptyTimeout;
        
        @JsonAlias("num_participants")
        private Integer numParticipants;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ParticipantInfo {
        private String sid;
        private String identity;
        private String name;
        
        @JsonAlias("joined_at")
        private Long joinedAt;
        
        @JsonAlias("left_at")
        private Long leftAt;
        
        private String state;
    }
}
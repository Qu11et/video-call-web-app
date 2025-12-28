package anhtaikhau.example.video_call_web_app.event;

import anhtaikhau.example.video_call_web_app.dto.webhook.LiveKitWebhookEvent;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class MeetingEndedEvent extends ApplicationEvent {
    
    private final LiveKitWebhookEvent webhookData;

    public MeetingEndedEvent(Object source, LiveKitWebhookEvent webhookData) {
        super(source);
        this.webhookData = webhookData;
    }
}
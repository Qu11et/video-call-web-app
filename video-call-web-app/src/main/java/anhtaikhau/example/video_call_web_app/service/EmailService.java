package anhtaikhau.example.video_call_web_app.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Async // <--- QUAN TRỌNG: Chạy ở thread riêng, không làm chậm API đăng ký
    public void sendVerificationEmail(String toEmail, String token) {
        try {
            String verifyUrl = "https://dev.lkht.id.vn/verify-email?token=" + token;
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Xác thực tài khoản Video Call App");
            message.setText("Chào mừng bạn!\n\nVui lòng nhấn vào link sau để kích hoạt tài khoản:\n" + verifyUrl);
            
            mailSender.send(message);
            log.info("Đã gửi email xác thực tới {}", toEmail);
        } catch (Exception e) {
            log.error("Lỗi gửi email: {}", e.getMessage());
        }
    }
}
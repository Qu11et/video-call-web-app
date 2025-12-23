package anhtaikhau.example.video_call_web_app.service.impl;

import anhtaikhau.example.video_call_web_app.dto.request.UserCreationRequest;
import anhtaikhau.example.video_call_web_app.model.Role;
import anhtaikhau.example.video_call_web_app.model.User;
import anhtaikhau.example.video_call_web_app.repository.UserRepository;
import anhtaikhau.example.video_call_web_app.service.EmailService;
import anhtaikhau.example.video_call_web_app.service.EmailVerificationService;
import anhtaikhau.example.video_call_web_app.model.UserStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final EmailVerificationService verificationService;
    private final EmailService emailService;
    
    // Spring Event Publisher (Để push sự kiện profile nếu cần sau này)
    private final ApplicationEventPublisher eventPublisher;

    public User createUser(UserCreationRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        var user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .status(UserStatus.PENDING) // Mặc định là PENDING cho đến khi xác thực email
                .build();

        User savedUser = userRepository.save(user);

        // 1. Tạo Token lưu Redis
        String token = verificationService.generateVerificationToken(user.getEmail());
        
        // 2. Gửi Email (Async)
        emailService.sendVerificationEmail(user.getEmail(), token);
        
        log.info("User created: {}", savedUser.getId());
        
        // TODO: Push sự kiện Profile Created (Ví dụ gửi email welcome hoặc sync qua service khác)
        // eventPublisher.publishEvent(new UserCreatedEvent(this, savedUser));

        return savedUser;
    }

    public void verifyUser(String token) {
        // 1. Check Redis
        String email = verificationService.validateToken(token);
        
        // 2. Update DB
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
    }
}
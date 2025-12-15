package anhtaikhau.example.video_call_web_app.service.impl;

import anhtaikhau.example.video_call_web_app.dto.request.UserCreationRequest;
import anhtaikhau.example.video_call_web_app.model.Role;
import anhtaikhau.example.video_call_web_app.model.User;
import anhtaikhau.example.video_call_web_app.repository.UserRepository;
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
                .build();

        User savedUser = userRepository.save(user);
        
        log.info("User created: {}", savedUser.getId());
        
        // TODO: Push sự kiện Profile Created (Ví dụ gửi email welcome hoặc sync qua service khác)
        // eventPublisher.publishEvent(new UserCreatedEvent(this, savedUser));

        return savedUser;
    }
}
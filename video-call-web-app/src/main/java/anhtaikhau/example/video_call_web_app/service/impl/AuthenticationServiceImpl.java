package anhtaikhau.example.video_call_web_app.service.impl;

import anhtaikhau.example.video_call_web_app.dto.request.SignInRequest;
import anhtaikhau.example.video_call_web_app.dto.response.TokenExchangeResponse;
import anhtaikhau.example.video_call_web_app.model.User;
import anhtaikhau.example.video_call_web_app.repository.UserRepository;
import anhtaikhau.example.video_call_web_app.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public TokenExchangeResponse authenticate(SignInRequest request) {
        // 1. Xác thực qua Spring Security
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // 2. Lấy thông tin user
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 3. Build Token (Access + Refresh)
        var accessToken = jwtService.generateToken(user);
        // Lưu ý: Cần bổ sung hàm generateRefreshToken trong JwtService nếu muốn tách biệt
        var refreshToken = jwtService.generateToken(user); 

        return TokenExchangeResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400L)
                .build();
    }
}
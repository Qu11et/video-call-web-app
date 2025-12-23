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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public TokenExchangeResponse authenticate(SignInRequest request) {
        // 1. Kiểm tra User có tồn tại không trước
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Email này chưa được đăng ký!"));

        // 2. Kiểm tra trạng thái xác thực Email
        // Giả sử bạn đã thêm field status và enum UserStatus ở các bước trước
        if (!user.isEnabled()) {
            throw new DisabledException("Tài khoản chưa xác thực email. Vui lòng kiểm tra hộp thư của bạn.");
        }        

        // 3. Nếu qua được 2 bước trên, mới gọi AuthenticationManager để check password
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Mật khẩu không chính xác!");
        }

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
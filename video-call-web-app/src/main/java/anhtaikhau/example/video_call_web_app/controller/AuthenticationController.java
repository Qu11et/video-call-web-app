package anhtaikhau.example.video_call_web_app.controller;

import anhtaikhau.example.video_call_web_app.dto.request.SignInRequest;
import anhtaikhau.example.video_call_web_app.dto.request.UserCreationRequest;
import anhtaikhau.example.video_call_web_app.dto.response.TokenExchangeResponse;
import anhtaikhau.example.video_call_web_app.model.User;
import anhtaikhau.example.video_call_web_app.service.impl.AuthenticationServiceImpl;
import anhtaikhau.example.video_call_web_app.service.impl.UserServiceImpl;
import anhtaikhau.example.video_call_web_app.util.AuthCookieManager;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuthenticationController {

    private final UserServiceImpl userService;
    private final AuthenticationServiceImpl authService;
    private final AuthCookieManager cookieManager;

    // Endpoint Đăng ký: /api/v1/users/registration
    @PostMapping("/users/registration")
    public ResponseEntity<String> register(@RequestBody UserCreationRequest request) {
        userService.createUser(request);
        return ResponseEntity.ok("User registered successfully");
    }

    // Endpoint Đăng nhập: /api/v1/auth/sign-in
    @PostMapping("/auth/sign-in")
    public ResponseEntity<TokenExchangeResponse> signIn(
            @RequestBody SignInRequest request,
            HttpServletResponse response // Để set cookie
    ) {
        TokenExchangeResponse tokenResponse = authService.authenticate(request);

        // Set Access Token & Refresh Token vào Cookie
        cookieManager.addAccessTokenCookie(response, tokenResponse.getAccessToken());
        cookieManager.addRefreshTokenCookie(response, tokenResponse.getRefreshToken());

        return ResponseEntity.ok(tokenResponse);
    }

    // API MỚI: Kiểm tra Token và lấy thông tin User (Auto Login)
    // Endpoint: /api/v1/auth/verify
    @GetMapping("/auth/verify")
    public ResponseEntity<Map<String, Object>> verifyToken(@AuthenticationPrincipal User userDetails) {
        // @AuthenticationPrincipal tự động lấy user từ SecurityContext (do Filter đã nạp vào)
        // Nếu token không hợp lệ hoặc không có, Filter đã chặn trước khi đến đây -> userDetails sẽ không null
        if (userDetails == null) {
            return ResponseEntity.status(403).build();
        }
        
        return ResponseEntity.ok(Map.of(
            "email", userDetails.getEmail(),
            "fullName", userDetails.getFullName(),
            "role", userDetails.getRole()
        ));
    }
}
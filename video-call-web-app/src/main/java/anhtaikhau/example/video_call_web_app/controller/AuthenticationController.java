package anhtaikhau.example.video_call_web_app.controller;

import anhtaikhau.example.video_call_web_app.dto.request.SignInRequest;
import anhtaikhau.example.video_call_web_app.dto.request.UserCreationRequest;
import anhtaikhau.example.video_call_web_app.dto.response.TokenExchangeResponse;
import anhtaikhau.example.video_call_web_app.model.User;
import anhtaikhau.example.video_call_web_app.service.impl.AuthenticationServiceImpl;
import anhtaikhau.example.video_call_web_app.service.impl.UserServiceImpl;
import anhtaikhau.example.video_call_web_app.util.AuthCookieManager;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.Cookie;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
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

    // API Đăng xuất: POST /api/v1/auth/logout
    @PostMapping("/auth/logout")
    public ResponseEntity<String> logout(HttpServletResponse response, HttpServletRequest request) {
        // Log cookie trước khi xóa
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                log.info("Cookie before logout: {} = {}", c.getName(), c.getValue());
            }
        }
        
        cookieManager.clearCookies(response);
        
        log.info("Logout successful, cookies cleared");
        return ResponseEntity.ok("Đăng xuất thành công");
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

    @GetMapping("/users/verify")
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        userService.verifyUser(token);
        return ResponseEntity.ok("Xác thực email thành công! Bạn có thể đăng nhập ngay bây giờ.");
    }
}
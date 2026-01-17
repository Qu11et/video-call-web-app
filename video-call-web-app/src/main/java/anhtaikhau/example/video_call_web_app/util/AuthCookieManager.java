package anhtaikhau.example.video_call_web_app.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AuthCookieManager {

    @Value("${application.cookie.domain}")
    private String domain;

    @Value("${application.cookie.secure}")
    private boolean secure;

    @Value("${application.cookie.http-only}")
    private boolean httpOnly;

    @Value("${application.cookie.max-age}")
    private int maxAge;

    public void addAccessTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("access_token", token);
        cookie.setHttpOnly(httpOnly);
        cookie.setSecure(secure);
        cookie.setPath("/");
        cookie.setDomain(domain);
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    public void addRefreshTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("refresh_token", token);
        cookie.setHttpOnly(httpOnly);
        cookie.setSecure(secure);
        cookie.setPath("/api/v1/auth/refresh"); // Chỉ gửi lên endpoint refresh
        cookie.setDomain(domain);
        cookie.setMaxAge(maxAge * 7); // 7 ngày
        response.addCookie(cookie);
    }

    public void clearCookies(HttpServletResponse response) {
        // 1. Xóa Access Token với path="/"
        Cookie accessCookie1 = new Cookie("access_token", "");
        accessCookie1.setPath("/");
        accessCookie1.setDomain(domain);
        accessCookie1.setSecure(secure);
        accessCookie1.setHttpOnly(httpOnly); 
        accessCookie1.setMaxAge(0);
        response.addCookie(accessCookie1);
        
        // 2. Xóa Refresh Token với path="/api/v1/auth/refresh"
        Cookie refreshCookie = new Cookie("refresh_token", "");
        refreshCookie.setPath("/api/v1/auth/refresh");
        refreshCookie.setDomain(domain);
        refreshCookie.setSecure(secure);
        refreshCookie.setHttpOnly(httpOnly);
        refreshCookie.setMaxAge(0);
        response.addCookie(refreshCookie);
        
        // 3. Xóa Refresh Token với path="/" (phòng trường hợp cũ)
        Cookie refreshCookie2 = new Cookie("refresh_token", "");
        refreshCookie2.setPath("/");
        refreshCookie2.setDomain(domain);
        refreshCookie2.setSecure(secure);
        refreshCookie2.setHttpOnly(httpOnly);
        refreshCookie2.setMaxAge(0);
        response.addCookie(refreshCookie2);
    }
}
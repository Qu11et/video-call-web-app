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
        // Xóa Access Token - thử nhiều combination
        clearCookie(response, "access_token", "/", domain);
        clearCookie(response, "access_token", "/", null); // Không có domain
        
        // Xóa Refresh Token với nhiều path
        clearCookie(response, "refresh_token", "/api/v1/auth/refresh", domain);
        clearCookie(response, "refresh_token", "/api/v1/auth/refresh", null);
        clearCookie(response, "refresh_token", "/", domain);
        clearCookie(response, "refresh_token", "/", null);
    }
    
    private void clearCookie(HttpServletResponse response, String name, String path, String domain) {
        Cookie cookie = new Cookie(name, "");
        cookie.setPath(path);
        if (domain != null && !domain.isEmpty()) {
            cookie.setDomain(domain);
        }
        cookie.setMaxAge(0);
        cookie.setHttpOnly(httpOnly);
        // Không set secure để đảm bảo xóa được cả cookie HTTP và HTTPS
        response.addCookie(cookie);
    }
}
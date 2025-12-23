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
        // 1. Xóa Access Token
        Cookie cookie = new Cookie("access_token", null); // Value null cũng được
        cookie.setPath("/");
        cookie.setDomain(domain); // <--- BẮT BUỘC PHẢI CÓ VÀ GIỐNG LÚC TẠO
        cookie.setSecure(secure); // <--- NÊN CÓ CHO ĐỒNG BỘ
        cookie.setHttpOnly(httpOnly); 
        cookie.setMaxAge(0);      // <--- Đây là lệnh xóa
        response.addCookie(cookie);
        
        // 2. Xóa Refresh Token
        Cookie refreshCookie = new Cookie("refresh_token", null);
        refreshCookie.setPath("/api/v1/auth/refresh");
        refreshCookie.setDomain(domain); // <--- BẮT BUỘC PHẢI CÓ
        refreshCookie.setSecure(secure);
        refreshCookie.setHttpOnly(httpOnly);
        refreshCookie.setMaxAge(0);
        response.addCookie(refreshCookie);
    }
}
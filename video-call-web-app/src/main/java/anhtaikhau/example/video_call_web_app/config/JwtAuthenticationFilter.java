package anhtaikhau.example.video_call_web_app.config;

import anhtaikhau.example.video_call_web_app.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String requestPath = request.getServletPath();
        
        // ✅ LOG REQUEST PATH ĐỂ DEBUG
        log.info("🔍 Processing request: {} {}", request.getMethod(), requestPath);

        if (requestPath.startsWith("/api/webhook/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // ✅ BỎ QUA JWT FILTER CHO WEBSOCKET VÀ CÁC ENDPOINT PUBLIC
        if (requestPath.startsWith("/ws") ||
            requestPath.equals("/api/v1/auth/sign-in") || 
            requestPath.equals("/api/v1/users/registration") ||
            requestPath.equals("/api/v1/auth/resend-verification") ||
            requestPath.equals("/api/v1/auth/logout") ||
            requestPath.equals("/api/v1/users/verify") ||
            requestPath.startsWith("/api/rooms/")) {
            
            log.info("⏩ Skipping JWT filter for public endpoint: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }
        
        String jwt = null;
        String userEmail = null;

        // 3. Ưu tiên lấy từ Header Authorization
        final String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            log.info("✅ JWT found in Authorization header");
        }

        // 4. Nếu Header không có, tìm trong Cookie
        if (jwt == null && request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) {
                    jwt = cookie.getValue();
                    log.info("✅ JWT found in Cookie");
                    break;
                }
            }
        }

        // Nếu không tìm thấy token -> Cho qua (SecurityConfig sẽ chặn 403 sau đó)
        if (jwt == null) {
            log.warn("❌ Request to {} failed: No JWT found in Header or Cookie", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        // 5. Xác thực Token
        try {
            userEmail = jwtService.extractUsername(jwt);
            log.info("📧 Extracted email from JWT: {}", userEmail);
            
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                
                log.info("👤 User authorities: {}", userDetails.getAuthorities());
                
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.info("✅ JWT authentication successful for user: {}", userEmail);
                } else {
                    log.warn("❌ JWT token is invalid for user: {}", userEmail);
                }
            }
        } catch (Exception e) {
            log.error("❌ JWT Authentication failed for {}: {}", requestPath, e.getMessage());
        }
        
        filterChain.doFilter(request, response);
    }
}    
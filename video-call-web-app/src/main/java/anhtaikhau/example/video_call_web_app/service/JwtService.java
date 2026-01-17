package anhtaikhau.example.video_call_web_app.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

// ✅ THÊM import này
import jakarta.annotation.PostConstruct;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
@Slf4j
public class JwtService {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    // ✅ Log secret key khi bean được tạo
    @PostConstruct
    public void init() {
        log.info("🔑 JWT Secret Key loaded: {}...{}", 
            secretKey.substring(0, 10), 
            secretKey.substring(secretKey.length() - 10)
        );
        log.info("🔑 Secret Key length: {}", secretKey.length());
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        // ✅ SỬA: Tạo token trước, sau đó log
        String token = Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24)) // 24h
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();

        // ✅ Log sau khi tạo token
        log.info("✅ Token generated for user: {}", userDetails.getUsername());
        log.debug("Token: {}...{}", token.substring(0, 20), token.substring(token.length() - 20));
        
        return token;
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            boolean valid = (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
            
            // ✅ Log validation result
            log.info("🔍 Token validation for {}: {}", userDetails.getUsername(), valid);
            
            return valid;
        } catch (Exception e) {
            // ✅ Log lỗi khi validate
            log.error("❌ Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        Date expiration = extractExpiration(token);
        boolean expired = expiration.before(new Date());
        
        // ✅ Log expiration
        if (expired) {
            log.warn("⏰ Token expired at: {}", expiration);
        }
        
        return expired;
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSignInKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (io.jsonwebtoken.security.SignatureException e) {
            // ✅ Log signature mismatch
            log.error("❌ JWT Signature mismatch! Token was signed with different key!");
            throw e;
        } catch (Exception e) {
            log.error("❌ JWT parsing error: {}", e.getMessage());
            throw e;
        }
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
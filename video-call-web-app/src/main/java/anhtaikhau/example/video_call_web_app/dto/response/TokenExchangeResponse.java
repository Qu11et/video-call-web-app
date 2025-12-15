package anhtaikhau.example.video_call_web_app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TokenExchangeResponse {
    private String accessToken;
    private String refreshToken; // Bổ sung Refresh Token
    private String tokenType;    // Thường là "Bearer"
    private Long expiresIn;      // Thời gian hết hạn (giây)
}
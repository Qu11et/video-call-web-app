package anhtaikhau.example.video_call_web_app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
    public class WebConfig implements WebMvcConfigurer {
        @Override
        public void addCorsMappings(CorsRegistry registry) {
            registry.addMapping("/**")
                // Dùng allowedOriginPatterns để linh hoạt hơn allowedOrigins
                .allowedOriginPatterns("https://*.id.vn", "http://localhost:*", "*") 
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true); // Nên để true để hỗ trợ Cookie/Auth sau này
        }
    }
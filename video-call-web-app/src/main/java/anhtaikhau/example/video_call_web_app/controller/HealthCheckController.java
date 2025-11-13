package anhtaikhau.example.video_call_web_app.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/health") 
public class HealthCheckController {

    @GetMapping
    public Map<String, String> checkHealth() {
        return Map.of("status", "Server is running!");
    }
}

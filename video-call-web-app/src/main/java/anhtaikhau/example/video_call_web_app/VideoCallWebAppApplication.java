package anhtaikhau.example.video_call_web_app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class VideoCallWebAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(VideoCallWebAppApplication.class, args);
	}

}

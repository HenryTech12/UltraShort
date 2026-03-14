package org.app.UltraShort;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@SpringBootApplication
@EnableCaching
@EnableScheduling
public class UltraShortApplication {

	public static void main(String[] args) {
		SpringApplication.run(UltraShortApplication.class, args);
	}

	@Scheduled(fixedRate = 60000) // Run every 60 seconds
	public void isServerRunning() {
		System.out.println("Server is running...");
	}
}

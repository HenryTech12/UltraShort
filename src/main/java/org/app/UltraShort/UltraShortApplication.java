package org.app.UltraShort;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class UltraShortApplication {

	public static void main(String[] args) {
		SpringApplication.run(UltraShortApplication.class, args);
	}

}

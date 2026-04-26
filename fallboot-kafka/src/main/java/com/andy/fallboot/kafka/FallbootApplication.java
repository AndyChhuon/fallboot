package com.andy.fallboot.kafka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EntityScan(basePackages = "com.andy.fallboot.shared")
@EnableScheduling
public class FallbootApplication {

	public static void main(String[] args) {
		SpringApplication.run(FallbootApplication.class, args);
	}

}

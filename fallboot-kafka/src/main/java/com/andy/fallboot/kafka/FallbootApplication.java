package com.andy.fallboot.kafka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

@SpringBootApplication
@EntityScan(basePackages = "com.andy.fallboot.shared")
public class FallbootApplication {

	public static void main(String[] args) {
		SpringApplication.run(FallbootApplication.class, args);
	}

}

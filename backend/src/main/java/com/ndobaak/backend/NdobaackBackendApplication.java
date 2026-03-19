package com.ndobaak.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class NdobaackBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(NdobaackBackendApplication.class, args);
	}

}

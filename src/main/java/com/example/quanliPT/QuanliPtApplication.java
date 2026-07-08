package com.example.quanliPT;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class QuanliPtApplication {

	public static void main(String[] args) {
		SpringApplication.run(QuanliPtApplication.class, args);
	}

}

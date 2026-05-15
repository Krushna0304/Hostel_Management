package com.krunity.HostelManagment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HostelManagmentApplication {

	public static void main(String[] args) {
		SpringApplication.run(HostelManagmentApplication.class, args);
	}

}

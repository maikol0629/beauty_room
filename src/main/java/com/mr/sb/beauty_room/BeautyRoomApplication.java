package com.mr.sb.beauty_room;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class BeautyRoomApplication {

	public static void main(String[] args) {
		SpringApplication.run(BeautyRoomApplication.class, args);
	}

}

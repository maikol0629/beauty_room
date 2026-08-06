package com.mr.sb.beauty_room;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@EnableScheduling
@SpringBootApplication
public class BeautyRoomApplication {

	public static void main(String[] args) {
		configureTempDirectory(Path.of(System.getProperty("user.dir"), "target", "tomcat"));
		SpringApplication.run(BeautyRoomApplication.class, args);
	}

	static void configureTempDirectory(Path directory) {
		try {
			Files.createDirectories(directory);
			System.setProperty("java.io.tmpdir", directory.toString());
		} catch (IOException ex) {
			throw new IllegalStateException("Could not initialize temporary directory for the application", ex);
		}
	}

}

package com.mr.sb.beauty_room;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class BeautyRoomApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void configureTempDirectoryCreatesExpectedWorkspacePath(@TempDir Path tempDir) throws IOException {
		Path runtimeDir = tempDir.resolve("runtime-tmp");

		BeautyRoomApplication.configureTempDirectory(runtimeDir);

		assertEquals(runtimeDir.toString(), System.getProperty("java.io.tmpdir"));
		assertTrue(Files.isDirectory(runtimeDir));
	}

}

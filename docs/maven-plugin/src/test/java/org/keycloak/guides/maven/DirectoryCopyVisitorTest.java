package org.keycloak.guides.maven;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DirectoryCopyVisitorTest {

	@TempDir
	Path temp;

	private Path srcDir;
	private Path targetDir;

	@BeforeEach
	void setUpDirectories() throws IOException {
		srcDir = temp.resolve("source");
		targetDir = temp.resolve("target");
		Files.createDirectories(srcDir);
		Files.createDirectories(targetDir);
	}

	@Test
	void copyDirectoriesMultipleLevels() throws IOException {
		Path level1 = srcDir.resolve("level1");
		Path level2a = level1.resolve("level2a");
		Path level2b = level1.resolve("level2b");
		Path level3 = level2a.resolve("level3");
		Files.createDirectories(level3);
		Files.createDirectories(level2b);
		Files.createFile(srcDir.resolve("rootfile"));
		Files.createFile(level1.resolve("l1file"));
		Files.createFile(level2b.resolve("l2filea"));
		Files.createFile(level2b.resolve("l2fileb"));

		Files.walkFileTree(srcDir, new DirectoryCopyVisitor(targetDir));

		assertEquals(List.of("level1", "rootfile"), listDirContent(targetDir));
		assertEquals(List.of("l1file", "level2a", "level2b"), listDirContent(targetDir.resolve("level1")));
		assertEquals(List.of("level3"), listDirContent(targetDir.resolve("level1").resolve("level2a")));
		assertEquals(List.of(), listDirContent(targetDir.resolve("level1").resolve("level2a").resolve("level3")));
		assertEquals(List.of("l2filea", "l2fileb"), listDirContent(targetDir.resolve("level1").resolve("level2b")));
	}

	private List<String> listDirContent(Path path) throws IOException {
		return Files.list(path).map(Path::getFileName).map(Path::toString).sorted().toList();
	}
}

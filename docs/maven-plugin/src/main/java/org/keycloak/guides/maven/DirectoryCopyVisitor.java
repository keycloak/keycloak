package org.keycloak.guides.maven;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

public class DirectoryCopyVisitor extends SimpleFileVisitor<Path> {

	private Path targetDir;
	private Path sourceDir;

	public DirectoryCopyVisitor(Path targetDir) {
		this.targetDir = targetDir;
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		if (sourceDir == null) {
			sourceDir = dir;
		} else {
			Path relativePath = sourceDir.relativize(dir);
			Files.createDirectories(targetDir.resolve(relativePath));
		}
		
		return	FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		Path relativePath = sourceDir.relativize(file);
		Files.copy(file, targetDir.resolve(relativePath), StandardCopyOption.REPLACE_EXISTING);
		return FileVisitResult.CONTINUE;
	}

}

package org.keycloak.db.compatibility.verifier;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

abstract class AbstractMojoTest {
    protected Path testDir;
    protected File supportedFile;
    protected File unsupportedFile;

    @BeforeEach
    void init() throws IOException {
        testDir = Files.createTempDirectory(ChangeSetSupportedMojoTest.class.getSimpleName());
        supportedFile = testDir.resolve("supported.json").toFile();
        unsupportedFile = testDir.resolve("unsupported.json").toFile();
    }

    @AfterEach
    void cleanup() throws IOException {
        if (Files.exists(testDir)) {
            Files.walkFileTree(testDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
}

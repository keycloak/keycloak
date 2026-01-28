package org.keycloak.db.compatibility.verifier;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CreateSnapshotMojoTest {

    private Path testDir;
    private File supportedFile;
    private File unsupportedFile;

    @BeforeEach
    void init() throws IOException {
        testDir = Files.createTempDirectory(CreateSnapshotMojoTest.class.getSimpleName());
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

    @Test
    void testSnapshotFilesCreated() throws Exception {
        var classLoader = CreateSnapshotMojoTest.class.getClassLoader();
        var mojo = new CreateSnapshotMojo();
        mojo.createSnapshot(classLoader, supportedFile, unsupportedFile);

        assertTrue(supportedFile.exists());
        assertTrue(unsupportedFile.exists());

        var mapper = new ObjectMapper();
        List<ChangeSet> supportedChanges = mapper.readValue(supportedFile, new TypeReference<>() {});
        assertEquals(2, supportedChanges.size());

        List<ChangeSet> unsupportedChanges = mapper.readValue(unsupportedFile, new TypeReference<>() {});
        assertEquals(0, unsupportedChanges.size());
    }
}

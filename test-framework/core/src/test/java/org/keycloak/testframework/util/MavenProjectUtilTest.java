package org.keycloak.testframework.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class MavenProjectUtilTest {

    private static final String SERVICE_ENTRY = "META-INF/services/org.keycloak.storage.UserStorageProviderFactory";

    @Test
    public void buildJarNormalizesResourceEntryPathSeparators(@TempDir Path tempDir) throws IOException {
        Path classesPath = Files.createDirectory(tempDir.resolve("classes"));

        Path serviceFile = classesPath.resolve(SERVICE_ENTRY);
        Files.createDirectories(serviceFile.getParent());
        Files.writeString(serviceFile, "com.example.TestUserStorageProviderFactory");

        Path jarPath = tempDir.resolve("provider.jar");
        MavenProjectUtil.buildJar("provider.jar", classesPath, jarPath);

        try (ZipFile zipFile = new ZipFile(jarPath.toFile())) {
            Assertions.assertNotNull(zipFile.getEntry(SERVICE_ENTRY));
        }
    }
}

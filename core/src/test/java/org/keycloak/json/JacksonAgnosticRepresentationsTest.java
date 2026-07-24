package org.keycloak.json;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Ensures that representation classes in {@code org.keycloak.representations} do not use
 * Jackson 2 specific types ({@code com.fasterxml.jackson.databind} or {@code com.fasterxml.jackson.core}).
 * <p>
 * Only shared annotations from {@code com.fasterxml.jackson.annotation} are allowed.
 * Custom serializer classes that must extend Jackson base classes are allowlisted.
 */
public class JacksonAgnosticRepresentationsTest {

    private static final Set<String> FORBIDDEN_PACKAGES = new HashSet<>(Arrays.asList(
            "com.fasterxml.jackson.databind",
            "com.fasterxml.jackson.core"
    ));

    private static final Set<String> ALLOWLISTED_FILES = new HashSet<>(Arrays.asList(
            "MultivaluedHashMapValueSerializer.java",
            "MultivaluedHashMapValueDeserializer.java",
            "AuthorizationSchema.java"
    ));

    @Test
    public void representationsMustNotUseJacksonDatabindOrCore() throws IOException {
        Path representationsDir = findRepresentationsDir();
        List<String> violations = new ArrayList<>();

        Files.walkFileTree(representationsDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!file.toString().endsWith(".java")) {
                    return FileVisitResult.CONTINUE;
                }
                String fileName = file.getFileName().toString();
                if (ALLOWLISTED_FILES.contains(fileName)) {
                    return FileVisitResult.CONTINUE;
                }

                List<String> lines = Files.readAllLines(file);
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    if (!line.startsWith("import ")) {
                        continue;
                    }
                    for (String forbidden : FORBIDDEN_PACKAGES) {
                        if (line.contains(forbidden)) {
                            String relative = representationsDir.getParent().relativize(file).toString();
                            violations.add(relative + ":" + (i + 1) + " — " + line.trim());
                        }
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });

        assertTrue(
                "Representation classes must not import Jackson 2 specific types "
                        + "(com.fasterxml.jackson.databind or com.fasterxml.jackson.core). "
                        + "Use org.keycloak.json abstractions instead (e.g., @StringOrArray, RawJsonValue). "
                        + "Violations:\n" + String.join("\n", violations),
                violations.isEmpty()
        );
    }

    private static Path findRepresentationsDir() {
        Path dir = Paths.get("core/src/main/java/org/keycloak/representations");
        if (Files.isDirectory(dir)) {
            return dir;
        }
        dir = Paths.get("src/main/java/org/keycloak/representations");
        if (Files.isDirectory(dir)) {
            return dir;
        }
        throw new IllegalStateException("Cannot find representations directory");
    }
}

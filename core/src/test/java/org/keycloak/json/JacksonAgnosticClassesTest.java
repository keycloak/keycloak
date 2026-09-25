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
import java.util.stream.Collectors;

import org.keycloak.util.JsonSerialization;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Ensures that representation classes in {@code org.keycloak.representations} and classes used by the representation
 * do not use Jackson 2 specific types ({@code com.fasterxml.jackson.databind} or {@code com.fasterxml.jackson.core})
 * or {@link JsonSerialization}.
 * <p>
 * Only shared annotations from {@code com.fasterxml.jackson.annotation} are allowed.
 * Custom serializer classes that must extend Jackson base classes are allowlisted.
 */
public class JacksonAgnosticClassesTest {

    private static final Set<String> FORBIDDEN_IMPORTS = new HashSet<>(Arrays.asList(
            "com.fasterxml.jackson.databind",
            "com.fasterxml.jackson.core",
            JsonSerialization.class.getName()
    ));

    private static final Set<String> ALLOWLISTED_FILES = new HashSet<>(Arrays.asList(
            "MultivaluedHashMapValueSerializer.java",
            "MultivaluedHashMapValueDeserializer.java"
    ));

    @Test
    public void representationsMustNotUseJackson2() throws IOException {
        Path representationsDir = findPackageDir("representations");
        List<String> violations = new ArrayList<>();

        collectClassesUsingJackson2(representationsDir, violations);

        assertTrue(
                "Representation classes must not use Jackson 2 specific types "
                        + "(" + String.join(" or ", FORBIDDEN_IMPORTS) + "). "
                        + "Use org.keycloak.json abstractions instead (e.g., @StringOrArray, RawJsonValue). "
                        + "Violations:\n" + String.join("\n", violations),
                violations.isEmpty()
        );
    }

    @Test
    public void tokenUtilMustNotUseJackson2() throws IOException {
        Path utilDir = findPackageDir("util");
        List<String> violations = new ArrayList<>();

        collectClassesUsingJackson2(utilDir, violations, "JsonSerialization");

        List<String> tokenUtilViolations = violations.stream().filter(v -> v.contains("util/TokenUtil.java")).collect(Collectors.toList());

        assertTrue(
                "TokenUtil class must not import Jackson 2 specific types "
                        + "(" + String.join(" or ", FORBIDDEN_IMPORTS) + "). "
                        + "Use org.keycloak.json abstractions instead (e.g., @StringOrArray, RawJsonValue). "
                        + "Violations:\n" + String.join("\n", tokenUtilViolations),
                tokenUtilViolations.isEmpty()
        );
    }

    private static void collectClassesUsingJackson2(Path classesDir, List<String> violations,
                                                    String... forbiddenClassesInSamePackage) throws IOException {
        Files.walkFileTree(classesDir, new SimpleFileVisitor<Path>() {
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
                        for (String forbiddenClass : forbiddenClassesInSamePackage) {
                            if (line.contains(forbiddenClass)) {
                                addViolation(file, i, line, classesDir, violations);
                            }
                        }
                    } else {
                        for (String forbidden : FORBIDDEN_IMPORTS) {
                            if (line.contains(forbidden)) {
                                addViolation(file, i, line, classesDir, violations);
                            }
                        }
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void addViolation(Path file, int i, String line, Path representationsDir, List<String> violations) {
        String relative = representationsDir.getParent().relativize(file).toString();
        violations.add(relative + ":" + (i + 1) + " — " + line.trim());
    }

    private static Path findPackageDir(String subPackage) {
        Path dir = Paths.get("core/src/main/java/org/keycloak/" + subPackage);
        if (Files.isDirectory(dir)) {
            return dir;
        }
        dir = Paths.get("src/main/java/org/keycloak/" + subPackage);
        if (Files.isDirectory(dir)) {
            return dir;
        }
        throw new IllegalStateException("Cannot find " + subPackage + " directory");
    }
}

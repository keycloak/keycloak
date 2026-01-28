package org.keycloak.test.migration;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import static java.lang.Thread.sleep;

public class MigrateTest {

    private static final String DIFF_COMMAND = System.getenv("DIFFTOOL");

    final List<Class<? extends TestRewrite>> MIGRATORS = List.of(
            AddKeycloakIntegrationTestRewrite.class,
            ChangePackageRewrite.class,
            RenameImportsRewrite.class,
            UpdateAssertsRewrite.class,
            AddManagedResourcesRewrite.class,
            AdminEventAssertRewrite.class,
            BeforeRewrite.class,
            CommonStatementsRewrite.class);

    Path rootPath = getRootPath();
    Path oldTestsuitePath = rootPath.resolve("testsuite/integration-arquillian/tests/base/src/test/java/org/keycloak/testsuite").toAbsolutePath();
    Path newBaseTestPath = rootPath.resolve("tests/base/src/test/java/org/keycloak/tests").toAbsolutePath();

    public static void main(String[] args) throws Exception {
        MigrateTest migrateTest = new MigrateTest();
        String testPath = args[0];
        migrateTest.migrate(testPath);
    }

    public void migrate(String test) throws Exception {
        if (!test.endsWith(".java")) {
            test += ".java";
        }
        Path testPath = Path.of(test).normalize().toAbsolutePath();
        if (!Files.isRegularFile(testPath)) {
            testPath = getOldTestsuitePath(testPath);
        }

        Path destinationPath = getDestination(testPath);

        System.out.println("Root:           " + rootPath);
        System.out.println("Old tests:      " + oldTestsuitePath);
        System.out.println("Migrated tests: " + newBaseTestPath);
        System.out.println();
        System.out.println("Migrating test: " + testPath);
        System.out.println("To:             " + destinationPath);
        System.out.println();

        validatePaths(testPath, destinationPath);

        List<String> content = readFileToList(testPath);

        for (Class<? extends TestRewrite> clazz : MIGRATORS) {
            TestRewrite m = clazz.getConstructor().newInstance();
            m.setContent(content);
            m.rewrite();
        }

        writeFile(content, destinationPath);

        if (DIFF_COMMAND != null && !DIFF_COMMAND.isEmpty()) {
            List<String> args = new ArrayList<>(List.of(DIFF_COMMAND.split(" ")));
            args.add(testPath.toString());
            args.add(destinationPath.toString());

            ProcessBuilder pb = new ProcessBuilder();
            pb.command(args);
            Process diffProcess = pb.start();
            BufferedReader diffOutput = new BufferedReader(new InputStreamReader(diffProcess.getInputStream()));
            String line = diffOutput.readLine();
            while (line != null) {
                System.out.println(line);
                line = diffOutput.readLine();
            }
            diffOutput.close();
        }
    }

    private Path getOldTestsuitePath(Path testPath) throws IOException {
        try (Stream<Path> paths = Files.walk(oldTestsuitePath)) {
            List<Path> foundPath = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equals(testPath.getFileName().toString()))
                    .toList();

            if (foundPath.isEmpty()) {
                throw new RuntimeException("Test file not found");
            }
            if (foundPath.size() != 1) {
                throw new RuntimeException("Multiple test files found: " + foundPath);
            }
            return foundPath.get(0).toAbsolutePath();
        }
    }

    private void validatePaths(Path testPath, Path destinationPath) throws IOException {
        if (!testPath.startsWith(oldTestsuitePath.toString())) {
            throw new RuntimeException("Can only migrate tests from " + oldTestsuitePath);
        }

        if (!Files.isRegularFile(testPath)) {
            throw new RuntimeException("Test file not found");
        }

        if (!destinationPath.startsWith(newBaseTestPath.toString())) {
            throw new RuntimeException("Can only migrate tests to " + newBaseTestPath);
        }

        Path destinationDir = destinationPath.resolve("..");
        if (!Files.exists(destinationDir)) {
            Files.createDirectories(destinationDir);
            System.out.println("Creating a new directory: " + destinationDir);
        }
    }

    private Path getDestination(Path testPath) {
        return Path.of(testPath.toString().replace(oldTestsuitePath.toString(), newBaseTestPath.toString()));
    }

    private List<String> readFileToList(Path path) throws IOException {
        List<String> content = new LinkedList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path.toFile()))) {
            for (String l = br.readLine(); l != null; l = br.readLine()) {
                content.add(l);
            }
        }
        return content;
    }

    private void writeFile(List<String> content, Path destinationPath) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(destinationPath.toFile()))) {
            for (String l : content) {
                bw.write(l);
                bw.newLine();
            }
        }
    }

    private static Path getRootPath() {
        Path path = Path.of(System.getProperty("user.dir")).normalize().toAbsolutePath();
        while (Files.isDirectory(path.getParent())) {
            if (!Files.isRegularFile(path.getParent().resolve("pom.xml"))) {
                return path;
            }
            path = path.getParent();
        }
        return null;
    }

}

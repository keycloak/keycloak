package org.keycloak.test.migration;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

public class MigrateTest {

    final List<Class<? extends TestRewrite>> MIGRATORS = List.of(
            AddKeycloakIntegrationTestRewrite.class,
            ChangePackageRewrite.class,
            RenameImportsRewrite.class,
            UpdateAssertsRewrite.class,
            AddManagedResourcesRewrite.class,
            AdminEventAssertRewrite.class);

    Path rootPath = getRootPath();
    Path oldTestsuitePath = rootPath.resolve("testsuite/integration-arquillian/tests/base/src/test/java/org/keycloak/testsuite").toAbsolutePath();
    Path newBaseTestPath = rootPath.resolve("tests/base/src/test/java/org/keycloak/tests").toAbsolutePath();

    public static void main(String[] args) throws Exception {
        MigrateTest migrateTest = new MigrateTest();
        String testPath = args[0];
        migrateTest.migrate(testPath);
    }

    public void migrate(String test) throws Exception {
        Path testPath = Path.of(test).normalize().toAbsolutePath();

        if (!Files.isRegularFile(testPath)) {
            testPath = oldTestsuitePath.resolve(test);
        }

        Path destinationPath = getDestination(testPath);

        System.out.println("Root:           " + rootPath);
        System.out.println("Old tests:      " + oldTestsuitePath);
        System.out.println("Migrated tests: " + newBaseTestPath);
        System.out.println();
        System.out.println("Migrating test: " + testPath);
        System.out.println("To:             " + destinationPath);
        System.out.println();

        if (!testPath.startsWith(oldTestsuitePath.toString())) {
            throw new RuntimeException("Can only migrate tests from " + oldTestsuitePath);
        }

        List<String> content = readFileToList(testPath);

        for (Class<? extends TestRewrite> clazz : MIGRATORS) {
            TestRewrite m = clazz.getConstructor().newInstance();
            m.setContent(content);
            m.rewrite();
        }

        writeFile(content, destinationPath);

//        ProcessBuilder pb = new ProcessBuilder();
//        pb.command("meld", testPath.toString(), destinationPath.toString());
//        pb.start();
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Created by st on 06.02.17.
 */
public class CopyDependencies {

    public static void main(String[] args) throws IOException {
        String version = args[2];
        Path targetRoot = new File(args[1]).toPath().resolve(version);
        Path projectDir = targetRoot.getParent().getParent().getParent().getParent();
        Path mavenRepository = new File(args[0]).toPath().resolve("org").resolve("keycloak");

        CopyDependencies dependencies = new CopyDependencies(version, projectDir, targetRoot, mavenRepository);
        dependencies.copyFiles();
    }

    private final String version;
    private final Path targetDir;
    private final Path projectDir;
    private final Path mavenRepository;

    public CopyDependencies(String version, Path projectDir, Path targetDir, Path mavenRepository) {
        this.version = version;
        this.targetDir = targetDir;
        this.projectDir = projectDir;
        this.mavenRepository = mavenRepository;
    }

    public void copyFiles() throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(CopyDependencies.class.getResourceAsStream("files")));
        targetDir.toFile().mkdirs();

        for (String l = br.readLine(); l != null; l = br.readLine()) {
            if (l.trim().length() > 0) {
                l = replaceVariables(l);

                String[] t = l.trim().split(":");

                String type = t[0];
                String artifactName = t[1];
                String destinationName = t.length == 2 ? artifactName : t[2];

                switch (type) {
                    case "mvn":
                        copyMaven(artifactName, destinationName);
                        break;
                    case "npm":
                        copyNpm(artifactName, destinationName);
                        break;
                }
            }
        }

        br.close();
    }

    private void copyMaven(String artifactName, String destinationName) throws IOException {
        File artifactDir = mavenRepository.resolve(artifactName).resolve(version).toFile();
        if (!artifactDir.isDirectory()) {
            throw new RuntimeException(artifactName + " (" + artifactDir + ") not found");
        }

        File[] files = artifactDir.listFiles((file, name) -> name.contains(".tar.gz") || name.contains(".tgz") || name.contains(".zip"));

        for (File f : files) {
            Files.copy(f.toPath(), targetDir.resolve(f.getName().replace(artifactName, destinationName)), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void copyNpm(String artifactName, String destinationName) throws IOException {
        Path artifactPath = projectDir.resolve(artifactName);
        if (!artifactPath.toFile().isFile()) {
            throw new RuntimeException(artifactName + " (" + artifactPath + ") not found");
        }

        Files.copy(projectDir.resolve(artifactName), targetDir.resolve(destinationName));
    }

    private String replaceVariables(String input) {
        return input.replaceAll("\\$\\$VERSION\\$\\$", version);
    }

}

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

        Path repository = new File(args[0]).toPath().resolve("org").resolve("keycloak");
        Path targetRoot = new File(args[1]).toPath().resolve(version);

        BufferedReader br = new BufferedReader(new InputStreamReader(CopyDependencies.class.getResourceAsStream("files")));

        Path target = targetRoot;
        for (String l = br.readLine(); l != null; l = br.readLine()) {

            if (l.startsWith("./")) {
                target = targetRoot.resolve(l.replace("./", "").replace('/', File.separatorChar));
                if (!target.toFile().isDirectory()) {
                    target.toFile().mkdirs();
                }
            } else if (l.trim().length() > 0) {
                String[] t = l.trim().split(":");

                String artifactName = t[0];
                String destName = t.length == 1 ? artifactName : t[1];

                File artifactDir = repository.resolve(artifactName).resolve(version).toFile();

                for (File f : artifactDir.listFiles((file, name) -> name.contains(".tar.gz") || name.contains(".zip"))) {
                    Files.copy(f.toPath(), target.resolve(f.getName().replace(artifactName, destName)), StandardCopyOption.REPLACE_EXISTING);
                }

                System.out.println(artifactName);
            }
        }

        br.close();
    }

}

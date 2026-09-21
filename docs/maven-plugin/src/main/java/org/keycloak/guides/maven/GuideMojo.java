package org.keycloak.guides.maven;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "keycloak-guide", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class GuideMojo extends AbstractMojo {

    @Parameter(property = "project.build.sourceDirectory")
    private String sourceDir;

    @Parameter(property = "project.build.directory")
    private String targetDir;

    @Parameter(defaultValue = "${maven.multiModuleProjectDirectory}/rest/admin-v2/services/target/admin-v2-doc.json")
    private String docFile;

    @Parameter(defaultValue = "${maven.multiModuleProjectDirectory}/integration/client-cli/admin-cli/target/admin-v2-cli-examples.json")
    private String cliExamplesFile;

    @Parameter(defaultValue = "${maven.multiModuleProjectDirectory}/js/libs/keycloak-admin-client/src/generated/doc-examples/admin-v2-js-examples.json")
    private String jsExamplesFile;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoFailureException {
        try {
            Log log = getLog();
            Path src = Paths.get(sourceDir);
            for (Path srcDir : getSourceDirs(src)) {
                String dirName = srcDir.getFileName().toString();
                Path targetRoot = Paths.get(targetDir);
                Path targetDir = targetRoot.resolve("generated-guides").resolve(dirName);
                Files.createDirectories(targetDir);

                if (dirName.equals("images")) {
                    log.info("Copy files from " + srcDir + " to " + targetRoot);
                    Files.walkFileTree(srcDir, new DirectoryCopyVisitor(targetRoot));
                } else {
                    log.info("Guide dir: " + srcDir);
                    log.info("Target dir: " + targetDir);

                    Path docPath = docFile != null ? Paths.get(docFile) : null;
                    Path cliExamplesPath = cliExamplesFile != null ? Paths.get(cliExamplesFile) : null;
                    Path jsExamplesPath = jsExamplesFile != null ? Paths.get(jsExamplesFile) : null;
                    GuideBuilder g = new GuideBuilder(srcDir, targetDir, log, project.getProperties(),
                            docPath, cliExamplesPath, jsExamplesPath);
                    g.build();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new MojoFailureException("Failed to generated asciidoc files", e);
        }
    }

    public static List<Path> getSourceDirs(Path src) throws IOException {
        try (Stream<Path> fileList = Files.list(src)) {
            return fileList
                  .filter(Files::isDirectory)
                  .filter(p ->
                        switch (p.getFileName().toString()) {
                            case "src", "target", "templates" -> false;
                            default -> true;
                        })
                  .toList();
        }
    }
}

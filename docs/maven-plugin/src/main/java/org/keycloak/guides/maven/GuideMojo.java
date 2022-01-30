package org.keycloak.guides.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

@Mojo(name = "keycloak-guide", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GuideMojo extends AbstractMojo {

    @Parameter(property = "project.build.sourceDirectory")
    private String sourceDir;

    @Parameter(property = "project.build.directory")
    private String targetDir;

    @Override
    public void execute() throws MojoFailureException {
        try {
            Log log = getLog();
            File srcDir = new File(sourceDir).getParentFile();
            File targetDir = new File(this.targetDir, "generated-guides");
            if (!targetDir.isDirectory()) {
                targetDir.mkdirs();
            }

            log.info("Guide dir: " + srcDir.getAbsolutePath());
            log.info("Target dir: " + targetDir.getAbsolutePath());

            GuideBuilder g = new GuideBuilder(srcDir, targetDir, log);
            g.server();
        } catch (Exception e) {
            e.printStackTrace();
            throw new MojoFailureException("Failed to generated asciidoc files", e);
        }
    }

}

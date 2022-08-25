package org.keycloak.guides.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;

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
            File topDir = new File(sourceDir).getParentFile();

            for (File srcDir: topDir.listFiles(d -> d.isDirectory() && !d.getName().equals("templates"))) {
                File targetDir = new File(new File(this.targetDir, "generated-guides"), srcDir.getName());
                if (!targetDir.isDirectory()) {
                    targetDir.mkdirs();
                }

                if (srcDir.getName().equals("images")) {
                    FileUtils.copyDirectory(srcDir, targetDir);
                } else {
                    log.info("Guide dir: " + srcDir.getAbsolutePath());
                    log.info("Target dir: " + targetDir.getAbsolutePath());

                    GuideBuilder g = new GuideBuilder(srcDir, targetDir, log);
                    g.build();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new MojoFailureException("Failed to generated asciidoc files", e);
        }
    }

}

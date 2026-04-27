package org.keycloak.db.compatibility.verifier;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "supported")
public class SupportedMojo extends AbstractNewEntryMojo {

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Skipping execution");
            return;
        }

        File root = project.getBasedir();
        File sFile = new File(root, supportedFile);
        File uFile = new File(root, unsupportedFile);
        checkFileExist("supported", sFile);
        checkFileExist("unsupported", uFile);

        try {
            execute(sFile, uFile);
        } catch (Exception e) {
            throw new MojoExecutionException("Error adding entry to supported file", e);
        }
    }
}

package org.keycloak.db.compatibility.verifier;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "unsupported")
public class ChangeSetUnsupportedMojo extends AbstractChangeSetMojo {

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
            if (addAll) {
                addAll(classLoader(), uFile, sFile);
            } else {
                checkValidChangeSetId(id, author, filename);
                ChangeSet changeSet = new ChangeSet(id, author, filename);
                addIndividual(classLoader(), changeSet, uFile, sFile);
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Error adding ChangeSet to unsupported file", e);
        }
    }
}

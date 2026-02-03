package org.keycloak.db.compatibility.verifier;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "snapshot")
public class CreateSnapshotMojo extends AbstractMojo {

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Skipping execution");
            return;
        }

        try {
            File root = project.getBasedir();
            File sFile = new File(root, supportedFile);
            File uFile = new File(root, unsupportedFile);

            ClassLoader classLoader = classLoader();
            createSnapshot(classLoader, sFile, uFile);
        } catch (Exception e) {
            throw new MojoExecutionException("Error creating ChangeSet snapshot", e);
        }
    }

    void createSnapshot(ClassLoader classLoader, File sFile, File uFile) throws IOException {
        // Write all known ChangeSet defined in the jpa-changelog*.xml files to the supported file
        ChangeLogXMLParser xmlParser = new ChangeLogXMLParser(classLoader);
        Set<ChangeSet> changeSets = xmlParser.discoverAllChangeSets();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.writeValue(sFile, changeSets);

        // Create an empty JSON array in the unsupported file
        objectMapper.writeValue(uFile, Set.of());
    }
}

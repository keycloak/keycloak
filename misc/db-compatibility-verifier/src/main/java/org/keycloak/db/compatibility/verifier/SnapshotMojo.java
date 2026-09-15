package org.keycloak.db.compatibility.verifier;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "snapshot")
public class SnapshotMojo extends AbstractMojo {

    @Parameter(property = "db.verify.migration.package")
    String migrationsPackage;

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
            createSnapshot(classLoader, sFile, uFile, migrationsPackage);
        } catch (Exception e) {
            throw new MojoExecutionException("Error creating ChangeSet snapshot", e);
        }
    }

    void createSnapshot(ClassLoader classLoader, File sFile, File uFile, String migrationsPackage) throws IOException {
        // Record all known ChangeSet defined in the jpa-changelog*.xml files
        ChangeLogXMLParser xmlParser = new ChangeLogXMLParser(classLoader);
        Set<ChangeSet> changeSets = xmlParser.discoverAllChangeSets();

        // Record all known org.keycloak.migration.migrators.Migration implementations
        Set<Migration> migrations = new KeycloakMigrationParser(classLoader, migrationsPackage).discoverAllMigrations();

        // Write all to the supported file
        JsonParent jsonFile = new JsonParent(changeSets, migrations);
        objectMapper.writeValue(sFile, jsonFile);

        // Create an empty JSON array in the unsupported file
        objectMapper.writeValue(uFile, new JsonParent(Set.of(), Set.of()));
    }
}

package org.keycloak.db.compatibility.verifier;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "verify")
public class VerifyMojo extends AbstractMojo {

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
            verify(classLoader(), sFile, uFile);
        } catch (Exception e) {
            throw new MojoExecutionException("Error loading project resources", e);
        }
    }

    void verify(ClassLoader classLoader, File sFile, File uFile) throws IOException, MojoExecutionException {
        if (!sFile.exists() && !uFile.exists()) {
            getLog().info("No JSON files exist to verify");
            return;
        }

        verifyChangeSets(classLoader, sFile, uFile);
        verifyMigrations(classLoader, sFile, uFile);
    }

    void verifyChangeSets(ClassLoader classLoader, File sFile, File uFile) throws IOException, MojoExecutionException {
        // Parse JSON files to determine all committed ChangeSets
        Collection<ChangeSet> sChanges = objectMapper.readValue(sFile, new TypeReference<JsonParent>() {}).changeSets();
        Collection<ChangeSet> uChanges = objectMapper.readValue(uFile, new TypeReference<JsonParent>() {}).changeSets();
        Set<ChangeSet> recordedChanges = Stream.of(sChanges, uChanges)
              .flatMap(Collection::stream)
              .collect(Collectors.toSet());

        var description = "ChangeSet";
        if (recordedChanges.isEmpty()) {
            getLog().info("No supported or unsupported ChangeSets exist in specified files");
            return;
        }

        verifyIntersection(description, sChanges, uChanges);

        // Parse all ChangeSets currently defined in the jpa-changelog* files
        ChangeLogXMLParser xmlParser = new ChangeLogXMLParser(classLoader);
        Set<ChangeSet> currentChanges = xmlParser.discoverAllChangeSets();
        verifyMissing(description, currentChanges, recordedChanges, sFile, uFile);
    }

    void verifyMigrations(ClassLoader classLoader, File sFile, File uFile) throws IOException, MojoExecutionException {
        if (migrationsPackage != null && migrationsPackage.isEmpty()) {
            getLog().info("Skipping Migrations verification as no package configured");
            return;
        }
        // Parse JSON files to determine all committed Migrations
        Collection<Migration> sChanges = objectMapper.readValue(sFile, new TypeReference<JsonParent>() {}).migrations();
        Collection<Migration> uChanges = objectMapper.readValue(uFile, new TypeReference<JsonParent>() {}).migrations();
        Set<Migration> recordedChanges = Stream.of(sChanges, uChanges)
              .flatMap(Collection::stream)
              .collect(Collectors.toSet());

        var description = "Migration";
        if (recordedChanges.isEmpty()) {
            getLog().info("No supported or unsupported Migrations exist in specified files");
            return;
        }
        verifyIntersection(description, sChanges, uChanges);

        // Parse all Migrations currently defined in the configured migrationsPackage
        Set<Migration> currentChanges = new KeycloakMigrationParser(classLoader, migrationsPackage).discoverAllMigrations();
        verifyMissing(description, currentChanges, recordedChanges, sFile, uFile);
    }

    void verifyIntersection(String description, Collection<?> sChanges, Collection<?> uChanges) throws MojoExecutionException {
        Set<?> intersection = new HashSet<>(sChanges);
        intersection.retainAll(uChanges);
        if (!intersection.isEmpty()) {
            getLog().error("The following %s should be defined in either the supported or unsupported file, they cannot appear in both:".formatted(description));
            intersection.forEach(change -> getLog().error("\t\t" + change.toString()));
            getLog().error("The offending %s should be removed from one of the files".formatted(description));
            throw new MojoExecutionException("One or more %s definitions exist in both the supported and unsupported file".formatted(description));
        }
    }

    void verifyMissing(String description, Set<?> currentChanges, Set<?> recordedChanges, File sFile, File uFile) throws MojoExecutionException {
        if (recordedChanges.equals(currentChanges)) {
            getLog().info("All %s in the module recorded as expected in the supported and unsupported files".formatted(description));
        } else {
            getLog().error("The recorded %s differ from the current repository state".formatted(description));
            getLog().error("The following %s should be defined in either the supported '%s' or unsupported '%s' file:".formatted(description, sFile.toString(), uFile.toString()));
            currentChanges.removeAll(recordedChanges);
            currentChanges.forEach(change -> getLog().error("\t\t" + change.toString()));
            getLog().error("You must determine whether the %s is compatible with rolling upgrades or not".formatted(description));
            getLog().error("A %s that requires locking preventing other cluster members accessing the database or makes schema changes that breaks functionality in earlier Keycloak versions is NOT compatible with rolling upgrades".formatted(description));
            getLog().error("Rolling upgrade compatibility must be verified against all supported database vendors before the supported file is updated");
            getLog().error("If the schema change IS compatible, then it should be committed to the repository in the supported file: '%s'".formatted(sFile.toString()));
            getLog().error("If the schema change IS NOT compatible, then it should be committed to the repository in the unsupported file: '%s'".formatted(sFile.toString()));
            getLog().error("Adding a %s to the unsupported file ensures that a rolling upgrade is not attempted when upgrading to the first patch version containing the change".formatted(description));
            getLog().error("%s can be added to the supported or unsupported files using the org.keycloak:db-compatibility-verifier-maven-plugin. See the module README for usage instructions".formatted(description));
            throw new MojoExecutionException("One or more %s definitions are missing from the supported or unsupported files".formatted(description));
        }
    }
}

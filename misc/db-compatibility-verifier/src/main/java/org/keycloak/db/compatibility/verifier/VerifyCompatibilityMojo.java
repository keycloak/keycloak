package org.keycloak.db.compatibility.verifier;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "verify")
public class VerifyCompatibilityMojo extends AbstractMojo {

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
            verifyCompatibility(classLoader(), sFile, uFile);
        } catch (Exception e) {
            throw new MojoExecutionException("Error loading project resources", e);
        }
    }

    void verifyCompatibility(ClassLoader classLoader, File sFile, File uFile) throws IOException, MojoExecutionException {
        if (!sFile.exists() && !uFile.exists()) {
            getLog().info("No JSON ChangeSet files exist to verify");
            return;
        }

        // Parse JSON files to determine all committed ChangeSets
        List<ChangeSet> sChanges = objectMapper.readValue(sFile, new TypeReference<>() {});
        List<ChangeSet> uChanges = objectMapper.readValue(uFile, new TypeReference<>() {});
        Set<ChangeSet> recordedChanges = Stream.of(sChanges, uChanges)
              .flatMap(List::stream)
              .collect(Collectors.toSet());

        if (recordedChanges.isEmpty()) {
            getLog().info("No supported or unsupported ChangeSet exist in specified files");
            return;
        }

        checkIntersection(sChanges, uChanges);

        // Parse all ChangeSets currently defined in the jpa-changegetLog() files
        ChangeLogXMLParser xmlParser = new ChangeLogXMLParser(classLoader);
        Set<ChangeSet> currentChanges = xmlParser.discoverAllChangeSets();
        checkMissingChangeSet(currentChanges, recordedChanges, sFile, uFile);
    }

    void checkIntersection(List<ChangeSet> sChanges, List<ChangeSet> uChanges) throws MojoExecutionException {
        Set<ChangeSet> intersection = new HashSet<>(sChanges);
        intersection.retainAll(uChanges);
        if (!intersection.isEmpty()) {
            getLog().error("The following ChangeSets should be defined in either the supported or unsupported file, they cannot appear in both:");
            intersection.forEach(change -> getLog().error("\t\t" + change.toString()));
            getLog().error("The offending ChangeSets should be removed from one of the files");
            throw new MojoExecutionException("One or more ChangeSet definitions exist in both the supported and unsupported file");
        }
    }

    void checkMissingChangeSet(Set<ChangeSet> currentChanges, Set<ChangeSet> recordedChanges, File sFile, File uFile) throws MojoExecutionException {
        if (recordedChanges.equals(currentChanges)) {
            getLog().info("All ChangeSets in the module recorded as expected in the supported and unsupported files");
        } else {
            getLog().error("The recorded ChangeSet JSON files differ from the current repository state");
            getLog().error("The following ChangeSets should be defined in either the supported '%s' or unsupported '%s' file:".formatted(sFile.toString(), uFile.toString()));
            currentChanges.removeAll(recordedChanges);
            currentChanges.forEach(change -> getLog().error("\t\t" + change.toString()));
            getLog().error("You must determine whether the ChangeSet(s) is compatible with rolling upgrades or not");
            getLog().error("A ChangeSet that requires locking preventing other cluster members accessing the database or makes schema changes that breaks functionality in earlier Keycloak versions is NOT compatible with rolling upgrades");
            getLog().error("Rolling upgrade compatibility must be verified against all supported database vendors before the supported file is updated");
            getLog().error("If the change IS compatible, then it should be committed to the repository in the supported file: '%s'".formatted(sFile.toString()));
            getLog().error("If the change IS NOT compatible, then it should be committed to the repository in the unsupported file: '%s'".formatted(sFile.toString()));
            getLog().error("Adding a ChangeSet to the unsupported file ensures that a rolling upgrade is not attempted when upgrading to the first patch version containing the change");
            getLog().error("ChangeSets can be added to the supported or unsupported files using the org.keycloak:db-compatibility-verifier-maven-plugin. See the module README for usage instructions");
            throw new MojoExecutionException("One or more ChangeSet definitions are missing from the supported or unsupported files");
        }
    }
}

package org.keycloak.db.compatibility.verifier;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

abstract class AbstractChangeSetMojo extends AbstractMojo {
    @Parameter(property = "db.verify.changeset.all", defaultValue = "false")
    boolean addAll;

    @Parameter(property = "db.verify.changeset.id")
    String id;

    @Parameter(property = "db.verify.changeset.author")
    String author;

    @Parameter(property = "db.verify.changeset.filename")
    String filename;


    void checkFileExist(String ref, File file) throws MojoExecutionException {
        if (!file.exists()) {
            throw new MojoExecutionException("%s file does not exist".formatted(ref));
        }
    }

    void checkUnknownChangeSet(Set<ChangeSet> knownChangeSets, ChangeSet changeSet) throws MojoExecutionException {
        if (!knownChangeSets.contains(changeSet)) {
            throw new MojoExecutionException("Unknown ChangeSet: " + changeSet);
        }
    }

    protected void checkValidChangeSetId(String id, String author, String filename) throws MojoExecutionException {
        if (id == null || id.isBlank()) {
            throw new MojoExecutionException("ChangeSet id not set");
        }
        if (author == null || author.isBlank()) {
            throw new MojoExecutionException("ChangeSet author not set");
        }
        if (filename == null || filename.isBlank()) {
            throw new MojoExecutionException("ChangeSet filename not set");
        }
    }

    void addAll(ClassLoader classLoader, File dest, File exclusions) throws IOException {
        // Discover all known ChangeSets
        ChangeLogXMLParser xmlParser = new ChangeLogXMLParser(classLoader);
        Set<ChangeSet> knownChangeSets = xmlParser.discoverAllChangeSets();

        // Load changes to exclude and remove them from the known changesets
        Set<ChangeSet> excludedChanges = objectMapper.readValue(exclusions, new TypeReference<>() {});
        knownChangeSets.removeAll(excludedChanges);

        // Overwrite all content in the destination file
        objectMapper.writeValue(dest, knownChangeSets);
    }

    void addIndividual(ClassLoader classLoader, ChangeSet changeSet, File dest, File alternate) throws IOException, MojoExecutionException {
        // Discover all known ChangeSets
        ChangeLogXMLParser xmlParser = new ChangeLogXMLParser(classLoader);
        Set<ChangeSet> knownChangeSets = xmlParser.discoverAllChangeSets();

        // It should not be possible to add an unknown changeset
        checkUnknownChangeSet(knownChangeSets, changeSet);

        Set<ChangeSet> alternateChangeSets = objectMapper.readValue(alternate, new TypeReference<>() {});
        if (alternateChangeSets.contains(changeSet)) {
            throw new MojoExecutionException("ChangeSet already defined in the %s file".formatted(alternate.getName()));
        }

        List<ChangeSet> destChanges = objectMapper.readValue(dest, new TypeReference<>() {});
        if (!destChanges.contains(changeSet)) {
            // If the ChangeSet is not already known, append to the end of the JSON array and overwrite the existing file
            destChanges.add(changeSet);
            objectMapper.writeValue(dest, destChanges);
        }
    }
}

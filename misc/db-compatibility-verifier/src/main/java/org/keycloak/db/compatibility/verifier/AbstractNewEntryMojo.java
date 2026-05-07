package org.keycloak.db.compatibility.verifier;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

abstract class AbstractNewEntryMojo extends AbstractMojo {
    @Parameter(property = "db.verify.changeset.all", defaultValue = "false")
    boolean addAll;

    @Parameter(property = "db.verify.changeset.id")
    String id;

    @Parameter(property = "db.verify.changeset.author")
    String author;

    @Parameter(property = "db.verify.changeset.filename")
    String filename;

    @Parameter(property = "db.verify.migration.class")
    String migration;

    protected void execute(File dest, File alternate) throws Exception {
        ClassLoader classLoader = classLoader();
        if (addAll) {
            addAllChangeSets(classLoader, dest, alternate);
        } else if (migration != null && !migration.isEmpty()) {
            addMigration(classLoader, new Migration(migration), dest, alternate);
        } else {
            checkValidChangeSetId(id, author, filename);
            ChangeSet changeSet = new ChangeSet(id, author, filename);
            addChangeSet(classLoader, changeSet, dest, alternate);
        }
    }

    protected void checkFileExist(String ref, File file) throws MojoExecutionException {
        if (!file.exists()) {
            throw new MojoExecutionException("%s file does not exist".formatted(ref));
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

    void addAllChangeSets(ClassLoader classLoader, File dest, File exclusions) throws IOException {
        // Discover all known ChangeSets
        ChangeLogXMLParser xmlParser = new ChangeLogXMLParser(classLoader);
        Set<ChangeSet> knownChangeSets = xmlParser.discoverAllChangeSets();

        // Load changes to exclude and remove them from the known changesets
        JsonParent excludedParent = objectMapper.readValue(exclusions, new TypeReference<>() {});
        Collection<ChangeSet> excludedChanges = excludedParent.changeSets();
        knownChangeSets.removeAll(excludedChanges);

        // Overwrite all ChangeSet content in the destination file
        JsonParent parent = objectMapper.readValue(dest, new TypeReference<>() {});
        objectMapper.writeValue(dest, new JsonParent(knownChangeSets, parent.migrations()));
    }

    void addChangeSet(ClassLoader classLoader, ChangeSet changeSet, File dest, File alternate) throws IOException, MojoExecutionException {
        // Discover all known ChangeSets
        ChangeLogXMLParser xmlParser = new ChangeLogXMLParser(classLoader);
        Set<ChangeSet> knownChangeSets = xmlParser.discoverAllChangeSets();

        // It should not be possible to add an unknown changeset
        if (!knownChangeSets.contains(changeSet)) {
            throw new MojoExecutionException("Unknown ChangeSet: " + changeSet);
        }

        JsonParent parent = objectMapper.readValue(alternate, new TypeReference<>() {});
        Set<ChangeSet> alternateChangeSets = new HashSet<>(parent.changeSets());
        if (alternateChangeSets.contains(changeSet)) {
            throw new MojoExecutionException("ChangeSet already defined in the %s file".formatted(alternate.getName()));
        }

        parent = objectMapper.readValue(dest, new TypeReference<>() {});
        Collection<ChangeSet> destChanges = parent.changeSets();
        if (!destChanges.contains(changeSet)) {
            // If the ChangeSet is not already known, append to the end of the JSON array and overwrite the existing file
            destChanges.add(changeSet);
            objectMapper.writeValue(dest, parent);
        }
    }

    void addMigration(ClassLoader classLoader, Migration migration, File dest, File alternate) throws IOException, MojoExecutionException {
        // Discover all known migrations
        String clazz = migration.clazz();
        int idx = clazz.lastIndexOf(".");
        String pkg = idx == -1 ? "" : clazz.substring(0, idx);

        KeycloakMigrationParser migrationParser = new KeycloakMigrationParser(classLoader, pkg);
        Set<Migration> knownMigrations = migrationParser.discoverAllMigrations();

        // It should not be possible to add an unknown Migration class
        if (!knownMigrations.contains(migration)) {
            throw new MojoExecutionException("Unknown Migration: " + migration);
        }

        JsonParent parent = objectMapper.readValue(alternate, new TypeReference<>() {});
        Set<Migration> alternateMigrations = new HashSet<>(parent.migrations());
        if (alternateMigrations.contains(migration)) {
            throw new MojoExecutionException("Migration already defined in the %s file".formatted(alternate.getName()));
        }

        parent = objectMapper.readValue(dest, new TypeReference<>() {});
        Collection<Migration> destChanges = parent.migrations();
        if (!destChanges.contains(migration)) {
            // If the Migration is not already known, append to the end of the JSON array and overwrite the existing file
            destChanges.add(migration);
            objectMapper.writeValue(dest, parent);
        }
    }
}

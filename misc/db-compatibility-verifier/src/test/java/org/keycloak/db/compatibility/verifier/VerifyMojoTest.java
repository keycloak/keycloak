package org.keycloak.db.compatibility.verifier;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class VerifyMojoTest {

    final ClassLoader classLoader = VerifyMojoTest.class.getClassLoader();
    final VerifyMojo mojo = new VerifyMojo();

    @Test
    void testFilesDoNotExist() {
        File noneExistingFile = new File("noneExistingFile");
        assertFalse(noneExistingFile.exists());

        assertDoesNotThrow(() -> mojo.verify(classLoader, noneExistingFile, noneExistingFile));
    }

    @Test
    void testEmptySnapshotFiles() {
        File emptyJson = new File(classLoader.getResource("META-INF/empty-snapshot.json").getFile());

        assertDoesNotThrow(() -> mojo.verifyChangeSets(classLoader, emptyJson, emptyJson));
    }

    @Test
    void testChangeSetIncludedInSupportedAndUnsupportedFiles() {
        var changeSet = new ChangeSet("1", "keycloak", "example.xml");

        Exception e = assertThrows(
              MojoExecutionException.class,
              () -> mojo.verifyIntersection("ChangeSet", List.of(changeSet), List.of(changeSet))
        );
        assertEquals("One or more ChangeSet definitions exist in both the supported and unsupported file", e.getMessage());
    }

    @Test
    void testAllChangeSetsRecorded() {
        var changeSets = Set.of(
              new ChangeSet("1", "keycloak", "example.xml"),
              new ChangeSet("2", "keycloak", "example.xml")
        );

        assertDoesNotThrow(() -> mojo.verifyMissing("ChangeSet", changeSets, new HashSet<>(changeSets), new File(""), new File("")));
    }

    @Test
    void testMissingChangeSet() {
        var currentChanges = new HashSet<ChangeSet>();
        currentChanges.add(new ChangeSet("1", "keycloak", "example.xml"));
        currentChanges.add(new ChangeSet("2", "keycloak", "example.xml"));

        var recordedChanges = Set.of(currentChanges.iterator().next());

        Exception e = assertThrows(
              MojoExecutionException.class,
              () -> mojo.verifyMissing("ChangeSet", currentChanges, recordedChanges, new File(""), new File(""))
        );
        assertEquals("One or more ChangeSet definitions are missing from the supported or unsupported files", e.getMessage());
    }

    @Test
    void testMigrationIncludedInSupportedAndUnsupportedFiles() {
        var migration = new Migration("example.Migration");

        Exception e = assertThrows(
              MojoExecutionException.class,
              () -> mojo.verifyIntersection("Migration", List.of(migration), List.of(migration))
        );
        assertEquals("One or more Migration definitions exist in both the supported and unsupported file", e.getMessage());
    }

    @Test
    void testAllMigrationsRecorded() {
        var migrations = Set.of(
              new Migration("example.Migration1"),
              new Migration("example.Migration2")
        );

        assertDoesNotThrow(() -> mojo.verifyMissing("Migration", migrations, new HashSet<>(migrations), new File(""), new File("")));
    }

    @Test
    void testMissingMigration() {
        var currentChanges = new HashSet<Migration>();
        currentChanges.add(new Migration("example.Migration1"));
        currentChanges.add(new Migration("example.Migration2"));

        var recordedChanges = Set.of(currentChanges.iterator().next());

        Exception e = assertThrows(
              MojoExecutionException.class,
              () -> mojo.verifyMissing("Migration", currentChanges, recordedChanges, new File(""), new File(""))
        );
        assertEquals("One or more Migration definitions are missing from the supported or unsupported files", e.getMessage());
    }
}

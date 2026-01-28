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

public class VerifyCompatibilityMojoTest {

    final ClassLoader classLoader = VerifyCompatibilityMojoTest.class.getClassLoader();

    @Test
    void testChangeSetFilesDoNotExist() {
        var mojo = new VerifyCompatibilityMojo();
        File noneExistingFile = new File("noneExistingFile");
        assertFalse(noneExistingFile.exists());

        assertDoesNotThrow(() -> mojo.verifyCompatibility(classLoader, noneExistingFile, noneExistingFile));
    }

    @Test
    void testEmptyChangeSetFiles() {
        var mojo = new VerifyCompatibilityMojo();
        File emptyJson = new File(classLoader.getResource("META-INF/empty-array.json").getFile());

        assertDoesNotThrow(() -> mojo.verifyCompatibility(classLoader, emptyJson, emptyJson));
    }

    @Test
    void testChangeSetIncludedInSupportedAndUnsupportedFiles() {
        var mojo = new VerifyCompatibilityMojo();
        var changeSet = new ChangeSet("1", "keycloak", "example.xml");

        Exception e = assertThrows(
              MojoExecutionException.class,
              () -> mojo.checkIntersection(List.of(changeSet), List.of(changeSet))
        );
        assertEquals("One or more ChangeSet definitions exist in both the supported and unsupported file", e.getMessage());
    }

    @Test
    void testAllChangeSetsRecorded() {
        var mojo = new VerifyCompatibilityMojo();
        var changeSets = Set.of(
              new ChangeSet("1", "keycloak", "example.xml"),
              new ChangeSet("2", "keycloak", "example.xml")
        );

        assertDoesNotThrow(() -> mojo.checkMissingChangeSet(changeSets, new HashSet<>(changeSets), new File(""), new File("")));
    }

    @Test
    void testMissingChangeSet() {
        var mojo = new VerifyCompatibilityMojo();
        var currentChanges = new HashSet<ChangeSet>();
        currentChanges.add(new ChangeSet("1", "keycloak", "example.xml"));
        currentChanges.add(new ChangeSet("2", "keycloak", "example.xml"));

        var recordedChanges = Set.of(currentChanges.iterator().next());

        Exception e = assertThrows(
              MojoExecutionException.class,
              () -> mojo.checkMissingChangeSet(currentChanges, recordedChanges, new File(""), new File(""))
        );
        assertEquals("One or more ChangeSet definitions are missing from the supported or unsupported files", e.getMessage());
    }
}

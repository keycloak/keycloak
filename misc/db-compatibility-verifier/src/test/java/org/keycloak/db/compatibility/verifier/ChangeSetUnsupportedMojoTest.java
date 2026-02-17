package org.keycloak.db.compatibility.verifier;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ChangeSetUnsupportedMojoTest extends AbstractMojoTest {

    @Test
    void testAddAll() throws Exception {
        var classLoader = ChangeSetUnsupportedMojoTest.class.getClassLoader();
        var mojo = new ChangeSetUnsupportedMojo();
        var mapper = new ObjectMapper();

        // Create supported file with a single ChangeSet
        List<ChangeSet> supportedChanges = new ChangeLogXMLParser(classLoader).extractChangeSets("META-INF/jpa-changelog-2.xml");
        assertEquals(1, supportedChanges.size());
        mapper.writeValue(unsupportedFile, supportedChanges);

        // Execute add all and expect all ChangeSets from jpa-changelog-1.xml to be present
        assertTrue(supportedFile.createNewFile());
        mojo.addAll(classLoader, supportedFile, unsupportedFile);

        List<ChangeSet> unsupportedChanges = mapper.readValue(supportedFile, new TypeReference<>() {});
        assertEquals(1, unsupportedChanges.size());

        ChangeSet sChange = unsupportedChanges.get(0);
        assertEquals("test", sChange.id());
        assertEquals("keycloak", sChange.author());
        assertEquals("META-INF/jpa-changelog-1.xml", sChange.filename());
    }

    @Test
    void testAddIndividual() throws Exception {
        var classLoader = ChangeSetUnsupportedMojoTest.class.getClassLoader();
        var changeLogParser = new ChangeLogXMLParser(classLoader);
        var mojo = new ChangeSetUnsupportedMojo();
        var mapper = new ObjectMapper();

        assertTrue(supportedFile.createNewFile());
        assertTrue(unsupportedFile.createNewFile());
        mapper.writeValue(supportedFile, List.of());
        mapper.writeValue(unsupportedFile, List.of());

        // Test ChangeSet is added to unsupported file as expected
        ChangeSet changeSet = changeLogParser.extractChangeSets("META-INF/jpa-changelog-1.xml").get(0);
        mojo.addIndividual(classLoader, changeSet, unsupportedFile, supportedFile);

        List<ChangeSet> unsupportedChanges = mapper.readValue(unsupportedFile, new TypeReference<>() {});
        assertEquals(1, unsupportedChanges.size());
        ChangeSet sChange = unsupportedChanges.get(0);
        assertEquals(changeSet.id(), sChange.id());
        assertEquals(changeSet.author(), sChange.author());
        assertEquals(changeSet.filename(), sChange.filename());

        // Test subsequent ChangeSets are added to already populated supported file
        changeSet = changeLogParser.extractChangeSets("META-INF/jpa-changelog-2.xml").get(0);
        mojo.addIndividual(classLoader, changeSet, unsupportedFile, supportedFile);

        unsupportedChanges = mapper.readValue(unsupportedFile, new TypeReference<>() {});
        assertEquals(2, unsupportedChanges.size());

        sChange = unsupportedChanges.get(1);
        assertEquals(changeSet.id(), sChange.id());
        assertEquals(changeSet.author(), sChange.author());
        assertEquals(changeSet.filename(), sChange.filename());

        // Test ChangeSet already exists handled gracefully
        mojo.addIndividual(classLoader, changeSet, unsupportedFile, supportedFile);

        unsupportedChanges = mapper.readValue(unsupportedFile, new TypeReference<>() {});
        assertEquals(2, unsupportedChanges.size());
    }

    @Test
    void testChangeAlreadySupported() throws Exception {
        var classLoader = ChangeSetUnsupportedMojoTest.class.getClassLoader();
        var mojo = new ChangeSetUnsupportedMojo();
        var mapper = new ObjectMapper();

        assertTrue(supportedFile.createNewFile());
        assertTrue(unsupportedFile.createNewFile());
        mapper.writeValue(unsupportedFile, List.of());

        // Create supported file with a single ChangeSet
        List<ChangeSet> unsupportedChanges = new ChangeLogXMLParser(classLoader).extractChangeSets("META-INF/jpa-changelog-1.xml");
        assertEquals(1, unsupportedChanges.size());

        ChangeSet changeSet = unsupportedChanges.get(0);
        mapper.writeValue(supportedFile, unsupportedChanges);

        Exception e = assertThrows(
              MojoExecutionException.class,
              () -> mojo.addIndividual(classLoader, changeSet, unsupportedFile, supportedFile)
        );

        assertEquals("ChangeSet already defined in the %s file".formatted(supportedFile.getName()), e.getMessage());
    }

    @Test
    void testAddUnknownChangeSet() throws Exception {
        var classLoader = ChangeSetSupportedMojoTest.class.getClassLoader();
        var mojo = new ChangeSetSupportedMojo();
        var mapper = new ObjectMapper();

        assertTrue(supportedFile.createNewFile());
        assertTrue(unsupportedFile.createNewFile());
        mapper.writeValue(unsupportedFile, List.of());
        ChangeSet unknown = new ChangeSet("asf", "asfgasg", "afasgfas");

        Exception e = assertThrows(
              MojoExecutionException.class,
              () -> mojo.addIndividual(classLoader, unknown, unsupportedFile, supportedFile)
        );

        assertEquals("Unknown ChangeSet: " + unknown, e.getMessage());
    }
}

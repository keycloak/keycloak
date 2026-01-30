package org.keycloak.db.compatibility.verifier;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ChangeSetSupportedMojoTest extends AbstractMojoTest {

    @Test
    void testAddAll() throws Exception {
        var classLoader = ChangeSetSupportedMojoTest.class.getClassLoader();
        var mojo = new ChangeSetSupportedMojo();
        var mapper = new ObjectMapper();

        // Create unsupported file with a single ChangeSet
        List<ChangeSet> unsupportedChanges = new ChangeLogXMLParser(classLoader).extractChangeSets("META-INF/jpa-changelog-2.xml");
        assertEquals(1, unsupportedChanges.size());
        mapper.writeValue(unsupportedFile, unsupportedChanges);

        // Execute add all and expect all ChangeSets from jpa-changelog-1.xml to be present
        assertTrue(supportedFile.createNewFile());
        mojo.addAll(classLoader, supportedFile, unsupportedFile);

        List<ChangeSet> supportedChanges = mapper.readValue(supportedFile, new TypeReference<>() {});
        assertEquals(1, supportedChanges.size());

        ChangeSet sChange = supportedChanges.get(0);
        assertEquals("test", sChange.id());
        assertEquals("keycloak", sChange.author());
        assertEquals("META-INF/jpa-changelog-1.xml", sChange.filename());
    }

    @Test
    void testAddIndividual() throws Exception {
        var classLoader = ChangeSetSupportedMojoTest.class.getClassLoader();
        var changeLogParser = new ChangeLogXMLParser(classLoader);
        var mojo = new ChangeSetSupportedMojo();
        var mapper = new ObjectMapper();

        assertTrue(supportedFile.createNewFile());
        assertTrue(unsupportedFile.createNewFile());
        mapper.writeValue(supportedFile, List.of());
        mapper.writeValue(unsupportedFile, List.of());

        // Test ChangeSet is added to supported file as expected
        ChangeSet changeSet = changeLogParser.extractChangeSets("META-INF/jpa-changelog-1.xml").get(0);
        mojo.addIndividual(classLoader, changeSet, supportedFile, unsupportedFile);

        List<ChangeSet> supportedChanges = mapper.readValue(supportedFile, new TypeReference<>() {});
        assertEquals(1, supportedChanges.size());
        ChangeSet sChange = supportedChanges.get(0);
        assertEquals(changeSet.id(), sChange.id());
        assertEquals(changeSet.author(), sChange.author());
        assertEquals(changeSet.filename(), sChange.filename());

        // Test subsequent ChangeSets are added to already populated supported file
        changeSet = changeLogParser.extractChangeSets("META-INF/jpa-changelog-2.xml").get(0);
        mojo.addIndividual(classLoader, changeSet, supportedFile, unsupportedFile);

        supportedChanges = mapper.readValue(supportedFile, new TypeReference<>() {});
        assertEquals(2, supportedChanges.size());

        sChange = supportedChanges.get(1);
        assertEquals(changeSet.id(), sChange.id());
        assertEquals(changeSet.author(), sChange.author());
        assertEquals(changeSet.filename(), sChange.filename());

        // Test ChangeSet already exists handled gracefully
        mojo.addIndividual(classLoader, changeSet, supportedFile, unsupportedFile);

        supportedChanges = mapper.readValue(supportedFile, new TypeReference<>() {});
        assertEquals(2, supportedChanges.size());
    }

    @Test
    void testChangeAlreadyUnsupported() throws Exception {
        var classLoader = ChangeSetSupportedMojoTest.class.getClassLoader();
        var mojo = new ChangeSetSupportedMojo();
        var mapper = new ObjectMapper();

        assertTrue(supportedFile.createNewFile());
        assertTrue(unsupportedFile.createNewFile());
        mapper.writeValue(supportedFile, List.of());

        // Create unsupported file with a single ChangeSet
        List<ChangeSet> unsupportedChanges = new ChangeLogXMLParser(classLoader).extractChangeSets("META-INF/jpa-changelog-1.xml");
        assertEquals(1, unsupportedChanges.size());

        ChangeSet changeSet = unsupportedChanges.get(0);
        mapper.writeValue(unsupportedFile, unsupportedChanges);

        Exception e = assertThrows(
              MojoExecutionException.class,
              () -> mojo.addIndividual(classLoader, changeSet, supportedFile, unsupportedFile)
        );

        assertEquals("ChangeSet already defined in the %s file".formatted(unsupportedFile.getName()), e.getMessage());
    }

    @Test
    void testAddUnknownChangeSet() throws Exception {
        var classLoader = ChangeSetSupportedMojoTest.class.getClassLoader();
        var mojo = new ChangeSetSupportedMojo();
        var mapper = new ObjectMapper();

        assertTrue(supportedFile.createNewFile());
        assertTrue(unsupportedFile.createNewFile());
        mapper.writeValue(supportedFile, List.of());
        ChangeSet unknown = new ChangeSet("asf", "asfgasg", "afasgfas");

        Exception e = assertThrows(
              MojoExecutionException.class,
              () -> mojo.addIndividual(classLoader, unknown, supportedFile, unsupportedFile)
        );

        assertEquals("Unknown ChangeSet: " + unknown, e.getMessage());
    }
}

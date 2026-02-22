package org.keycloak.db.compatibility.verifier;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

abstract class AbstractNewEntryMojoTest extends AbstractMojoTest {

    protected AbstractNewEntryMojo mojo;
    protected ClassLoader classLoader;
    protected ObjectMapper mapper;

    protected AbstractNewEntryMojoTest(AbstractNewEntryMojo mojo) {
        this.mojo = mojo;
        this.classLoader = getClass().getClassLoader();
        this.mapper = new ObjectMapper();
    }

    protected abstract File getTargetFile();

    protected abstract File getAlternateFile();

    @Test
    void testAddAllChangeSets() throws Exception {
        // Create alternate file with a single ChangeSet
        List<ChangeSet> alternateChanges = new ChangeLogXMLParser(classLoader).extractChangeSets("META-INF/jpa-changelog-2.xml");
        assertEquals(1, alternateChanges.size());
        mapper.writeValue(getAlternateFile(), new JsonParent(alternateChanges, List.of()));

        // Execute add all and expect all ChangeSets from jpa-changelog-1.xml to be present
        mapper.writeValue(getTargetFile(), new JsonParent(List.of(), List.of()));
        mojo.addAllChangeSets(classLoader, getTargetFile(), getAlternateFile());

        JsonParent parent = mapper.readValue(getTargetFile(), new TypeReference<>() {});
        List<ChangeSet> targetChanges = new ArrayList<>(parent.changeSets());
        assertEquals(1, targetChanges.size());

        ChangeSet sChange = targetChanges.get(0);
        assertEquals("test", sChange.id());
        assertEquals("keycloak", sChange.author());
        assertEquals("META-INF/jpa-changelog-1.xml", sChange.filename());
    }

    @Test
    void testAddChangeSet() throws Exception {
        var changeLogParser = new ChangeLogXMLParser(classLoader);

        assertTrue(supportedFile.createNewFile());
        assertTrue(unsupportedFile.createNewFile());
        mapper.writeValue(supportedFile, new JsonParent(List.of(), List.of()));
        mapper.writeValue(unsupportedFile, new JsonParent(List.of(), List.of()));

        // Test ChangeSet is added to target file as expected
        ChangeSet changeSet = changeLogParser.extractChangeSets("META-INF/jpa-changelog-1.xml").get(0);
        mojo.addChangeSet(classLoader, changeSet, getTargetFile(), getAlternateFile());

        JsonParent parent = mapper.readValue(getTargetFile(), new TypeReference<>() {});
        List<ChangeSet> targetChanges = new ArrayList<>(parent.changeSets());
        assertEquals(1, targetChanges.size());
        ChangeSet sChange = targetChanges.get(0);
        assertEquals(changeSet.id(), sChange.id());
        assertEquals(changeSet.author(), sChange.author());
        assertEquals(changeSet.filename(), sChange.filename());

        // Test subsequent ChangeSets are added to already populated target file
        changeSet = changeLogParser.extractChangeSets("META-INF/jpa-changelog-2.xml").get(0);
        mojo.addChangeSet(classLoader, changeSet, getTargetFile(), getAlternateFile());

        parent = mapper.readValue(getTargetFile(), new TypeReference<>() {});
        targetChanges = new ArrayList<>(parent.changeSets());
        assertEquals(2, targetChanges.size());

        sChange = targetChanges.get(1);
        assertEquals(changeSet.id(), sChange.id());
        assertEquals(changeSet.author(), sChange.author());
        assertEquals(changeSet.filename(), sChange.filename());

        // Test ChangeSet already exists handled gracefully
        mojo.addChangeSet(classLoader, changeSet, getTargetFile(), getAlternateFile());

        parent = mapper.readValue(getTargetFile(), new TypeReference<>() {});
        targetChanges = new ArrayList<>(parent.changeSets());
        assertEquals(2, targetChanges.size());
    }

    @Test
    void testChangeAlreadyExistsInAlternateFile() throws Exception {
        assertTrue(supportedFile.createNewFile());
        assertTrue(unsupportedFile.createNewFile());
        mapper.writeValue(getTargetFile(), new JsonParent(List.of(), List.of()));

        // Create alternate file with a single ChangeSet
        List<ChangeSet> alternateChanges = new ChangeLogXMLParser(classLoader).extractChangeSets("META-INF/jpa-changelog-1.xml");
        assertEquals(1, alternateChanges.size());

        ChangeSet changeSet = alternateChanges.get(0);
        mapper.writeValue(getAlternateFile(), new JsonParent(alternateChanges, List.of()));

        Exception e = assertThrows(
              MojoExecutionException.class,
              () -> mojo.addChangeSet(classLoader, changeSet, getTargetFile(), getAlternateFile())
        );

        assertEquals("ChangeSet already defined in the %s file".formatted(getAlternateFile().getName()), e.getMessage());
    }

    @Test
    void testAddUnknownChangeSet() throws Exception {
        assertTrue(supportedFile.createNewFile());
        assertTrue(unsupportedFile.createNewFile());

        mapper.writeValue(getAlternateFile(), new JsonParent(List.of(), List.of()));
        ChangeSet unknown = new ChangeSet("asf", "asfgasg", "afasgfas");

        Exception e = assertThrows(
              MojoExecutionException.class,
              () -> mojo.addChangeSet(classLoader, unknown, getTargetFile(), getAlternateFile())
        );

        assertEquals("Unknown ChangeSet: " + unknown, e.getMessage());
    }


    @Test
    void testAddMigration() throws Exception {
        assertTrue(supportedFile.createNewFile());
        assertTrue(unsupportedFile.createNewFile());
        mapper.writeValue(supportedFile, new JsonParent(List.of(), List.of()));
        mapper.writeValue(unsupportedFile, new JsonParent(List.of(), List.of()));

        // Test Migration is added to target file as expected
        Migration migration = new Migration(getClass().getName());
        mojo.addMigration(classLoader, migration, getTargetFile(), getAlternateFile());

        JsonParent parent = mapper.readValue(getTargetFile(), new TypeReference<>() {});
        List<Migration> supportedMigrations = new ArrayList<>(parent.migrations());
        assertEquals(1, supportedMigrations.size());
        Migration sMigration = supportedMigrations.get(0);
        assertEquals(migration.clazz(), sMigration.clazz());

        // Test subsequent Migration is added to already populated target file
        migration = new Migration(VerifyMojoTest.class.getName());
        mojo.addMigration(classLoader, migration, getTargetFile(), getAlternateFile());

        parent = mapper.readValue(getTargetFile(), new TypeReference<>() {});
        supportedMigrations = new ArrayList<>(parent.migrations());
        assertEquals(2, supportedMigrations.size());

        sMigration = supportedMigrations.get(1);
        assertEquals(migration.clazz(), sMigration.clazz());

        // Test existing Migration handled gracefully
        mojo.addMigration(classLoader, migration, getTargetFile(), getAlternateFile());

        parent = mapper.readValue(getTargetFile(), new TypeReference<>() {});
        supportedMigrations = new ArrayList<>(parent.migrations());
        assertEquals(2, supportedMigrations.size());
    }

    @Test
    void testMigrationAlreadyExistsInAlternateFile() throws Exception {
        assertTrue(supportedFile.createNewFile());
        assertTrue(unsupportedFile.createNewFile());
        mapper.writeValue(getTargetFile(), new JsonParent(List.of(), List.of()));

        // Create alternate file with a single Migration
        var migration = new Migration(getClass().getName());
        mapper.writeValue(getAlternateFile(), new JsonParent(List.of(), List.of(migration)));

        Exception e = assertThrows(
              MojoExecutionException.class,
              () -> mojo.addMigration(classLoader, migration, getTargetFile(), getAlternateFile())
        );

        assertEquals("Migration already defined in the %s file".formatted(getAlternateFile().getName()), e.getMessage());
    }

    @Test
    void testAddUnknownMigration() throws Exception {
        assertTrue(supportedFile.createNewFile());
        assertTrue(unsupportedFile.createNewFile());

        mapper.writeValue(getTargetFile(), new JsonParent(List.of(), List.of()));
        var unknown = new Migration("unknownClass");

        Exception e = assertThrows(
              MojoExecutionException.class,
              () -> mojo.addMigration(classLoader, unknown, getTargetFile(), getAlternateFile())
        );
        assertEquals("Unknown Migration: " + unknown, e.getMessage());
    }
}

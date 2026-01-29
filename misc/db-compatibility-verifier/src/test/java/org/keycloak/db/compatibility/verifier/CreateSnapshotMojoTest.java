package org.keycloak.db.compatibility.verifier;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CreateSnapshotMojoTest extends AbstractMojoTest {

    @Test
    void testSnapshotFilesCreated() throws Exception {
        var classLoader = CreateSnapshotMojoTest.class.getClassLoader();
        var mojo = new CreateSnapshotMojo();
        mojo.createSnapshot(classLoader, supportedFile, unsupportedFile);

        assertTrue(supportedFile.exists());
        assertTrue(unsupportedFile.exists());

        var mapper = new ObjectMapper();
        List<ChangeSet> supportedChanges = mapper.readValue(supportedFile, new TypeReference<>() {});
        assertEquals(2, supportedChanges.size());

        List<ChangeSet> unsupportedChanges = mapper.readValue(unsupportedFile, new TypeReference<>() {});
        assertEquals(0, unsupportedChanges.size());
    }
}

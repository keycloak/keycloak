package org.keycloak.schema.generator;

import java.util.List;

import org.keycloak.representations.migration.ChangeSetDescriptor;
import org.keycloak.representations.migration.MigrationManifest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ManifestDriftCheckerTest {

    @Test
    public void testNoViolationsWhenUnchangedOrAppended() {
        MigrationManifest baseline = manifest(cs("a", "9:1"), cs("b", "9:2"));
        MigrationManifest current = manifest(cs("a", "9:1"), cs("b", "9:2"), cs("c", "9:3"));

        assertTrue(ManifestDriftChecker.check(baseline, current).isEmpty());
    }

    @Test
    public void testModifiedChecksumIsReported() {
        MigrationManifest baseline = manifest(cs("a", "9:1"));
        MigrationManifest current = manifest(cs("a", "9:changed"));

        List<String> violations = ManifestDriftChecker.check(baseline, current);
        assertEquals(1, violations.size());
        assertTrue(violations.get(0).contains("modified"), violations.get(0));
    }

    @Test
    public void testRemovedChangeSetIsReported() {
        MigrationManifest baseline = manifest(cs("a", "9:1"), cs("b", "9:2"));
        MigrationManifest current = manifest(cs("a", "9:1"));

        List<String> violations = ManifestDriftChecker.check(baseline, current);
        assertEquals(1, violations.size());
        assertTrue(violations.get(0).contains("removed"), violations.get(0));
    }

    private static ChangeSetDescriptor cs(String id, String checkSum) {
        ChangeSetDescriptor cs = new ChangeSetDescriptor();
        cs.setId(id);
        cs.setAuthor("keycloak");
        cs.setChangeLogFile("META-INF/test.xml");
        cs.setCheckSum(checkSum);
        return cs;
    }

    private static MigrationManifest manifest(ChangeSetDescriptor... changeSets) {
        MigrationManifest manifest = new MigrationManifest();
        manifest.getChangeSets().addAll(List.of(changeSets));
        return manifest;
    }
}

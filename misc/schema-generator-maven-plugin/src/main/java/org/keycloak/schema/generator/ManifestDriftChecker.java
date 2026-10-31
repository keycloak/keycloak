package org.keycloak.schema.generator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.keycloak.representations.migration.ChangeSetDescriptor;
import org.keycloak.representations.migration.MigrationManifest;

/**
 * Build-time drift detection. The runtime updater only looks at the change-set key (id, author, file) to decide
 * whether a change-set has already been applied, so editing a released change-set has no effect on existing
 * databases. This check compares a newly generated manifest with the manifest of a previous release and reports
 * released change-sets that were removed or whose checksum changed.
 */
public class ManifestDriftChecker {

    private ManifestDriftChecker() {
    }

    /**
     * @return human readable descriptions of all violations; empty when the manifest is compatible with the baseline
     */
    public static List<String> check(MigrationManifest baseline, MigrationManifest current) {
        Map<String, ChangeSetDescriptor> currentByKey = new LinkedHashMap<>();
        for (ChangeSetDescriptor cs : current.getChangeSets()) {
            currentByKey.put(key(cs), cs);
        }

        List<String> violations = new ArrayList<>();
        for (ChangeSetDescriptor released : baseline.getChangeSets()) {
            ChangeSetDescriptor cs = currentByKey.get(key(released));
            if (cs == null) {
                violations.add("Released change-set was removed: " + key(released));
            } else if (!Objects.equals(released.getCheckSum(), cs.getCheckSum())) {
                violations.add("Released change-set was modified: " + key(released) + " (checksum "
                        + released.getCheckSum() + " -> " + cs.getCheckSum() + "). Add a new change-set instead.");
            }
        }
        return violations;
    }

    private static String key(ChangeSetDescriptor cs) {
        return cs.getId() + "::" + cs.getAuthor() + "::" + cs.getChangeLogFile();
    }
}

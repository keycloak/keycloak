package org.keycloak.compatibility;

import java.util.Map;

import org.keycloak.common.Profile;
import org.keycloak.common.Version;
import org.keycloak.migration.ModelVersion;
import org.keycloak.migration.ModelVersionUtils;

/**
 * A {@link CompatibilityMetadataProvider} implementation to provide the Keycloak version.
 */
public class KeycloakCompatibilityMetadataProvider implements CompatibilityMetadataProvider {

    public static final String ID = "keycloak";
    public static final String VERSION_KEY = "version";

    @Override
    public Map<String, String> metadata() {
        return Map.of(VERSION_KEY, Version.VERSION);
    }

    @Override
    public CompatibilityResult isCompatible(Map<String, String> other) {
        CompatibilityResult equalComparison = CompatibilityMetadataProvider.super.isCompatible(other);

        // If V2 feature is enabled, we consider versions upgradable in rolling way also if other is the previous micro release
        if (Profile.isFeatureEnabled(Profile.Feature.ROLLING_UPDATES_V2) && Util.isNotCompatible(equalComparison)) {
            String otherVersion = other.get(VERSION_KEY);

            // We need to make sure the previous version is not null
            if (otherVersion == null) {
                return equalComparison;
            }

            ModelVersion otherModelVersion = new ModelVersion(otherVersion);
            ModelVersion currentModelVersion = new ModelVersion(metadata().get(VERSION_KEY));
            if (!ModelVersionUtils.areSameMajorMinorVersions(otherModelVersion, currentModelVersion)) {
                return equalComparison;
            }

            // We are in the same major.minor release stream
            int otherMicro = otherModelVersion.getMicro();
            int currentMicro = currentModelVersion.getMicro();

            // Do not allow rolling rollback
            if (currentMicro < otherMicro) {
                return equalComparison;
            }

            return CompatibilityResult.providerCompatible(ID);
        }

        return equalComparison;
    }

    @Override
    public String getId() {
        return ID;
    }
}

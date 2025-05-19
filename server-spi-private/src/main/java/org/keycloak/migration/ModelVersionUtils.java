package org.keycloak.migration;

public class ModelVersionUtils {

    /**
     * Return true if {@code v1} and {@code v2} versions are the same major.minor version
     * <p/>
     * Note: this method is not taking version qualifier into account
     *
     * @param v1 First version
     * @param v2 Second version
     * @return true if versions are the same major.minor
     */
    public static boolean areSameMajorMinorVersions(ModelVersion v1, ModelVersion v2) {
        return v1.major == v2.major && v1.minor == v2.minor;
    }
}

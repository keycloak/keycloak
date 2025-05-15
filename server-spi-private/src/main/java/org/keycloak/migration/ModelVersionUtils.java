package org.keycloak.migration;

public class ModelVersionUtils {

    /**
     * Return true if {@code firstVersion} and {@code secondVersion} versions are consecutive in given order.
     * <p/>
     * Example of consecutive versions:
     *   areConsecutiveOrSameMicroVersions(26.2.2, 26.2.3) = true
     *   areConsecutiveOrSameMicroVersions(26.2.3, 26.2.2) = false
     *   areConsecutiveOrSameMicroVersions(26.2.2, 26.2.4) = false
     *   areConsecutiveOrSameMicroVersions(26.2.2, 26.3.3) = false
     * <p/>
     * Note: this method is not taking version qualifier into account
     *
     * @param firstVersion First version
     * @param secondVersion Second version
     * @return true if versions are consecutive
     */
    public static boolean areConsecutiveOrSameMicroVersions(ModelVersion firstVersion, ModelVersion secondVersion) {
        if (firstVersion.major != secondVersion.major || firstVersion.minor != secondVersion.minor) {
            return false;
        }

        return firstVersion.micro == secondVersion.micro || firstVersion.micro == secondVersion.micro - 1;
    }
}

package org.keycloak.migration;

public class ModelVersionUtils {

    /**
     * Return true if {@code v1} and {@code v2} versions are consecutive in given order.
     * <p/>
     * Example of consecutive versions:
     *   areConsecutiveOrSameMicroVersions(26.2.2, 26.2.3) = true
     *   areConsecutiveOrSameMicroVersions(26.2.3, 26.2.2) = false
     *   areConsecutiveOrSameMicroVersions(26.2.2, 26.2.4) = false
     *   areConsecutiveOrSameMicroVersions(26.2.2, 26.3.3) = false
     * <p/>
     * Note: this method is not taking version qualifier into account
     *
     * @param v1 First version
     * @param v2 Second version
     * @return true if versions are consecutive
     */
    public static boolean areConsecutiveOrSameMicroVersions(ModelVersion v1, ModelVersion v2) {
        if (v1.major != v2.major || v1.minor != v2.minor) {
            return false;
        }

        return v1.micro == v2.micro || v1.micro == v2.micro - 1;
    }
}

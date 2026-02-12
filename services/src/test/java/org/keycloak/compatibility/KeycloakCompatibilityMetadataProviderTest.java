package org.keycloak.compatibility;

import java.util.Map;

import org.keycloak.common.Profile;

import org.junit.Test;

import static org.keycloak.compatibility.KeycloakCompatibilityMetadataProvider.VERSION_KEY;

public class KeycloakCompatibilityMetadataProviderTest extends AbstractCompatibilityMetadataProviderTest {

    @Test
    public void testMicroVersionUpgradeWorksWithRollingUpdateV2() {

        // Make compatibility provider return hardcoded version as we are not able to test this in integration tests with micro versions equal to 0
        KeycloakCompatibilityMetadataProvider compatibilityProvider = new KeycloakCompatibilityMetadataProvider("999.999.999-Final");

        // Test compatible
        assertCompatibility(CompatibilityResult.ExitCode.ROLLING, compatibilityProvider.isCompatible(Map.of(VERSION_KEY, "999.999.999-Final")));
        assertCompatibility(CompatibilityResult.ExitCode.ROLLING, compatibilityProvider.isCompatible(Map.of(VERSION_KEY, "999.999.998-Final")));
        assertCompatibility(CompatibilityResult.ExitCode.ROLLING, compatibilityProvider.isCompatible(Map.of(VERSION_KEY, "999.999.999-Final1")));
        assertCompatibility(CompatibilityResult.ExitCode.ROLLING, compatibilityProvider.isCompatible(Map.of(VERSION_KEY, "999.999.1-Final")));

        // Test incompatible
        assertCompatibility(CompatibilityResult.ExitCode.RECREATE, compatibilityProvider.isCompatible(Map.of(VERSION_KEY, "999.999.1000-Final")));
        assertCompatibility(CompatibilityResult.ExitCode.RECREATE, compatibilityProvider.isCompatible(Map.of(VERSION_KEY, "999.998.999-Final")));
        assertCompatibility(CompatibilityResult.ExitCode.RECREATE, compatibilityProvider.isCompatible(Map.of(VERSION_KEY, "998.999.999-Final")));
        assertCompatibility(CompatibilityResult.ExitCode.RECREATE, compatibilityProvider.isCompatible(Map.of(VERSION_KEY, "999.998.998-Final")));
        assertCompatibility(CompatibilityResult.ExitCode.RECREATE, compatibilityProvider.isCompatible(Map.of(VERSION_KEY, "998.999.998-Final")));

        Profile.reset();
    }

    @Test
    public void testRollingUpgradeRefusedWithOtherMetadataNotEquals() {

        // Make compatibility provider return hardcoded version as we are not able to test this in integration tests with micro versions equal to 0
        KeycloakCompatibilityMetadataProvider compatibilityProvider = new KeycloakCompatibilityMetadataProvider("999.999.999-Final") {
            @Override
            public Map<String, String> metadata() {
                return Map.of(VERSION_KEY, "999.999.999-Final",
                        "key2", "value2");
            }
        };

        // Test compatible
        assertCompatibility(CompatibilityResult.ExitCode.ROLLING, compatibilityProvider.isCompatible(Map.of(VERSION_KEY, "999.999.998-Final", "key2", "value2")));

        // Test incompatible
        assertCompatibility(CompatibilityResult.ExitCode.RECREATE, compatibilityProvider.isCompatible(Map.of(VERSION_KEY, "999.999.998-Final", "key2", "different-value")));
        assertCompatibility(CompatibilityResult.ExitCode.RECREATE, compatibilityProvider.isCompatible(Map.of(VERSION_KEY, "999.999.998-Final")));
    }
}

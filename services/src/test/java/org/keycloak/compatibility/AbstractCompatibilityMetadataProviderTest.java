package org.keycloak.compatibility;

import static org.junit.Assert.assertEquals;

abstract class AbstractCompatibilityMetadataProviderTest {
    protected void assertCompatibility(CompatibilityResult.ExitCode expected, CompatibilityResult actual) {
        assertEquals("Expected compatibility result was " + expected, expected.exitCode, actual.exitCode());
    }
}

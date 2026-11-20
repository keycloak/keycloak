package org.keycloak.tests.suites;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/**
 * Tests that run against clusters mixing released and current versions of Keycloak.
 */
@Suite
@SelectPackages("org.keycloak.tests.compatibility")
public class MixedClusterTestSuite {
}

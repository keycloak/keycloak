package org.keycloak.tests.suites;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/**
 * Tests that run against clusters of the current build only.
 */
@Suite
@SelectPackages("org.keycloak.tests.clustering")
public class ClusterTestSuite {
}

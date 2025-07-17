package org.keycloak.tests.suites;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectPackages({"org.keycloak.tests.admin", "org.keycloak.tests.db"})
public class DatabaseTestSuite {
}

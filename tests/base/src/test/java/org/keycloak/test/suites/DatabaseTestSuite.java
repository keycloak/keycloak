package org.keycloak.test.suites;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectPackages({"org.keycloak.test.admin"})
public class DatabaseTestSuite {
}

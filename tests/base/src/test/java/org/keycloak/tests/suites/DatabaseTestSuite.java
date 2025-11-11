package org.keycloak.tests.suites;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.keycloak.tests.keys.GeneratedRsaKeyProviderTest;

@Suite
@SelectPackages({
        "org.keycloak.tests.admin",
        "org.keycloak.tests.db"
})
@SelectClasses({
        GeneratedRsaKeyProviderTest.class
})
public class DatabaseTestSuite {
}

package org.keycloak.tests.suites;

import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@IncludeTags(DatabaseTest.TAG)
@SelectPackages({
        "org.keycloak.tests"
})
public class DatabaseTestSuite {
}

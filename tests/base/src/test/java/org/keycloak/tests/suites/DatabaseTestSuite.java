package org.keycloak.tests.suites;

import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

import org.keycloak.tests.policy.PasswordPolicyTest;

@Suite
@IncludeTags(DatabaseTest.TAG)
@SelectPackages({
        "org.keycloak.tests"
})
public class DatabaseTestSuite {

}

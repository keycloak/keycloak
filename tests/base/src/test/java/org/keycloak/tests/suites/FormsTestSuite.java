package org.keycloak.tests.suites;

import org.keycloak.tests.i18n.LoginPageTest;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
// TODO: Select relevant test classes or packages once they have been migrated
@SelectClasses({
        LoginPageTest.class
})
public class FormsTestSuite {
}

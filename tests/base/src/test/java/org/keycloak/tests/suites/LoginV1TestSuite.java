package org.keycloak.tests.suites;

import org.keycloak.tests.i18n.LoginPageTest;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
        LoginPageTest.class
})
public class LoginV1TestSuite {
}

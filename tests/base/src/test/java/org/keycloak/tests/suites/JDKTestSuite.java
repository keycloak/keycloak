package org.keycloak.tests.suites;

import org.keycloak.tests.admin.client.CredentialsTest;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({CredentialsTest.class})
public class JDKTestSuite {
}

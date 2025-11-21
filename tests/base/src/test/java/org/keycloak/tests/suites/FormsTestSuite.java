package org.keycloak.tests.suites;

import org.keycloak.tests.admin.AdminHeadersTest;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
// TODO: Select relevant test classes or packages once they have been migrated
@SelectClasses({AdminHeadersTest.class})
public class FormsTestSuite {
}

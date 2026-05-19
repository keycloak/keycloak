package org.keycloak.testframework.tests.suites;

import org.keycloak.testframework.tests.CustomServerConfigTest;
import org.keycloak.testframework.tests.LogTest;
import org.keycloak.testframework.tests.ManagedResourcesTest;
import org.keycloak.testframework.tests.RunOnServerTest;
import org.keycloak.testframework.tests.TlsEnabledTest;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
        CustomServerConfigTest.class,
        RunOnServerTest.class,
        TlsEnabledTest.class,
        ManagedResourcesTest.class,
        LogTest.class
})
public class EmbeddedTestSuite {
}

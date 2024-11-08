package org.keycloak.test.suites;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.keycloak.test.tmp.PlaceHolderTest;

@Suite
@SelectClasses({ PlaceHolderTest.class })
public class DatabaseTestSuite {
}

package org.keycloak.tests.suites;

import org.keycloak.tests.admin.client.CredentialsTest;
import org.keycloak.tests.keys.GeneratedRsaKeyProviderTest;
import org.keycloak.tests.keys.JavaKeystoreKeyProviderTest;
import org.keycloak.tests.transactions.TransactionsTest;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
        CredentialsTest.class,
        GeneratedRsaKeyProviderTest.class,
        JavaKeystoreKeyProviderTest.class,
        TransactionsTest.class
})
public class JDKTestSuite {
}

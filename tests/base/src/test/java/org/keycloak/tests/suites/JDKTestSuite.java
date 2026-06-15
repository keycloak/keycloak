package org.keycloak.tests.suites;

import org.keycloak.tests.account.AccountRestServiceTest;
import org.keycloak.tests.admin.client.CredentialsTest;
import org.keycloak.tests.client.MutualTLSClientTest;
import org.keycloak.tests.exportimport.ExportImportTest;
import org.keycloak.tests.forms.LoginTest;
import org.keycloak.tests.forms.SSOTest;
import org.keycloak.tests.keys.GeneratedRsaKeyProviderTest;
import org.keycloak.tests.keys.JavaKeystoreKeyProviderTest;
import org.keycloak.tests.oauth.AuthorizationCodeTest;
import org.keycloak.tests.policy.PasswordPolicyTest;
import org.keycloak.tests.transactions.TransactionsTest;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
        AccountRestServiceTest.class,
        CredentialsTest.class,
        ExportImportTest.class,
        GeneratedRsaKeyProviderTest.class,
        JavaKeystoreKeyProviderTest.class,
        PasswordPolicyTest.class,
        SSOTest.class,
        TransactionsTest.class,
        MutualTLSClientTest.class,
        LoginTest.class,
        AuthorizationCodeTest.class
})
public class JDKTestSuite {
}

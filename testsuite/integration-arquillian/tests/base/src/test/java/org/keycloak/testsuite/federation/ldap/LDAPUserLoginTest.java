/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.keycloak.testsuite.federation.ldap;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runners.MethodSorters;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.OAuth2Constants;
import org.keycloak.models.ModelException;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.EnableVault;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.runonserver.RunOnServerException;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPRule.LDAPConnectionParameters;
import org.keycloak.testsuite.util.LDAPTestConfiguration;
import org.keycloak.testsuite.util.LDAPTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

import java.util.Objects;
import org.junit.Assume;
import static org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer.REMOTE;

/**
 * Test user logins utilizing various LDAP authentication methods and different LDAP connection encryption mechanisms.
 *
 * @author <a href="mailto:jlieskov@redhat.com">Jan Lieskovsky</a>
 */
@EnableVault
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPUserLoginTest extends AbstractLDAPTest {

    @Rule
    // Start an embedded LDAP server with configuration derived from test annotations before each test
    public LDAPRule ldapRule = new LDAPRule()
            .assumeTrue((LDAPTestConfiguration ldapConfig) -> {

                return ldapConfig.isStartEmbeddedLdapServer();

            });

    @Override
    protected LDAPRule getLDAPRule() {
        return ldapRule;
    }

    @Rule
    // Recreate a new LDAP provider based on test annotations before each test
    public ExternalResource ldapProviderRule = new ExternalResource() {

        @Override
        protected void after() {
            // Delete the previously imported realm(s) after each test. This forces
            // a new LDAP provider with custom configuration (derived from the test
            // annotations) to be created each time the next test is run
            if (getTestingClient() != null) {
                getTestContext().getTestRealmReps().clear();
            }
        }

    };

    @Rule
    public AssertEvents events = new AssertEvents(this);

    protected static final Map<String, String> DEFAULT_TEST_USERS = new HashMap<String, String>();
    static {
        DEFAULT_TEST_USERS.put("EMPTY_USER_PASSWORD", new String());
        DEFAULT_TEST_USERS.put("INVALID_USER_NAME", "userUnknown");
        DEFAULT_TEST_USERS.put("INVALID_USER_EMAIL", "unknown@keycloak.org");
        DEFAULT_TEST_USERS.put("INVALID_USER_PASSWORD", "1nval!D");
        DEFAULT_TEST_USERS.put("VALID_USER_EMAIL", "jdoe@keycloak.org");
        DEFAULT_TEST_USERS.put("VALID_USER_NAME", "jdoe");
        DEFAULT_TEST_USERS.put("VALID_USER_FIRST_NAME", "John");
        DEFAULT_TEST_USERS.put("VALID_USER_LAST_NAME", "Doe");
        DEFAULT_TEST_USERS.put("VALID_USER_PASSWORD", "P@ssw0rd!");
        DEFAULT_TEST_USERS.put("VALID_USER_POSTAL_CODE", "12345");
        DEFAULT_TEST_USERS.put("VALID_USER_STREET", "1th Avenue");
    }

    @Override
    protected void afterImportTestRealm() {
        try {
            getTestingClient().server().run(session -> {
                LDAPTestContext ctx = LDAPTestContext.init(session);
                RealmModel appRealm = ctx.getRealm();

                // Delete all LDAP users
                LDAPTestUtils.removeAllLDAPUsers(ctx.getLdapProvider(), appRealm);
                // Add some new LDAP users for testing
                LDAPObject john = LDAPTestUtils.addLDAPUser
                (
                    ctx.getLdapProvider(),
                    appRealm,
                    DEFAULT_TEST_USERS.get("VALID_USER_NAME"),
                    DEFAULT_TEST_USERS.get("VALID_USER_FIRST_NAME"),
                    DEFAULT_TEST_USERS.get("VALID_USER_LAST_NAME"),
                    DEFAULT_TEST_USERS.get("VALID_USER_EMAIL"),
                    DEFAULT_TEST_USERS.get("VALID_USER_STREET"),
                    DEFAULT_TEST_USERS.get("VALID_USER_POSTAL_CODE")
                );
                LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), john, DEFAULT_TEST_USERS.get("VALID_USER_PASSWORD"));
            });
        } catch (RunOnServerException ex) {
            Assume.assumeFalse("Work around JDK-8214440",
                 ex.getCause() instanceof ModelException
              && ex.getCause().getCause() instanceof ModelException
              && ex.getCause().getCause().getCause() instanceof javax.naming.AuthenticationException
              && Objects.equals(ex.getCause().getCause().getCause().getMessage(), "Could not negotiate TLS"));
        }
    }

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    // Helper methods
    private void verifyLoginSucceededAndLogout(String username, String password) {
        loginPage.open();
        loginPage.login(username, password);
        appPage.assertCurrent();
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));
        appPage.logout();
    }

    private void verifyLoginFailed(String username, String password) {
        // Clear the events queue before the actual test to catch all errors properly
        events.clear();
        // Run the test actions
        loginPage.open();
        loginPage.login(username, password);
        Assert.assertEquals("Invalid username or password.", loginPage.getInputError());

        if (username.equals(DEFAULT_TEST_USERS.get("INVALID_USER_EMAIL")) || username.equals(DEFAULT_TEST_USERS.get("INVALID_USER_NAME"))) {

            events.expect(EventType.LOGIN_ERROR).user((String) null).error(Errors.USER_NOT_FOUND).assertEvent();

        } else if (username.equals(DEFAULT_TEST_USERS.get("VALID_USER_EMAIL")) || username.equals(DEFAULT_TEST_USERS.get("VALID_USER_NAME"))) {

            List<UserRepresentation> knownUsers = getAdminClient().realm(TEST_REALM_NAME).users().search(DEFAULT_TEST_USERS.get("VALID_USER_NAME"));
            Assert.assertTrue(!knownUsers.isEmpty());
            final String userId = knownUsers.get(0).getId();
            events.expect(EventType.LOGIN_ERROR).user(userId).error(Errors.INVALID_USER_CREDENTIALS).assertEvent();

        }
    }

    private void runLDAPLoginTest() {
        final String emptyPassword = DEFAULT_TEST_USERS.get("EMPTY_USER_PASSWORD");
        final String invalidEmail = DEFAULT_TEST_USERS.get("INVALID_USER_EMAIL");
        final String invalidPassword = DEFAULT_TEST_USERS.get("INVALID_USER_PASSWORD");
        final String invalidUsername = DEFAULT_TEST_USERS.get("INVALID_USER_NAME");
        final String validEmail = DEFAULT_TEST_USERS.get("VALID_USER_EMAIL");
        final String validPassword = DEFAULT_TEST_USERS.get("VALID_USER_PASSWORD");
        final String validUsername = DEFAULT_TEST_USERS.get("VALID_USER_NAME");

        // Check LDAP login via valid username + valid password
        verifyLoginSucceededAndLogout(validUsername, validPassword);
        // Check LDAP login via valid email + valid password
        verifyLoginSucceededAndLogout(validEmail, validPassword);
        // Check LDAP login via valid username + empty password
        verifyLoginFailed(validUsername, emptyPassword);
        // Check LDAP login via valid email + empty password
        verifyLoginFailed(validEmail, emptyPassword);
        // Check LDAP login via valid username + invalid password
        verifyLoginFailed(validUsername, invalidPassword);
        // Check LDAP login via valid email + invalid password
        verifyLoginFailed(validEmail, invalidPassword);
        // Check LDAP login via invalid username
        verifyLoginFailed(invalidUsername, invalidPassword);
        // Check LDAP login via invalid email
        verifyLoginFailed(invalidEmail, invalidPassword);
    }

    private void verifyConnectionUrlProtocolPrefix(String ldapProtocolPrefix) {
        final String ldapConnectionUrl = ldapRule.getConfig().get(LDAPConstants.CONNECTION_URL);
        Assert.assertTrue(!ldapConnectionUrl.isEmpty() && ldapConnectionUrl.startsWith(ldapProtocolPrefix));
    }

    // Tests themselves

    // Check LDAP federated user (in)valid login(s) with simple authentication & encryption (both SSL and startTLS) disabled
    // Test variant: Bind credential set to secret (default)
    @Test
    @LDAPConnectionParameters(bindType=LDAPConnectionParameters.BindType.SIMPLE, encryption=LDAPConnectionParameters.Encryption.NONE)
    public void loginLDAPUserAuthenticationSimpleEncryptionNone() {
        verifyConnectionUrlProtocolPrefix("ldap://");
        runLDAPLoginTest();
    }

    // Check LDAP federated user (in)valid login(s) with simple authentication & encryption (both SSL and startTLS) disabled
    // Test variant: Bind credential set to vault
    @Test
    @LDAPConnectionParameters(bindCredential=LDAPConnectionParameters.BindCredential.VAULT, bindType=LDAPConnectionParameters.BindType.SIMPLE, encryption=LDAPConnectionParameters.Encryption.NONE)
    @AuthServerContainerExclude(value = REMOTE, details =
            "java.io.NotSerializableException: com.sun.jndi.ldap.LdapCtx")
    public void loginLDAPUserCredentialVaultAuthenticationSimpleEncryptionNone() {
        verifyConnectionUrlProtocolPrefix("ldap://");
        runLDAPLoginTest();
    }

    // Check LDAP federated user (in)valid login(s) with simple authentication & SSL encryption enabled
    // Test variant: Bind credential set to secret (default)
    @Test
    @LDAPConnectionParameters(bindType=LDAPConnectionParameters.BindType.SIMPLE, encryption=LDAPConnectionParameters.Encryption.SSL)
    public void loginLDAPUserAuthenticationSimpleEncryptionSSL() {
        verifyConnectionUrlProtocolPrefix("ldaps://");
        runLDAPLoginTest();
    }

    // Check LDAP federated user (in)valid login(s) with simple authentication & SSL encryption enabled
    // Test variant: Bind credential set to vault
    @Test
    @LDAPConnectionParameters(bindCredential=LDAPConnectionParameters.BindCredential.VAULT, bindType=LDAPConnectionParameters.BindType.SIMPLE, encryption=LDAPConnectionParameters.Encryption.SSL)
    @AuthServerContainerExclude(value = REMOTE, details =
            "java.io.NotSerializableException: com.sun.jndi.ldap.LdapCtx")
    public void loginLDAPUserCredentialVaultAuthenticationSimpleEncryptionSSL() {
        verifyConnectionUrlProtocolPrefix("ldaps://");
        runLDAPLoginTest();
    }

    // Check LDAP federated user (in)valid login(s) with simple authentication & startTLS encryption enabled
    // Test variant: Bind credential set to secret (default)
    @Test
    @LDAPConnectionParameters(bindType=LDAPConnectionParameters.BindType.SIMPLE, encryption=LDAPConnectionParameters.Encryption.STARTTLS)
    public void loginLDAPUserAuthenticationSimpleEncryptionStartTLS() {
        verifyConnectionUrlProtocolPrefix("ldap://");
        runLDAPLoginTest();
    }

    // Check LDAP federated user (in)valid login(s) with simple authentication & startTLS encryption enabled
    // Test variant: Bind credential set to vault
    @Test
    @LDAPConnectionParameters(bindCredential=LDAPConnectionParameters.BindCredential.VAULT, bindType=LDAPConnectionParameters.BindType.SIMPLE, encryption=LDAPConnectionParameters.Encryption.STARTTLS)
    @AuthServerContainerExclude(value = REMOTE, details =
            "java.io.NotSerializableException: com.sun.jndi.ldap.LdapCtx")
    public void loginLDAPUserCredentialVaultAuthenticationSimpleEncryptionStartTLS() {
        verifyConnectionUrlProtocolPrefix("ldap://");
        runLDAPLoginTest();
    }

    // Check LDAP federated user (in)valid login(s) with anonymous authentication & encryption (both SSL and startTLS) disabled
    // Test variant: Bind credential set to secret (default)
    @Test
    @LDAPConnectionParameters(bindType=LDAPConnectionParameters.BindType.NONE, encryption=LDAPConnectionParameters.Encryption.NONE)
    public void loginLDAPUserAuthenticationNoneEncryptionNone() {
        verifyConnectionUrlProtocolPrefix("ldap://");
        runLDAPLoginTest();
    }

    // Check LDAP federated user (in)valid login(s) with anonymous authentication & encryption (both SSL and startTLS) disabled
    // Test variant: Bind credential set to vault
    @Test
    @LDAPConnectionParameters(bindCredential=LDAPConnectionParameters.BindCredential.VAULT, bindType=LDAPConnectionParameters.BindType.NONE, encryption=LDAPConnectionParameters.Encryption.NONE)
    public void loginLDAPUserCredentialVaultAuthenticationNoneEncryptionNone() {
        verifyConnectionUrlProtocolPrefix("ldap://");
        runLDAPLoginTest();
    }

    // Check LDAP federated user (in)valid login(s) with anonymous authentication & SSL encryption enabled
    // Test variant: Bind credential set to secret (default)
    @Test
    @LDAPConnectionParameters(bindType=LDAPConnectionParameters.BindType.NONE, encryption=LDAPConnectionParameters.Encryption.SSL)
    public void loginLDAPUserAuthenticationNoneEncryptionSSL() {
        verifyConnectionUrlProtocolPrefix("ldaps://");
        runLDAPLoginTest();
    }

    // Check LDAP federated user (in)valid login(s) with anonymous authentication & SSL encryption enabled
    // Test variant: Bind credential set to vault
    @Test
    @LDAPConnectionParameters(bindCredential=LDAPConnectionParameters.BindCredential.VAULT, bindType=LDAPConnectionParameters.BindType.NONE, encryption=LDAPConnectionParameters.Encryption.SSL)
    public void loginLDAPUserCredentialVaultAuthenticationNoneEncryptionSSL() {
        verifyConnectionUrlProtocolPrefix("ldaps://");
        runLDAPLoginTest();
    }

    // Check LDAP federated user (in)valid login(s) with anonymous authentication & startTLS encryption enabled
    // Test variant: Bind credential set to secret (default)
    @Test
    @LDAPConnectionParameters(bindType=LDAPConnectionParameters.BindType.NONE, encryption=LDAPConnectionParameters.Encryption.STARTTLS)
    public void loginLDAPUserAuthenticationNoneEncryptionStartTLS() {
        verifyConnectionUrlProtocolPrefix("ldap://");
        runLDAPLoginTest();
    }

    // Check LDAP federated user (in)valid login(s) with anonymous authentication & startTLS encryption enabled
    // Test variant: Bind credential set to vault
    @Test
    @LDAPConnectionParameters(bindCredential=LDAPConnectionParameters.BindCredential.VAULT, bindType=LDAPConnectionParameters.BindType.NONE, encryption=LDAPConnectionParameters.Encryption.STARTTLS)
    public void loginLDAPUserCredentialVaultAuthenticationNoneEncryptionStartTLS() {
        verifyConnectionUrlProtocolPrefix("ldap://");
        runLDAPLoginTest();
    }
}

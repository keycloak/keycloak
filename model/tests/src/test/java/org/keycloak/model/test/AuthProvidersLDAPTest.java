package org.keycloak.model.test;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.authentication.AuthProviderConstants;
import org.keycloak.authentication.AuthenticationProviderException;
import org.keycloak.authentication.AuthenticationProviderManager;
import org.keycloak.models.AuthenticationLinkModel;
import org.keycloak.models.AuthenticationProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.managers.AuthenticationManager;

import javax.ws.rs.core.MultivaluedMap;
import java.util.Arrays;
import java.util.Collections;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AuthProvidersLDAPTest extends AbstractModelTest {

    private static LDAPEmbeddedServer embeddedServer;

    private RealmModel realm;
    private AuthenticationManager am;

    @BeforeClass
    public static void beforeClass() {
        AbstractModelTest.beforeClass();

        try {
            embeddedServer = new LDAPEmbeddedServer();
            embeddedServer.setup();
            embeddedServer.importLDIF("ldap/users.ldif");
        } catch (Exception e) {
            throw new RuntimeException("Error starting Embedded LDAP server.", e);
        }
    }

    @AfterClass
    public static void afterClass() {
        AbstractModelTest.afterClass();

        try {
            embeddedServer.tearDown();
        } catch (Exception e) {
            throw new RuntimeException("Error starting Embedded LDAP server.", e);
        }
    }

    @Before
    public void before() throws Exception {
        super.before();

        // Create realm and configure ldap
        realm = realmManager.createRealm("realm");
        realm.setBruteForceProtected(false);
        realm.addRequiredCredential(CredentialRepresentation.PASSWORD);
        this.embeddedServer.setupLdapInRealm(realm);

        am = new AuthenticationManager();
    }

    @Test
    public void testLdapAuthentication() {
        MultivaluedMap<String, String> formData = AuthProvidersExternalModelTest.createFormData("johnkeycloak", "password");

        // Set password of user in LDAP
        LDAPTestUtils.setLdapPassword(session, realm, "johnkeycloak", "password");

        // Verify that user doesn't exists in realm2 and can't authenticate here
        Assert.assertEquals(AuthenticationManager.AuthenticationStatus.INVALID_USER, am.authenticateForm(session, null, realm, formData));
        Assert.assertNull(session.users().getUserByUsername("johnkeycloak", realm));

        // Add ldap authenticationProvider
        setupAuthenticationProviders();

        // Authenticate john and verify that now he exists in realm
        Assert.assertEquals(AuthenticationManager.AuthenticationStatus.SUCCESS, am.authenticateForm(session, null, realm, formData));
        UserModel john = session.users().getUserByUsername("johnkeycloak", realm);
        Assert.assertNotNull(john);
        Assert.assertEquals("johnkeycloak", john.getUsername());
        Assert.assertEquals("John", john.getFirstName());
        Assert.assertEquals("Doe", john.getLastName());
        Assert.assertEquals("john@email.org", john.getEmail());

        // Verify link exists
        AuthenticationLinkModel authLink = john.getAuthenticationLink();
        Assert.assertNotNull(authLink);
        Assert.assertEquals(authLink.getAuthProvider(), AuthProviderConstants.PROVIDER_NAME_PICKETLINK);
    }

    @Test
    public void testLdapInvalidAuthentication() {
        setupAuthenticationProviders();
        // Add some user and password to realm
        UserModel realmUser = session.users().addUser(realm, "realmUser");
        realmUser.setEnabled(true);
        UserCredentialModel credential = new UserCredentialModel();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue("pass");
        realmUser.updateCredential(credential);

        // User doesn't exists
        MultivaluedMap<String, String> formData = AuthProvidersExternalModelTest.createFormData("invalid", "invalid");
        Assert.assertEquals(AuthenticationManager.AuthenticationStatus.INVALID_USER, am.authenticateForm(session, null, realm, formData));

        // User exists in ldap
        formData = AuthProvidersExternalModelTest.createFormData("johnkeycloak", "invalid");
        Assert.assertEquals(AuthenticationManager.AuthenticationStatus.INVALID_CREDENTIALS, am.authenticateForm(session, null, realm, formData));

        // User exists in realm
        formData = AuthProvidersExternalModelTest.createFormData("realmUser", "invalid");
        Assert.assertEquals(AuthenticationManager.AuthenticationStatus.INVALID_CREDENTIALS, am.authenticateForm(session, null, realm, formData));

        // User disabled
        realmUser.setEnabled(false);
        formData = AuthProvidersExternalModelTest.createFormData("realmUser", "pass");
        Assert.assertEquals(AuthenticationManager.AuthenticationStatus.ACCOUNT_DISABLED, am.authenticateForm(session, null, realm, formData));

        // Successful authentication
        realmUser.setEnabled(true);
        formData = AuthProvidersExternalModelTest.createFormData("realmUser", "pass");
        Assert.assertEquals(AuthenticationManager.AuthenticationStatus.SUCCESS, am.authenticateForm(session, null, realm, formData));
    }

    @Test
    public void testLdapPasswordUpdate() {
        // Add ldap
        setupAuthenticationProviders();

        LDAPTestUtils.setLdapPassword(session, realm, "johnkeycloak", "password");

        // First authenticate successfully to sync john into realm
        MultivaluedMap<String, String> formData = AuthProvidersExternalModelTest.createFormData("johnkeycloak", "password");
        Assert.assertEquals(AuthenticationManager.AuthenticationStatus.SUCCESS, am.authenticateForm(session, null, realm, formData));

        // Change credential and validate that user can authenticate
        AuthenticationProviderManager authProviderManager = AuthenticationProviderManager.getManager(realm, session);

        UserModel john = session.users().getUserByUsername("johnkeycloak", realm);
        try {
            Assert.assertTrue(authProviderManager.updatePassword(john, "password-updated"));
        } catch (AuthenticationProviderException ape) {
            ape.printStackTrace();
            Assert.fail("Error not expected");
        }
        formData = AuthProvidersExternalModelTest.createFormData("johnkeycloak", "password-updated");
        Assert.assertEquals(AuthenticationManager.AuthenticationStatus.SUCCESS, am.authenticateForm(session, null, realm, formData));

        // Password updated just in LDAP, so validating directly in realm should fail
        Assert.assertFalse(realm.validatePassword(john, "password-updated"));

        // Switch to not allow updating passwords in ldap
        AuthProvidersExternalModelTest.setPasswordUpdateForProvider(false, AuthProviderConstants.PROVIDER_NAME_PICKETLINK, realm);

        // Change credential and validate that password is not updated
        try {
            Assert.assertFalse(authProviderManager.updatePassword(john, "password-updated2"));
        } catch (AuthenticationProviderException ape) {
            ape.printStackTrace();
            Assert.fail("Error not expected");
        }
        formData = AuthProvidersExternalModelTest.createFormData("johnkeycloak", "password-updated2");
        Assert.assertEquals(AuthenticationManager.AuthenticationStatus.INVALID_CREDENTIALS, am.authenticateForm(session, null, realm, formData));
    }

    /**
     * Setup authentication providers in realm
     */
    private void setupAuthenticationProviders() {
        AuthenticationProviderModel ap1 = new AuthenticationProviderModel(AuthProviderConstants.PROVIDER_NAME_MODEL, false, Collections.EMPTY_MAP);
        AuthenticationProviderModel ap2 = new AuthenticationProviderModel(AuthProviderConstants.PROVIDER_NAME_PICKETLINK, true, Collections.EMPTY_MAP);
        realm.setAuthenticationProviders(Arrays.asList(ap1, ap2));
    }
}

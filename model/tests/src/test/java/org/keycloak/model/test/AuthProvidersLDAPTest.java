package org.keycloak.model.test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.models.AuthenticationLinkModel;
import org.keycloak.models.AuthenticationProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.spi.authentication.AuthProviderConstants;
import org.keycloak.spi.authentication.AuthenticationProviderException;
import org.keycloak.spi.authentication.AuthenticationProviderManager;
import org.keycloak.util.KeycloakRegistry;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AuthProvidersLDAPTest extends AbstractModelTest {

    private RealmModel realm;
    private AuthenticationManager am;
    private LDAPEmbeddedServer embeddedServer;

    @Before
    @Override
    public void before() throws Exception {
        super.before();

        try {
            this.embeddedServer = new LDAPEmbeddedServer();
            this.embeddedServer.setup();
            this.embeddedServer.importLDIF("ldap/users.ldif");
        } catch (Exception e) {
            throw new RuntimeException("Error starting Embedded LDAP server.", e);
        }

        // Create realm and configure ldap
        realm = realmManager.createRealm("realm");
        realm.addRequiredCredential(CredentialRepresentation.PASSWORD);
        this.embeddedServer.setupLdapInRealm(realm);

        am = new AuthenticationManager();
    }

    @After
    @Override
    public void after() throws Exception {
        super.after();
        try {
            this.embeddedServer.tearDown();
        } catch (Exception e) {
            throw new RuntimeException("Error starting Embedded LDAP server.", e);
        }
    }

    @Test
    public void testLdapAuthentication() {
        MultivaluedMap<String, String> formData = AuthProvidersExternalModelTest.createFormData("john", "password");

        try {
            // this is needed for Picketlink model provider
            ResteasyProviderFactory.pushContext(KeycloakRegistry.class, new KeycloakRegistry());

            // Set password of user in LDAP
            LdapTestUtils.setLdapPassword(realm, "john", "password");

            // Verify that user doesn't exists in realm2 and can't authenticate here
            Assert.assertEquals(AuthenticationManager.AuthenticationStatus.INVALID_USER, am.authenticateForm(realm, formData));
            Assert.assertNull(realm.getUser("john"));

            // Add ldap authenticationProvider
            setupAuthenticationProviders();

            // Authenticate john and verify that now he exists in realm
            Assert.assertEquals(AuthenticationManager.AuthenticationStatus.SUCCESS, am.authenticateForm(realm, formData));
            UserModel john = realm.getUser("john");
            Assert.assertNotNull(john);
            Assert.assertEquals("john", john.getLoginName());
            Assert.assertEquals("John", john.getFirstName());
            Assert.assertEquals("Doe", john.getLastName());
            Assert.assertEquals("john@email.org", john.getEmail());

            // Verify link exists
            Set<AuthenticationLinkModel> authLinks = realm.getAuthenticationLinks(john);
            Assert.assertEquals(1, authLinks.size());
            AuthenticationLinkModel authLink = authLinks.iterator().next();
            Assert.assertEquals(authLink.getAuthProvider(), AuthProviderConstants.PROVIDER_NAME_PICKETLINK);
        } finally {
            ResteasyProviderFactory.clearContextData();
        }

    }

    @Test
    public void testLdapInvalidAuthentication() {
        setupAuthenticationProviders();

        try {
            ResteasyProviderFactory.pushContext(KeycloakRegistry.class, new KeycloakRegistry());

            // Add some user and password to realm
            UserModel realmUser = realm.addUser("realmUser");
            UserCredentialModel credential = new UserCredentialModel();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue("pass");
            realm.updateCredential(realmUser, credential);

            // User doesn't exists
            MultivaluedMap<String, String> formData = AuthProvidersExternalModelTest.createFormData("invalid", "invalid");
            Assert.assertEquals(AuthenticationManager.AuthenticationStatus.INVALID_USER, am.authenticateForm(realm, formData));

            // User exists in ldap
            formData = AuthProvidersExternalModelTest.createFormData("john", "invalid");
            Assert.assertEquals(AuthenticationManager.AuthenticationStatus.INVALID_CREDENTIALS, am.authenticateForm(realm, formData));

            // User exists in realm
            formData = AuthProvidersExternalModelTest.createFormData("realmUser", "invalid");
            Assert.assertEquals(AuthenticationManager.AuthenticationStatus.INVALID_CREDENTIALS, am.authenticateForm(realm, formData));

            // User disabled
            realmUser.setEnabled(false);
            formData = AuthProvidersExternalModelTest.createFormData("realmUser", "pass");
            Assert.assertEquals(AuthenticationManager.AuthenticationStatus.ACCOUNT_DISABLED, am.authenticateForm(realm, formData));
        } finally {
            ResteasyProviderFactory.clearContextData();
        }
    }

    @Test
    public void testLdapPasswordUpdate() {
        // Add ldap
        setupAuthenticationProviders();

        try {
            // this is needed for ldap provider
            ResteasyProviderFactory.pushContext(KeycloakRegistry.class, new KeycloakRegistry());

            // Change credential and validate that user can authenticate
            AuthenticationProviderManager authProviderManager = AuthenticationProviderManager.getManager(realm);
            try {
                authProviderManager.updatePassword("john", "password-updated");
            } catch (AuthenticationProviderException ape) {
                ape.printStackTrace();
                Assert.fail("Error not expected");
            }
            MultivaluedMap<String, String> formData = AuthProvidersExternalModelTest.createFormData("john", "password-updated");
            Assert.assertEquals(AuthenticationManager.AuthenticationStatus.SUCCESS, am.authenticateForm(realm, formData));

            // Password updated just in LDAP, so validating directly in realm should fail
            Assert.assertFalse(realm.validatePassword(realm.getUser("john"), "password-updated"));

            // Switch to not allow updating passwords in ldap
            AuthProvidersExternalModelTest.setPasswordUpdateForProvider(false, AuthProviderConstants.PROVIDER_NAME_PICKETLINK, realm);

            // Change credential and validate that password is not updated
            try {
                authProviderManager.updatePassword("john", "password-updated2");
            } catch (AuthenticationProviderException ape) {
                ape.printStackTrace();
                Assert.fail("Error not expected");
            }
            formData = AuthProvidersExternalModelTest.createFormData("john", "password-updated2");
            Assert.assertEquals(AuthenticationManager.AuthenticationStatus.INVALID_CREDENTIALS, am.authenticateForm(realm, formData));
        } finally {
            ResteasyProviderFactory.clearContextData();
        }
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

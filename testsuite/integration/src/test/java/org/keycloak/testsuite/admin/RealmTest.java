package org.keycloak.testsuite.admin;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.KeycloakServer;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.HashSet;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RealmTest extends AbstractClientTest {

    @Test
    public void getRealms() {
        List<RealmRepresentation> realms = keycloak.realms().findAll();
        assertNames(realms, "master", "test", REALM_NAME);

        for (RealmRepresentation rep : realms) {
            assertNull(rep.getPrivateKey());
            assertNull(rep.getCodeSecret());
            assertNotNull(rep.getPublicKey());
            assertNotNull(rep.getCertificate());
        }
    }

    @Test
    public void createRealmEmpty() {
        try {
            RealmRepresentation rep = new RealmRepresentation();
            rep.setRealm("new-realm");

            keycloak.realms().create(rep);

            assertNames(keycloak.realms().findAll(), "master", "test", REALM_NAME, "new-realm");
        } finally {
            KeycloakSession session = keycloakRule.startSession();
            RealmManager manager = new RealmManager(session);
            RealmModel newRealm = manager.getRealmByName("new-realm");
            if (newRealm != null) {
                manager.removeRealm(newRealm);
            }
            keycloakRule.stopSession(session, true);
        }
    }

    @Test
    public void createRealm() {
        try {
            RealmRepresentation rep = KeycloakServer.loadJson(getClass().getResourceAsStream("/admin-test/testrealm.json"), RealmRepresentation.class);
            keycloak.realms().create(rep);

            RealmRepresentation created = keycloak.realms().realm("admin-test-1").toRepresentation();
            assertRealm(rep, created);
        } finally {
            KeycloakSession session = keycloakRule.startSession();
            RealmManager manager = new RealmManager(session);
            RealmModel newRealm = manager.getRealmByName("admin-test-1");
            if (newRealm != null) {
                manager.removeRealm(newRealm);
            }
            keycloakRule.stopSession(session, true);
        }
    }

    @Test
    public void removeRealm() {
        realm.remove();

        assertNames(keycloak.realms().findAll(), "master", "test");
    }

    @Test
    public void updateRealm() {
        // first change
        RealmRepresentation rep = realm.toRepresentation();
        rep.setSsoSessionIdleTimeout(123);
        rep.setSsoSessionMaxLifespan(12);
        rep.setAccessCodeLifespanLogin(1234);
        rep.setRegistrationAllowed(true);
        rep.setRegistrationEmailAsUsername(true);
        rep.setEditUsernameAllowed(true);

        realm.update(rep);

        rep = realm.toRepresentation();

        assertEquals(123, rep.getSsoSessionIdleTimeout().intValue());
        assertEquals(12, rep.getSsoSessionMaxLifespan().intValue());
        assertEquals(1234, rep.getAccessCodeLifespanLogin().intValue());
        assertEquals(Boolean.TRUE, rep.isRegistrationAllowed());
        assertEquals(Boolean.TRUE, rep.isRegistrationEmailAsUsername());
        assertEquals(Boolean.TRUE, rep.isEditUsernameAllowed());

        // second change
        rep.setRegistrationAllowed(false);
        rep.setRegistrationEmailAsUsername(false);
        rep.setEditUsernameAllowed(false);

        realm.update(rep);

        rep = realm.toRepresentation();
        assertEquals(Boolean.FALSE, rep.isRegistrationAllowed());
        assertEquals(Boolean.FALSE, rep.isRegistrationEmailAsUsername());
        assertEquals(Boolean.FALSE, rep.isEditUsernameAllowed());
    }

    @Test
    public void updateRealmWithNewRepresentation() {
        // first change
        RealmRepresentation rep = new RealmRepresentation();
        rep.setEditUsernameAllowed(true);
        rep.setSupportedLocales(new HashSet<>(Arrays.asList("en", "de")));

        realm.update(rep);

        rep = realm.toRepresentation();

        assertEquals(Boolean.TRUE, rep.isEditUsernameAllowed());
        assertEquals(2, rep.getSupportedLocales().size());

        // second change
        rep = new RealmRepresentation();
        rep.setEditUsernameAllowed(false);

        realm.update(rep);

        rep = realm.toRepresentation();
        assertEquals(Boolean.FALSE, rep.isEditUsernameAllowed());
        assertEquals(2, rep.getSupportedLocales().size());
    }

    @Test
    public void getRealmRepresentation() {
        RealmRepresentation rep = realm.toRepresentation();
        assertEquals(REALM_NAME, rep.getRealm());
        assertTrue(rep.isEnabled());

        assertNull(rep.getPrivateKey());
        assertNull(rep.getCodeSecret());
        assertNotNull(rep.getPublicKey());
        assertNotNull(rep.getCertificate());
    }

    @Test
    // KEYCLOAK-1110
    public void deleteDefaultRole() {
        RoleRepresentation role = new RoleRepresentation("test", "test", false);
        realm.roles().create(role);

        assertNotNull(realm.roles().get("test").toRepresentation());

        RealmRepresentation rep = realm.toRepresentation();
        rep.setDefaultRoles(new LinkedList<String>());
        rep.getDefaultRoles().add("test");

        realm.update(rep);

        realm.roles().deleteRole("test");

        try {
            realm.roles().get("testsadfsadf").toRepresentation();
            fail("Expected NotFoundException");
        } catch (NotFoundException e) {
        }
    }

    @Test
    public void convertKeycloakClientDescription() throws IOException {
        ClientRepresentation description = new ClientRepresentation();
        description.setClientId("client-id");
        description.setRedirectUris(Collections.singletonList("http://localhost"));

        ClientRepresentation converted = realm.convertClientDescription(JsonSerialization.writeValueAsString(description));
        assertEquals("client-id", converted.getClientId());
        assertEquals("http://localhost", converted.getRedirectUris().get(0));
    }

    @Test
    public void convertOIDCClientDescription() throws IOException {
        String description = IOUtils.toString(getClass().getResourceAsStream("/client-descriptions/client-oidc.json"));

        ClientRepresentation converted = realm.convertClientDescription(description);
        assertEquals(1, converted.getRedirectUris().size());
        assertEquals("http://localhost", converted.getRedirectUris().get(0));
    }

    @Test
    public void convertSAMLClientDescription() throws IOException {
        String description = IOUtils.toString(getClass().getResourceAsStream("/client-descriptions/saml-entity-descriptor.xml"));

        ClientRepresentation converted = realm.convertClientDescription(description);
        assertEquals("loadbalancer-9.siroe.com", converted.getClientId());
        assertEquals(1, converted.getRedirectUris().size());
        assertEquals("https://LoadBalancer-9.siroe.com:3443/federation/Consumer/metaAlias/sp", converted.getRedirectUris().get(0));
    }

    public static void assertRealm(RealmRepresentation realm, RealmRepresentation storedRealm) {
        if (realm.getId() != null) {
            Assert.assertEquals(realm.getId(), storedRealm.getId());
        }
        if (realm.getRealm() != null) {
            Assert.assertEquals(realm.getRealm(), storedRealm.getRealm());
        }
        if (realm.isEnabled() != null) Assert.assertEquals(realm.isEnabled(), storedRealm.isEnabled());
        if (realm.isBruteForceProtected() != null) Assert.assertEquals(realm.isBruteForceProtected(), storedRealm.isBruteForceProtected());
        if (realm.getMaxFailureWaitSeconds() != null) Assert.assertEquals(realm.getMaxFailureWaitSeconds(), storedRealm.getMaxFailureWaitSeconds());
        if (realm.getMinimumQuickLoginWaitSeconds() != null) Assert.assertEquals(realm.getMinimumQuickLoginWaitSeconds(), storedRealm.getMinimumQuickLoginWaitSeconds());
        if (realm.getWaitIncrementSeconds() != null) Assert.assertEquals(realm.getWaitIncrementSeconds(), storedRealm.getWaitIncrementSeconds());
        if (realm.getQuickLoginCheckMilliSeconds() != null) Assert.assertEquals(realm.getQuickLoginCheckMilliSeconds(), storedRealm.getQuickLoginCheckMilliSeconds());
        if (realm.getMaxDeltaTimeSeconds() != null) Assert.assertEquals(realm.getMaxDeltaTimeSeconds(), storedRealm.getMaxDeltaTimeSeconds());
        if (realm.getFailureFactor() != null) Assert.assertEquals(realm.getFailureFactor(), storedRealm.getFailureFactor());
        if (realm.isRegistrationAllowed() != null) Assert.assertEquals(realm.isRegistrationAllowed(), storedRealm.isRegistrationAllowed());
        if (realm.isRegistrationEmailAsUsername() != null) Assert.assertEquals(realm.isRegistrationEmailAsUsername(), storedRealm.isRegistrationEmailAsUsername());
        if (realm.isRememberMe() != null) Assert.assertEquals(realm.isRememberMe(), storedRealm.isRememberMe());
        if (realm.isVerifyEmail() != null) Assert.assertEquals(realm.isVerifyEmail(), storedRealm.isVerifyEmail());
        if (realm.isResetPasswordAllowed() != null) Assert.assertEquals(realm.isResetPasswordAllowed(), storedRealm.isResetPasswordAllowed());
        if (realm.isEditUsernameAllowed() != null) Assert.assertEquals(realm.isEditUsernameAllowed(), storedRealm.isEditUsernameAllowed());
        if (realm.getSslRequired() != null) Assert.assertEquals(realm.getSslRequired(), storedRealm.getSslRequired());
        if (realm.getAccessCodeLifespan() != null) Assert.assertEquals(realm.getAccessCodeLifespan(), storedRealm.getAccessCodeLifespan());
        if (realm.getAccessCodeLifespanUserAction() != null)
            Assert.assertEquals(realm.getAccessCodeLifespanUserAction(), storedRealm.getAccessCodeLifespanUserAction());
        if (realm.getNotBefore() != null) Assert.assertEquals(realm.getNotBefore(), storedRealm.getNotBefore());
        if (realm.getAccessTokenLifespan() != null) Assert.assertEquals(realm.getAccessTokenLifespan(), storedRealm.getAccessTokenLifespan());
        if (realm.getAccessTokenLifespanForImplicitFlow() != null) Assert.assertEquals(realm.getAccessTokenLifespanForImplicitFlow(), storedRealm.getAccessTokenLifespanForImplicitFlow());
        if (realm.getSsoSessionIdleTimeout() != null) Assert.assertEquals(realm.getSsoSessionIdleTimeout(), storedRealm.getSsoSessionIdleTimeout());
        if (realm.getSsoSessionMaxLifespan() != null) Assert.assertEquals(realm.getSsoSessionMaxLifespan(), storedRealm.getSsoSessionMaxLifespan());
        if (realm.getRequiredCredentials() != null) {
            Assert.assertNotNull(storedRealm.getRequiredCredentials());
            for (String cred : realm.getRequiredCredentials()) {
                Assert.assertTrue(storedRealm.getRequiredCredentials().contains(cred));
            }
        }
        if (realm.getLoginTheme() != null) Assert.assertEquals(realm.getLoginTheme(), storedRealm.getLoginTheme());
        if (realm.getAccountTheme() != null) Assert.assertEquals(realm.getAccountTheme(), storedRealm.getAccountTheme());
        if (realm.getAdminTheme() != null) Assert.assertEquals(realm.getAdminTheme(), storedRealm.getAdminTheme());
        if (realm.getEmailTheme() != null) Assert.assertEquals(realm.getEmailTheme(), storedRealm.getEmailTheme());

        if (realm.getPasswordPolicy() != null) Assert.assertEquals(realm.getPasswordPolicy(), storedRealm.getPasswordPolicy());

        if (realm.getDefaultRoles() != null) {
            Assert.assertNotNull(storedRealm.getDefaultRoles());
            for (String role : realm.getDefaultRoles()) {
                Assert.assertTrue(storedRealm.getDefaultRoles().contains(role));
            }
        }

        if (realm.getSmtpServer() != null) {
            Assert.assertEquals(realm.getSmtpServer(), storedRealm.getSmtpServer());
        }

        if (realm.getBrowserSecurityHeaders() != null) {
            Assert.assertEquals(realm.getBrowserSecurityHeaders(), storedRealm.getBrowserSecurityHeaders());
        }

    }

}

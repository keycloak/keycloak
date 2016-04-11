/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
 */

package org.keycloak.testsuite.admin;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ServerInfoResource;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.KeycloakServer;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
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

    public static final String PRIVATE_KEY = "MIICXAIBAAKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQABAoGAfmO8gVhyBxdqlxmIuglbz8bcjQbhXJLR2EoS8ngTXmN1bo2L90M0mUKSdc7qF10LgETBzqL8jYlQIbt+e6TH8fcEpKCjUlyq0Mf/vVbfZSNaVycY13nTzo27iPyWQHK5NLuJzn1xvxxrUeXI6A2WFpGEBLbHjwpx5WQG9A+2scECQQDvdn9NE75HPTVPxBqsEd2z10TKkl9CZxu10Qby3iQQmWLEJ9LNmy3acvKrE3gMiYNWb6xHPKiIqOR1as7L24aTAkEAtyvQOlCvr5kAjVqrEKXalj0Tzewjweuxc0pskvArTI2Oo070h65GpoIKLc9jf+UA69cRtquwP93aZKtW06U8dQJAF2Y44ks/mK5+eyDqik3koCI08qaC8HYq2wVl7G2QkJ6sbAaILtcvD92ToOvyGyeE0flvmDZxMYlvaZnaQ0lcSQJBAKZU6umJi3/xeEbkJqMfeLclD27XGEFoPeNrmdx0q10Azp4NfJAY+Z8KRyQCR2BEG+oNitBOZ+YXF9KCpH3cdmECQHEigJhYg+ykOvr1aiZUMFT72HU0jnmQe2FVekuG+LJUt2Tm7GtMjTFoGpf0JwrVuZN39fOYAlo+nTixgeW7X8Y=";
    public static final String PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQAB";
    public static final String CERTIFICATE = "MIICsTCCAZkCBgFTLB5bhDANBgkqhkiG9w0BAQsFADAcMRowGAYDVQQDDBFhZG1pbi1jbGllbnQtdGVzdDAeFw0xNjAyMjkwODIwMDBaFw0yNjAyMjgwODIxNDBaMBwxGjAYBgNVBAMMEWFkbWluLWNsaWVudC10ZXN0MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAquzJtpAlpTFnJzILjTOHW+SOWav1eIsCtlAqiFTvBskbod6b4BtVaR3FVrQm8rFiwDOIEWT3IG3ZIz0LKYxnqvuffyLHGHjiroqrR63kY9Wa9B790lSEWVaGeNOMnKleqKu5QUNfL3wVebUh/C/QfxZ29R1EIbxNe2ThN8yuIca8Ltn43D5VlyatptojffxpCYiYqAmIwQDaq1um2cQ+4rPBLxC5jM9UBvYOMUP4u0caNSaPI1o9lHVKgTtWcdQzUeMmAGsnLV26XGhA/OwRduUxksumR1kh/KSqowasjgSrpVqtF/uo5TY57s7drD+zKG58cdHLreclB9AQNvNwZwIDAQABMA0GCSqGSIb3DQEBCwUAA4IBAQBh4iwg8GnadeQP52pV5vKJ4Z8A1R2aYCzoW7Lc3FI/pXWX9Af5dKILX5O2j/daamPS+WtDWxIuwvZC5drrkvJn/r8e4KstnXQzPQggIJbI9v3wfIX3VlFvwvZVGiuE5PSLSWb0L57PEojZVpIU5bLchq4yRSD2zK4dWX8Y6I/D40a74KDvPOlEL8405/T1iW7ytKT9awNJW04N91owoI+kdUL+DMnnGzIxDAoYAeZI/1vcwoaH24zyTLGItkzpKxqLOdB05cnxn5jCWY2Hyd1zqtRkadhgZaqu4lcDHAHEMDp6dEjLZW8ym8bnlto+MD2y//CsyPCzyCLlA726vrli";

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
    public void renameRealm() {
        RealmRepresentation rep = new RealmRepresentation();
        rep.setId("old");
        rep.setRealm("old");

        try {
            keycloak.realms().create(rep);

            rep.setRealm("new");
            keycloak.realm("old").update(rep);

            // Check client in master realm renamed
            assertEquals(0, keycloak.realm("master").clients().findByClientId("old-realm").size());
            assertEquals(1, keycloak.realm("master").clients().findByClientId("new-realm").size());

            ClientRepresentation adminClient = keycloak.realm("new").clients().findByClientId(Constants.ADMIN_CONSOLE_CLIENT_ID).get(0);
            assertEquals("/auth/admin/new/console/index.html", adminClient.getBaseUrl());
            assertEquals("/auth/admin/new/console/*", adminClient.getRedirectUris().get(0));

            ClientRepresentation accountClient = keycloak.realm("new").clients().findByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID).get(0);
            assertEquals("/auth/realms/new/account", accountClient.getBaseUrl());
            assertEquals("/auth/realms/new/account/*", accountClient.getRedirectUris().get(0));
        } finally {
            keycloak.realms().realm(rep.getRealm()).remove();
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
    public void loginAfterRemoveRealm() {
        realm.remove();

        ServerInfoResource serverInfoResource = Keycloak.getInstance("http://localhost:8081/auth", "master", "admin", "admin", Constants.ADMIN_CLI_CLIENT_ID).serverInfo();
        serverInfoResource.getInfo();
    }

    /**
     * KEYCLOAK-1990 1991
     * @throws Exception
     */
    @Test
    public void renameRealmTest() throws Exception {
        Keycloak keycloak = Keycloak.getInstance("http://localhost:8081/auth", "master", "admin", "admin", Constants.ADMIN_CLI_CLIENT_ID);
        RealmRepresentation realm1 = new RealmRepresentation();
        realm1.setRealm("test-immutable");
        keycloak.realms().create(realm1);
        realm1 = keycloak.realms().realm("test-immutable").toRepresentation();
        realm1.setRealm("test-immutable-old");
        keycloak.realms().realm("test-immutable").update(realm1);
        realm1 = keycloak.realms().realm("test-immutable-old").toRepresentation();

        RealmRepresentation realm2 = new RealmRepresentation();
        realm2.setRealm("test-immutable");
        keycloak.realms().create(realm2);
        realm2 = keycloak.realms().realm("test-immutable").toRepresentation();

        keycloak.realms().realm("test-immutable-old").remove();
        keycloak.realms().realm("test-immutable").remove();




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

    @Test
    public void uploadRealmKeys() throws Exception {
        String originalPublicKey = realm.toRepresentation().getPublicKey();

        RealmRepresentation rep = new RealmRepresentation();
        rep.setPrivateKey("INVALID");
        rep.setPublicKey(PUBLIC_KEY);

        try {
            realm.update(rep);
            fail("Expected BadRequestException");
        } catch (BadRequestException e) {
        }

        rep.setPrivateKey(PRIVATE_KEY);
        rep.setPublicKey("INVALID");

        try {
            realm.update(rep);
            fail("Expected BadRequestException");
        } catch (BadRequestException e) {
        }

        assertEquals(originalPublicKey, realm.toRepresentation().getPublicKey());

        rep.setPublicKey(PUBLIC_KEY);
        realm.update(rep);

        assertEquals(PUBLIC_KEY, rep.getPublicKey());

        String privateKey2048 = IOUtils.toString(getClass().getResourceAsStream("/keys/private2048.pem"));
        String publicKey2048 = IOUtils.toString(getClass().getResourceAsStream("/keys/public2048.pem"));

        rep.setPrivateKey(privateKey2048);

        try {
            realm.update(rep);
            fail("Expected BadRequestException");
        } catch (BadRequestException e) {
        }

        assertEquals(PUBLIC_KEY, realm.toRepresentation().getPublicKey());

        rep.setPublicKey(publicKey2048);

        realm.update(rep);

        assertEquals(publicKey2048, realm.toRepresentation().getPublicKey());

        String privateKey4096 = IOUtils.toString(getClass().getResourceAsStream("/keys/private4096.pem"));
        String publicKey4096 = IOUtils.toString(getClass().getResourceAsStream("/keys/public4096.pem"));
        rep.setPrivateKey(privateKey4096);
        rep.setPublicKey(publicKey4096);

        realm.update(rep);

        assertEquals(publicKey4096, realm.toRepresentation().getPublicKey());
    }

    @Test
    public void uploadCertificate() throws IOException {
        RealmRepresentation rep = new RealmRepresentation();
        rep.setCertificate(CERTIFICATE);

        realm.update(rep);

        assertEquals(CERTIFICATE, rep.getCertificate());

        String certificate = IOUtils.toString(getClass().getResourceAsStream("/keys/certificate.pem"));
        rep.setCertificate(certificate);

        realm.update(rep);

        assertEquals(certificate, rep.getCertificate());
    }

    @Test
    // KEYCLOAK-2700
    public void deleteRealmWithDefaultGroups() throws IOException {
        RealmRepresentation rep = new RealmRepresentation();
        rep.setRealm("foo");

        GroupRepresentation group = new GroupRepresentation();
        group.setName("default1");
        group.setPath("/default1");

        rep.setGroups(Collections.singletonList(group));
        rep.setDefaultGroups(Collections.singletonList("/default1"));

        keycloak.realms().create(rep);

        keycloak.realm(rep.getRealm()).remove();
    }

}

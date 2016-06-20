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

package org.keycloak.testsuite.admin.realm;

import org.apache.commons.io.IOUtils;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ServerInfoResource;
import org.keycloak.common.util.StreamUtil;
import org.keycloak.common.util.Time;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.Constants;
import org.keycloak.representations.adapters.action.GlobalRequestResult;
import org.keycloak.representations.adapters.action.PushNotBeforeAction;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.AbstractAdminTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.auth.page.AuthRealm;
import org.keycloak.testsuite.util.AdminEventPaths;
import org.keycloak.testsuite.util.CredentialBuilder;
import org.keycloak.testsuite.util.OAuthClient.AccessTokenResponse;
import org.keycloak.testsuite.util.UserBuilder;
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
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RealmTest extends AbstractAdminTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    public static final String PRIVATE_KEY = "MIICXAIBAAKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQABAoGAfmO8gVhyBxdqlxmIuglbz8bcjQbhXJLR2EoS8ngTXmN1bo2L90M0mUKSdc7qF10LgETBzqL8jYlQIbt+e6TH8fcEpKCjUlyq0Mf/vVbfZSNaVycY13nTzo27iPyWQHK5NLuJzn1xvxxrUeXI6A2WFpGEBLbHjwpx5WQG9A+2scECQQDvdn9NE75HPTVPxBqsEd2z10TKkl9CZxu10Qby3iQQmWLEJ9LNmy3acvKrE3gMiYNWb6xHPKiIqOR1as7L24aTAkEAtyvQOlCvr5kAjVqrEKXalj0Tzewjweuxc0pskvArTI2Oo070h65GpoIKLc9jf+UA69cRtquwP93aZKtW06U8dQJAF2Y44ks/mK5+eyDqik3koCI08qaC8HYq2wVl7G2QkJ6sbAaILtcvD92ToOvyGyeE0flvmDZxMYlvaZnaQ0lcSQJBAKZU6umJi3/xeEbkJqMfeLclD27XGEFoPeNrmdx0q10Azp4NfJAY+Z8KRyQCR2BEG+oNitBOZ+YXF9KCpH3cdmECQHEigJhYg+ykOvr1aiZUMFT72HU0jnmQe2FVekuG+LJUt2Tm7GtMjTFoGpf0JwrVuZN39fOYAlo+nTixgeW7X8Y=";
    public static final String PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQAB";
    public static final String CERTIFICATE = "MIICsTCCAZkCBgFTLB5bhDANBgkqhkiG9w0BAQsFADAcMRowGAYDVQQDDBFhZG1pbi1jbGllbnQtdGVzdDAeFw0xNjAyMjkwODIwMDBaFw0yNjAyMjgwODIxNDBaMBwxGjAYBgNVBAMMEWFkbWluLWNsaWVudC10ZXN0MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAquzJtpAlpTFnJzILjTOHW+SOWav1eIsCtlAqiFTvBskbod6b4BtVaR3FVrQm8rFiwDOIEWT3IG3ZIz0LKYxnqvuffyLHGHjiroqrR63kY9Wa9B790lSEWVaGeNOMnKleqKu5QUNfL3wVebUh/C/QfxZ29R1EIbxNe2ThN8yuIca8Ltn43D5VlyatptojffxpCYiYqAmIwQDaq1um2cQ+4rPBLxC5jM9UBvYOMUP4u0caNSaPI1o9lHVKgTtWcdQzUeMmAGsnLV26XGhA/OwRduUxksumR1kh/KSqowasjgSrpVqtF/uo5TY57s7drD+zKG58cdHLreclB9AQNvNwZwIDAQABMA0GCSqGSIb3DQEBCwUAA4IBAQBh4iwg8GnadeQP52pV5vKJ4Z8A1R2aYCzoW7Lc3FI/pXWX9Af5dKILX5O2j/daamPS+WtDWxIuwvZC5drrkvJn/r8e4KstnXQzPQggIJbI9v3wfIX3VlFvwvZVGiuE5PSLSWb0L57PEojZVpIU5bLchq4yRSD2zK4dWX8Y6I/D40a74KDvPOlEL8405/T1iW7ytKT9awNJW04N91owoI+kdUL+DMnnGzIxDAoYAeZI/1vcwoaH24zyTLGItkzpKxqLOdB05cnxn5jCWY2Hyd1zqtRkadhgZaqu4lcDHAHEMDp6dEjLZW8ym8bnlto+MD2y//CsyPCzyCLlA726vrli";

    @Test
    public void getRealms() {
        List<RealmRepresentation> realms = adminClient.realms().findAll();
        Assert.assertNames(realms, "master", AuthRealm.TEST, REALM_NAME);

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
            adminClient.realms().create(rep);

            rep.setRealm("new");
            adminClient.realm("old").update(rep);

            // Check client in master realm renamed
            Assert.assertEquals(0, adminClient.realm("master").clients().findByClientId("old-realm").size());
            Assert.assertEquals(1, adminClient.realm("master").clients().findByClientId("new-realm").size());

            ClientRepresentation adminConsoleClient = adminClient.realm("new").clients().findByClientId(Constants.ADMIN_CONSOLE_CLIENT_ID).get(0);
            assertEquals("/auth/admin/new/console/index.html", adminConsoleClient.getBaseUrl());
            assertEquals("/auth/admin/new/console/*", adminConsoleClient.getRedirectUris().get(0));

            ClientRepresentation accountClient = adminClient.realm("new").clients().findByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID).get(0);
            assertEquals("/auth/realms/new/account", accountClient.getBaseUrl());
            assertEquals("/auth/realms/new/account/*", accountClient.getRedirectUris().get(0));
        } finally {
            adminClient.realms().realm(rep.getRealm()).remove();
        }
    }

    @Test
    public void createRealmEmpty() {
        RealmRepresentation rep = new RealmRepresentation();
        rep.setRealm("new-realm");

        adminClient.realms().create(rep);

        Assert.assertNames(adminClient.realms().findAll(), "master", AuthRealm.TEST, REALM_NAME, "new-realm");

        adminClient.realms().realm("new-realm").remove();

        Assert.assertNames(adminClient.realms().findAll(), "master", AuthRealm.TEST, REALM_NAME);
    }

    @Test
    public void createRealmCheckDefaultPasswordPolicy() {
        RealmRepresentation rep = new RealmRepresentation();
        rep.setRealm("new-realm");

        adminClient.realms().create(rep);

        assertEquals("hashIterations(20000)", adminClient.realm("new-realm").toRepresentation().getPasswordPolicy());

        adminClient.realms().realm("new-realm").remove();

        rep.setPasswordPolicy("length(8)");

        adminClient.realms().create(rep);

        assertEquals("length(8)", adminClient.realm("new-realm").toRepresentation().getPasswordPolicy());

        adminClient.realms().realm("new-realm").remove();
    }

    @Test
    public void createRealmFromJson() {
        RealmRepresentation rep = loadJson(getClass().getResourceAsStream("/admin-test/testrealm.json"), RealmRepresentation.class);
        adminClient.realms().create(rep);

        RealmRepresentation created = adminClient.realms().realm("admin-test-1").toRepresentation();
        assertRealm(rep, created);

        adminClient.realms().realm("admin-test-1").remove();
    }

    @Test
    public void removeRealm() {
        realm.remove();

        Assert.assertNames(adminClient.realms().findAll(), "master", AuthRealm.TEST);
    }

    @Test
    public void loginAfterRemoveRealm() {
        realm.remove();

        ServerInfoResource serverInfoResource = Keycloak.getInstance(AuthServerTestEnricher.getAuthServerContextRoot() + "/auth", "master", "admin", "admin", Constants.ADMIN_CLI_CLIENT_ID).serverInfo();
        serverInfoResource.getInfo();
    }

    /**
     * KEYCLOAK-1990 1991
     * @throws Exception
     */
    @Test
    public void renameRealmTest() throws Exception {
        RealmRepresentation realm1 = new RealmRepresentation();
        realm1.setRealm("test-immutable");
        adminClient.realms().create(realm1);
        realm1 = adminClient.realms().realm("test-immutable").toRepresentation();
        realm1.setRealm("test-immutable-old");
        adminClient.realms().realm("test-immutable").update(realm1);
        realm1 = adminClient.realms().realm("test-immutable-old").toRepresentation();

        RealmRepresentation realm2 = new RealmRepresentation();
        realm2.setRealm("test-immutable");
        adminClient.realms().create(realm2);
        realm2 = adminClient.realms().realm("test-immutable").toRepresentation();

        adminClient.realms().realm("test-immutable-old").remove();
        adminClient.realms().realm("test-immutable").remove();
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
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, Matchers.nullValue(String.class), rep, ResourceType.REALM);

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
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, Matchers.nullValue(String.class), rep, ResourceType.REALM);

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
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, Matchers.nullValue(String.class), rep, ResourceType.REALM);

        rep = realm.toRepresentation();

        assertEquals(Boolean.TRUE, rep.isEditUsernameAllowed());
        assertEquals(2, rep.getSupportedLocales().size());

        // second change
        rep = new RealmRepresentation();
        rep.setEditUsernameAllowed(false);

        realm.update(rep);
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, Matchers.nullValue(String.class), rep, ResourceType.REALM);

        rep = realm.toRepresentation();
        assertEquals(Boolean.FALSE, rep.isEditUsernameAllowed());
        assertEquals(2, rep.getSupportedLocales().size());
    }

    @Test
    public void getRealmRepresentation() {
        RealmRepresentation rep = realm.toRepresentation();
        Assert.assertEquals(REALM_NAME, rep.getRealm());
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
        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.roleResourcePath("test"), role, ResourceType.REALM_ROLE);

        assertNotNull(realm.roles().get("test").toRepresentation());

        RealmRepresentation rep = realm.toRepresentation();
        rep.setDefaultRoles(new LinkedList<String>());
        rep.getDefaultRoles().add("test");

        realm.update(rep);
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, Matchers.nullValue(String.class), rep, ResourceType.REALM);

        realm.roles().deleteRole("test");
        assertAdminEvents.assertEvent(realmId, OperationType.DELETE, AdminEventPaths.roleResourcePath("test"), ResourceType.REALM_ROLE);

        try {
            realm.roles().get("testsadfsadf").toRepresentation();
            fail("Expected NotFoundException");
        } catch (NotFoundException e) {
            // Expected
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
            assertEquals(realm.getId(), storedRealm.getId());
        }
        if (realm.getRealm() != null) {
            assertEquals(realm.getRealm(), storedRealm.getRealm());
        }
        if (realm.isEnabled() != null) assertEquals(realm.isEnabled(), storedRealm.isEnabled());
        if (realm.isBruteForceProtected() != null) assertEquals(realm.isBruteForceProtected(), storedRealm.isBruteForceProtected());
        if (realm.getMaxFailureWaitSeconds() != null) assertEquals(realm.getMaxFailureWaitSeconds(), storedRealm.getMaxFailureWaitSeconds());
        if (realm.getMinimumQuickLoginWaitSeconds() != null) assertEquals(realm.getMinimumQuickLoginWaitSeconds(), storedRealm.getMinimumQuickLoginWaitSeconds());
        if (realm.getWaitIncrementSeconds() != null) assertEquals(realm.getWaitIncrementSeconds(), storedRealm.getWaitIncrementSeconds());
        if (realm.getQuickLoginCheckMilliSeconds() != null) assertEquals(realm.getQuickLoginCheckMilliSeconds(), storedRealm.getQuickLoginCheckMilliSeconds());
        if (realm.getMaxDeltaTimeSeconds() != null) assertEquals(realm.getMaxDeltaTimeSeconds(), storedRealm.getMaxDeltaTimeSeconds());
        if (realm.getFailureFactor() != null) assertEquals(realm.getFailureFactor(), storedRealm.getFailureFactor());
        if (realm.isRegistrationAllowed() != null) assertEquals(realm.isRegistrationAllowed(), storedRealm.isRegistrationAllowed());
        if (realm.isRegistrationEmailAsUsername() != null) assertEquals(realm.isRegistrationEmailAsUsername(), storedRealm.isRegistrationEmailAsUsername());
        if (realm.isRememberMe() != null) assertEquals(realm.isRememberMe(), storedRealm.isRememberMe());
        if (realm.isVerifyEmail() != null) assertEquals(realm.isVerifyEmail(), storedRealm.isVerifyEmail());
        if (realm.isResetPasswordAllowed() != null) assertEquals(realm.isResetPasswordAllowed(), storedRealm.isResetPasswordAllowed());
        if (realm.isEditUsernameAllowed() != null) assertEquals(realm.isEditUsernameAllowed(), storedRealm.isEditUsernameAllowed());
        if (realm.getSslRequired() != null) assertEquals(realm.getSslRequired(), storedRealm.getSslRequired());
        if (realm.getAccessCodeLifespan() != null) assertEquals(realm.getAccessCodeLifespan(), storedRealm.getAccessCodeLifespan());
        if (realm.getAccessCodeLifespanUserAction() != null)
            assertEquals(realm.getAccessCodeLifespanUserAction(), storedRealm.getAccessCodeLifespanUserAction());
        if (realm.getNotBefore() != null) assertEquals(realm.getNotBefore(), storedRealm.getNotBefore());
        if (realm.getAccessTokenLifespan() != null) assertEquals(realm.getAccessTokenLifespan(), storedRealm.getAccessTokenLifespan());
        if (realm.getAccessTokenLifespanForImplicitFlow() != null) assertEquals(realm.getAccessTokenLifespanForImplicitFlow(), storedRealm.getAccessTokenLifespanForImplicitFlow());
        if (realm.getSsoSessionIdleTimeout() != null) assertEquals(realm.getSsoSessionIdleTimeout(), storedRealm.getSsoSessionIdleTimeout());
        if (realm.getSsoSessionMaxLifespan() != null) assertEquals(realm.getSsoSessionMaxLifespan(), storedRealm.getSsoSessionMaxLifespan());
        if (realm.getRequiredCredentials() != null) {
            assertNotNull(storedRealm.getRequiredCredentials());
            for (String cred : realm.getRequiredCredentials()) {
                assertTrue(storedRealm.getRequiredCredentials().contains(cred));
            }
        }
        if (realm.getLoginTheme() != null) assertEquals(realm.getLoginTheme(), storedRealm.getLoginTheme());
        if (realm.getAccountTheme() != null) assertEquals(realm.getAccountTheme(), storedRealm.getAccountTheme());
        if (realm.getAdminTheme() != null) assertEquals(realm.getAdminTheme(), storedRealm.getAdminTheme());
        if (realm.getEmailTheme() != null) assertEquals(realm.getEmailTheme(), storedRealm.getEmailTheme());

        if (realm.getPasswordPolicy() != null) assertEquals(realm.getPasswordPolicy(), storedRealm.getPasswordPolicy());

        if (realm.getDefaultRoles() != null) {
            assertNotNull(storedRealm.getDefaultRoles());
            for (String role : realm.getDefaultRoles()) {
                assertTrue(storedRealm.getDefaultRoles().contains(role));
            }
        }

        if (realm.getSmtpServer() != null) {
            assertEquals(realm.getSmtpServer(), storedRealm.getSmtpServer());
        }

        if (realm.getBrowserSecurityHeaders() != null) {
            assertEquals(realm.getBrowserSecurityHeaders(), storedRealm.getBrowserSecurityHeaders());
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
            // Expected
            assertAdminEvents.assertEmpty();
        }

        rep.setPrivateKey(PRIVATE_KEY);
        rep.setPublicKey("INVALID");

        try {
            realm.update(rep);
            fail("Expected BadRequestException");
        } catch (BadRequestException e) {
            // Expected
            assertAdminEvents.assertEmpty();
        }

        Assert.assertEquals(originalPublicKey, realm.toRepresentation().getPublicKey());

        rep.setPublicKey(PUBLIC_KEY);
        realm.update(rep);
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, Matchers.nullValue(String.class), rep, ResourceType.REALM);

        assertEquals(PUBLIC_KEY, rep.getPublicKey());

        String privateKey2048 = StreamUtil.readString(getClass().getResourceAsStream("/keys/private2048.pem"));
        String publicKey2048 = StreamUtil.readString(getClass().getResourceAsStream("/keys/public2048.pem"));

        rep.setPrivateKey(privateKey2048);

        try {
            realm.update(rep);
            fail("Expected BadRequestException");
        } catch (BadRequestException e) {
            // Expected
            assertAdminEvents.assertEmpty();
        }

        Assert.assertEquals(PUBLIC_KEY, realm.toRepresentation().getPublicKey());

        rep.setPrivateKey("{}{}{}{}{}{}324re9gvj0r");
        rep.setPublicKey("{}{}{}{}{}{}324re9gvj0r");
        try {
            realm.update(rep);
            fail("Expected BadRequestException");
        } catch (BadRequestException e) {
            // Expected
            assertAdminEvents.assertEmpty();
        }

        Assert.assertEquals(PUBLIC_KEY, realm.toRepresentation().getPublicKey());

        rep.setPrivateKey(privateKey2048);
        rep.setPublicKey(publicKey2048);

        realm.update(rep);
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, Matchers.nullValue(String.class), rep, ResourceType.REALM);

        Assert.assertEquals(publicKey2048, realm.toRepresentation().getPublicKey());

        String privateKey4096 = StreamUtil.readString(getClass().getResourceAsStream("/keys/private4096.pem"));
        String publicKey4096 = StreamUtil.readString(getClass().getResourceAsStream("/keys/public4096.pem"));
        rep.setPrivateKey(privateKey4096);
        rep.setPublicKey(publicKey4096);

        realm.update(rep);
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, Matchers.nullValue(String.class), rep, ResourceType.REALM);

        Assert.assertEquals(publicKey4096, realm.toRepresentation().getPublicKey());
    }

    @Test
    public void uploadCertificate() throws IOException {
        RealmRepresentation rep = new RealmRepresentation();
        rep.setCertificate(CERTIFICATE);

        realm.update(rep);
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, Matchers.nullValue(String.class), rep, ResourceType.REALM);

        assertEquals(CERTIFICATE, rep.getCertificate());

        String certificate = IOUtils.toString(getClass().getResourceAsStream("/keys/certificate.pem"));
        rep.setCertificate(certificate);

        realm.update(rep);
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, Matchers.nullValue(String.class), rep, ResourceType.REALM);

        assertEquals(certificate, realm.toRepresentation().getCertificate());

        rep.setCertificate("{}{}{}{}{}{}324re9gvj0r");
        try {
            realm.update(rep);
            fail("Expected BadRequestException");
        } catch (BadRequestException e) {
            // Expected
            assertAdminEvents.assertEmpty();
        }

        rep.setCertificate("invalid");
        try {
            realm.update(rep);
            fail("Expected BadRequestException");
        } catch (BadRequestException e) {
            // Expected
            assertAdminEvents.assertEmpty();
        }

        assertEquals(certificate, realm.toRepresentation().getCertificate());
    }

    @Test
    public void clearRealmCache() {
        RealmRepresentation realmRep = realm.toRepresentation();
        assertTrue(testingClient.testing().isCached("realms", realmRep.getId()));

        realm.clearRealmCache();
        assertAdminEvents.assertEvent(realmId, OperationType.ACTION, "clear-realm-cache", ResourceType.REALM);

        assertFalse(testingClient.testing().isCached("realms", realmRep.getId()));
    }

    @Test
    public void clearUserCache() {
        UserRepresentation user = new UserRepresentation();
        user.setUsername("clearcacheuser");
        Response response = realm.users().create(user);
        String userId = ApiUtil.getCreatedId(response);
        response.close();
        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.userResourcePath(userId), user, ResourceType.USER);

        realm.users().get(userId).toRepresentation();

        assertTrue(testingClient.testing().isCached("users", userId));

        realm.clearUserCache();
        assertAdminEvents.assertEvent(realmId, OperationType.ACTION, "clear-user-cache", ResourceType.REALM);

        assertFalse(testingClient.testing().isCached("users", userId));
    }

    @Test
    public void pushNotBefore() {
        setupTestAppAndUser();

        int time = Time.currentTime() - 60;

        RealmRepresentation rep = realm.toRepresentation();
        rep.setNotBefore(time);
        realm.update(rep);
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, Matchers.nullValue(String.class), rep, ResourceType.REALM);

        GlobalRequestResult globalRequestResult = realm.pushRevocation();
        assertAdminEvents.assertEvent(realmId, OperationType.ACTION, "push-revocation", globalRequestResult, ResourceType.REALM);

        assertEquals(1, globalRequestResult.getSuccessRequests().size());
        assertEquals("http://localhost:8180/auth/realms/master/app/admin", globalRequestResult.getSuccessRequests().get(0));
        assertNull(globalRequestResult.getFailedRequests());

        PushNotBeforeAction adminPushNotBefore = testingClient.testApp().getAdminPushNotBefore();
        assertEquals(time, adminPushNotBefore.getNotBefore());
    }

    @Test
    public void logoutAll() {
        setupTestAppAndUser();

        Response response = realm.users().create(UserBuilder.create().username("user").build());
        String userId = ApiUtil.getCreatedId(response);
        response.close();
        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.userResourcePath(userId), ResourceType.USER);

        realm.users().get(userId).resetPassword(CredentialBuilder.create().password("password").build());
        assertAdminEvents.assertEvent(realmId, OperationType.ACTION, AdminEventPaths.userResetPasswordPath(userId), ResourceType.USER);

        oauth.doLogin("user", "password");

        GlobalRequestResult globalRequestResult = realm.logoutAll();
        assertAdminEvents.assertEvent(realmId, OperationType.ACTION, "logout-all", globalRequestResult, ResourceType.REALM);

        assertEquals(1, globalRequestResult.getSuccessRequests().size());
        assertEquals("http://localhost:8180/auth/realms/master/app/admin", globalRequestResult.getSuccessRequests().get(0));
        assertNull(globalRequestResult.getFailedRequests());

        assertNotNull(testingClient.testApp().getAdminLogoutAction());
    }

    @Test
    public void deleteSession() {
        setupTestAppAndUser();

        oauth.doLogin("testuser", "password");
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.getCurrentQuery().get(OAuth2Constants.CODE), "secret");
        assertEquals(200, tokenResponse.getStatusCode());

        EventRepresentation event = events.poll();
        assertNotNull(event);

        realm.deleteSession(event.getSessionId());
        assertAdminEvents.assertEvent(realmId, OperationType.DELETE, AdminEventPaths.deleteSessionPath(event.getSessionId()), ResourceType.USER_SESSION);
        try {
            realm.deleteSession(event.getSessionId());
            fail("Expected 404");
        } catch (NotFoundException e) {
            // Expected
            assertAdminEvents.assertEmpty();
        }

        tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken(), "secret");
        assertEquals(400, tokenResponse.getStatusCode());
        assertEquals("Session not active", tokenResponse.getErrorDescription());
    }

    @Test
    public void clientSessionStats() {
        setupTestAppAndUser();

        List<Map<String, String>> sessionStats = realm.getClientSessionStats();
        assertTrue(sessionStats.isEmpty());

        System.out.println(sessionStats.size());

        oauth.doLogin("testuser", "password");
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.getCurrentQuery().get(OAuth2Constants.CODE), "secret");
        assertEquals(200, tokenResponse.getStatusCode());

        sessionStats = realm.getClientSessionStats();

        assertEquals(1, sessionStats.size());
        assertEquals("test-app", sessionStats.get(0).get("clientId"));
        assertEquals("1", sessionStats.get(0).get("active"));
    }

    private void setupTestAppAndUser() {
        testingClient.testApp().clearAdminActions();

        String redirectUri = oauth.getRedirectUri().replace("/master/", "/" + REALM_NAME + "/");

        ClientRepresentation client = new ClientRepresentation();
        client.setClientId("test-app");
        client.setAdminUrl(suiteContext.getAuthServerInfo().getContextRoot() + "/auth/realms/master/app/admin");
        client.setRedirectUris(Collections.singletonList(redirectUri));
        client.setSecret("secret");
        Response resp = realm.clients().create(client);
        String clientDbId = ApiUtil.getCreatedId(resp);
        resp.close();
        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.clientResourcePath(clientDbId), client, ResourceType.CLIENT);

        oauth.realm(REALM_NAME);
        oauth.redirectUri(redirectUri);

        UserRepresentation userRep = UserBuilder.create().username("testuser").build();
        Response response = realm.users().create(userRep);
        String userId = ApiUtil.getCreatedId(response);
        response.close();
        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.userResourcePath(userId), userRep, ResourceType.USER);

        realm.users().get(userId).resetPassword(CredentialBuilder.create().password("password").build());
        assertAdminEvents.assertEvent(realmId, OperationType.ACTION, AdminEventPaths.userResetPasswordPath(userId), ResourceType.USER);

        testingClient.testApp().clearAdminActions();
    }

}

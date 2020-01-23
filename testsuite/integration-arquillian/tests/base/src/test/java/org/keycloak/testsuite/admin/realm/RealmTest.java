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
import org.junit.rules.ExpectedException;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.util.Time;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.events.log.JBossLoggingEventListenerProviderFactory;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.adapters.action.GlobalRequestResult;
import org.keycloak.representations.adapters.action.PushNotBeforeAction;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.AbstractAdminTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.auth.page.AuthRealm;
import org.keycloak.testsuite.client.KeycloakTestingClient;
import org.keycloak.testsuite.events.EventsListenerProviderFactory;
import org.keycloak.testsuite.runonserver.RunHelpers;
import org.keycloak.testsuite.updaters.Creator;
import org.keycloak.testsuite.util.AdminEventPaths;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.CredentialBuilder;
import org.keycloak.testsuite.util.OAuthClient.AccessTokenResponse;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.utils.tls.TLSUtils;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
public class RealmTest extends AbstractAdminTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);
    @Rule
    public ExpectedException expectedException = ExpectedException.none();


    // Remove all realms before first run
    @Override
    public void beforeAbstractKeycloakTestRealmImport() {
        if (testContext.isInitialized()) {
            return;
        }

        removeAllRealmsDespiteMaster();

        testContext.setInitialized(true);
    }


    @Test
    public void getRealms() {
        List<RealmRepresentation> realms = adminClient.realms().findAll();
        Assert.assertNames(realms, "master", AuthRealm.TEST, REALM_NAME);
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
            assertEquals(Constants.AUTH_ADMIN_URL_PROP, adminConsoleClient.getRootUrl());
            assertEquals("/admin/new/console/", adminConsoleClient.getBaseUrl());
            assertEquals("/admin/new/console/*", adminConsoleClient.getRedirectUris().get(0));

            ClientRepresentation accountClient = adminClient.realm("new").clients().findByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID).get(0);
            assertEquals(Constants.AUTH_BASE_URL_PROP, accountClient.getRootUrl());
            assertEquals("/realms/new/account/", accountClient.getBaseUrl());
            assertEquals("/realms/new/account/*", accountClient.getRedirectUris().get(0));
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

        List<String> clients = adminClient.realms().realm("new-realm").clients().findAll().stream().map(ClientRepresentation::getClientId).collect(Collectors.toList());
        assertThat(clients, containsInAnyOrder("account", "account-console", "admin-cli", "broker", "realm-management", "security-admin-console"));

        adminClient.realms().realm("new-realm").remove();

        Assert.assertNames(adminClient.realms().findAll(), "master", AuthRealm.TEST, REALM_NAME);
    }

    @Test
    public void smtpPasswordSecret() {
        RealmRepresentation rep = RealmBuilder.create().testEventListener().testMail().build();
        rep.setRealm("realm-with-smtp");
        rep.getSmtpServer().put("user", "user");
        rep.getSmtpServer().put("password", "secret");

        adminClient.realms().create(rep);

        RealmRepresentation returned = adminClient.realm("realm-with-smtp").toRepresentation();
        assertEquals(ComponentRepresentation.SECRET_VALUE, returned.getSmtpServer().get("password"));

        KeycloakTestingClient.Server serverClient = testingClient.server("realm-with-smtp");

        RealmRepresentation internalRep = serverClient.fetch(RunHelpers.internalRealm());
        assertEquals("secret", internalRep.getSmtpServer().get("password"));

        adminClient.realm("realm-with-smtp").update(rep);

        AdminEventRepresentation event = testingClient.testing().pollAdminEvent();
        assertFalse(event.getRepresentation().contains("some secret value!!"));
        assertTrue(event.getRepresentation().contains(ComponentRepresentation.SECRET_VALUE));

        internalRep = serverClient.fetch(RunHelpers.internalRealm());
        assertEquals("secret", internalRep.getSmtpServer().get("password"));

        RealmRepresentation realm = adminClient.realms().findAll().stream().filter(r -> r.getRealm().equals("realm-with-smtp")).findFirst().get();
        assertEquals(ComponentRepresentation.SECRET_VALUE, realm.getSmtpServer().get("password"));

        adminClient.realm("realm-with-smtp").remove();
    }

    @Test
    public void createRealmCheckDefaultPasswordPolicy() {
        RealmRepresentation rep = new RealmRepresentation();
        rep.setRealm("new-realm");

        adminClient.realms().create(rep);

        assertEquals(null, adminClient.realm("new-realm").toRepresentation().getPasswordPolicy());

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

    //KEYCLOAK-6146
    @Test
    public void createRealmWithPasswordPolicyFromJsonWithInvalidPasswords() {
        //try to create realm with password policies and users with plain-text passwords what doesn't met the policies
        RealmRepresentation rep = loadJson(getClass().getResourceAsStream("/import/testrealm-keycloak-6146-error.json"), RealmRepresentation.class);

        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage(String.valueOf(Response.Status.NOT_FOUND.getStatusCode()));

        try {
            adminClient.realms().create(rep);
        } catch (BadRequestException ex) {
            //ensure the realm was not created
            log.info("--Caught expected BadRequestException--");
            adminClient.realms().realm("secure-app").toRepresentation();
        }
        //test will fail on AssertionError when both BadRequestException and NotFoundException is not thrown
    }

    //KEYCLOAK-6146
    @Test
    public void createRealmWithPasswordPolicyFromJsonWithValidPasswords() {
        RealmRepresentation rep = loadJson(getClass().getResourceAsStream("/import/testrealm-keycloak-6146.json"), RealmRepresentation.class);
        try (Creator<RealmResource> c = Creator.create(adminClient, rep)) {
            RealmRepresentation created = c.resource().toRepresentation();
            assertRealm(rep, created);
        }
    }

    @Test
    public void removeRealm() {
        realm.remove();

        Assert.assertNames(adminClient.realms().findAll(), "master", AuthRealm.TEST);

        // Re-create realm
        reCreateRealm();
    }

    private void reCreateRealm() {
        // Re-create realm
        RealmRepresentation realmRep = testContext.getTestRealmReps().stream().filter((RealmRepresentation realm) -> {
            return realm.getRealm().equals(REALM_NAME);
        }).findFirst().get();
        adminClient.realms().create(realmRep);
    }

    @Test
    public void loginAfterRemoveRealm() {
        realm.remove();

        try (Keycloak client = Keycloak.getInstance(AuthServerTestEnricher.getAuthServerContextRoot() + "/auth", "master", "admin", "admin", Constants.ADMIN_CLI_CLIENT_ID, TLSUtils.initializeTLS())) {
            client.serverInfo().getInfo();
        }

        reCreateRealm();
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

    private RealmEventsConfigRepresentation copyRealmEventsConfigRepresentation(RealmEventsConfigRepresentation rep) {
        RealmEventsConfigRepresentation recr = new RealmEventsConfigRepresentation();
        recr.setEnabledEventTypes(rep.getEnabledEventTypes());
        recr.setEventsListeners(rep.getEventsListeners());
        recr.setEventsExpiration(rep.getEventsExpiration());
        recr.setEventsEnabled(rep.isEventsEnabled());
        recr.setAdminEventsEnabled(rep.isAdminEventsEnabled());
        recr.setAdminEventsDetailsEnabled(rep.isAdminEventsDetailsEnabled());
        return recr;
    }

    private void checkRealmEventsConfigRepresentation(RealmEventsConfigRepresentation expected,
            RealmEventsConfigRepresentation actual) {
        assertEquals(expected.getEnabledEventTypes().size(), actual.getEnabledEventTypes().size());
        assertTrue(actual.getEnabledEventTypes().containsAll(expected.getEnabledEventTypes()));
        assertEquals(expected.getEventsListeners().size(), actual.getEventsListeners().size());
        assertTrue(actual.getEventsListeners().containsAll(expected.getEventsListeners()));
        assertEquals(expected.getEventsExpiration(), actual.getEventsExpiration());
        assertEquals(expected.isEventsEnabled(), actual.isEventsEnabled());
        assertEquals(expected.isAdminEventsEnabled(), actual.isAdminEventsEnabled());
        assertEquals(expected.isAdminEventsDetailsEnabled(), actual.isAdminEventsDetailsEnabled());
    }

    @Test
    public void updateRealmEventsConfig() {
        RealmEventsConfigRepresentation rep = realm.getRealmEventsConfig();
        RealmEventsConfigRepresentation repOrig = copyRealmEventsConfigRepresentation(rep);

        // the "event-queue" listener should be enabled by default
        assertTrue("event-queue should be enabled initially", rep.getEventsListeners().contains(EventsListenerProviderFactory.PROVIDER_ID));

        // first modification => remove "event-queue", should be sent to the queue
        rep.setEnabledEventTypes(Arrays.asList(EventType.LOGIN.name(), EventType.LOGIN_ERROR.name()));
        rep.setEventsListeners(Arrays.asList(JBossLoggingEventListenerProviderFactory.ID));
        rep.setEventsExpiration(36000L);
        rep.setEventsEnabled(true);
        rep.setAdminEventsEnabled(true);
        rep.setAdminEventsDetailsEnabled(true);
        adminClient.realms().realm(REALM_NAME).updateRealmEventsConfig(rep);
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, "events/config", rep, ResourceType.REALM);
        RealmEventsConfigRepresentation actual = realm.getRealmEventsConfig();
        checkRealmEventsConfigRepresentation(rep, actual);

        // second modification => should not be sent cos event-queue was removed in the first mod
        rep.setEnabledEventTypes(Arrays.asList(EventType.LOGIN.name(),
                EventType.LOGIN_ERROR.name(), EventType.CLIENT_LOGIN.name()));
        adminClient.realms().realm(REALM_NAME).updateRealmEventsConfig(rep);
        assertAdminEvents.assertEmpty();
        actual = realm.getRealmEventsConfig();
        checkRealmEventsConfigRepresentation(rep, actual);

        // third modification => restore queue => should be sent and recovered
        adminClient.realms().realm(REALM_NAME).updateRealmEventsConfig(repOrig);
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, "events/config", repOrig, ResourceType.REALM);
        actual = realm.getRealmEventsConfig();
        checkRealmEventsConfigRepresentation(repOrig, actual);
    }

    @Test
    public void updateRealm() {
        // first change
        RealmRepresentation rep = realm.toRepresentation();
        rep.setSsoSessionIdleTimeout(123);
        rep.setSsoSessionMaxLifespan(12);
        rep.setSsoSessionIdleTimeoutRememberMe(33);
        rep.setSsoSessionMaxLifespanRememberMe(34);
        rep.setAccessCodeLifespanLogin(1234);
        rep.setActionTokenGeneratedByAdminLifespan(2345);
        rep.setActionTokenGeneratedByUserLifespan(3456);
        rep.setRegistrationAllowed(true);
        rep.setRegistrationEmailAsUsername(true);
        rep.setEditUsernameAllowed(true);
        rep.setUserManagedAccessAllowed(true);

        realm.update(rep);
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, Matchers.nullValue(String.class), rep, ResourceType.REALM);

        rep = realm.toRepresentation();

        assertEquals(123, rep.getSsoSessionIdleTimeout().intValue());
        assertEquals(12, rep.getSsoSessionMaxLifespan().intValue());
        assertEquals(33, rep.getSsoSessionIdleTimeoutRememberMe().intValue());
        assertEquals(34, rep.getSsoSessionMaxLifespanRememberMe().intValue());
        assertEquals(1234, rep.getAccessCodeLifespanLogin().intValue());
        assertEquals(2345, rep.getActionTokenGeneratedByAdminLifespan().intValue());
        assertEquals(3456, rep.getActionTokenGeneratedByUserLifespan().intValue());
        assertEquals(Boolean.TRUE, rep.isRegistrationAllowed());
        assertEquals(Boolean.TRUE, rep.isRegistrationEmailAsUsername());
        assertEquals(Boolean.TRUE, rep.isEditUsernameAllowed());
        assertEquals(Boolean.TRUE, rep.isUserManagedAccessAllowed());

        // second change
        rep.setRegistrationAllowed(false);
        rep.setRegistrationEmailAsUsername(false);
        rep.setEditUsernameAllowed(false);
        rep.setUserManagedAccessAllowed(false);

        realm.update(rep);
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, Matchers.nullValue(String.class), rep, ResourceType.REALM);

        rep = realm.toRepresentation();
        assertEquals(Boolean.FALSE, rep.isRegistrationAllowed());
        assertEquals(Boolean.FALSE, rep.isRegistrationEmailAsUsername());
        assertEquals(Boolean.FALSE, rep.isEditUsernameAllowed());
        assertEquals(Boolean.FALSE, rep.isUserManagedAccessAllowed());
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
    public void updateRealmAttributes() {
        // first change
        RealmRepresentation rep = new RealmRepresentation();
        List<String> webAuthnPolicyAcceptableAaguids = new ArrayList<>();
        webAuthnPolicyAcceptableAaguids.add("aaguid1");
        webAuthnPolicyAcceptableAaguids.add("aaguid2");

        rep.setAttributes(new HashMap<>());
        rep.getAttributes().put("foo1", "bar1");
        rep.getAttributes().put("foo2", "bar2");

        rep.setWebAuthnPolicyAcceptableAaguids(webAuthnPolicyAcceptableAaguids);
        rep.setBruteForceProtected(true);
        rep.setDisplayName("dn1");

        realm.update(rep);
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, Matchers.nullValue(String.class), rep, ResourceType.REALM);

        rep = realm.toRepresentation();
        assertEquals("bar1", rep.getAttributes().get("foo1"));
        assertEquals("bar2", rep.getAttributes().get("foo2"));
        assertTrue(rep.isBruteForceProtected());
        assertEquals("dn1", rep.getDisplayName());
        assertEquals(webAuthnPolicyAcceptableAaguids, rep.getWebAuthnPolicyAcceptableAaguids());

        // second change
        webAuthnPolicyAcceptableAaguids.clear();
        rep.setBruteForceProtected(false);
        rep.setDisplayName("dn2");
        rep.getAttributes().put("foo1", "bar11");
        rep.getAttributes().remove("foo2");
        rep.setWebAuthnPolicyAcceptableAaguids(webAuthnPolicyAcceptableAaguids);

        realm.update(rep);
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, Matchers.nullValue(String.class), rep, ResourceType.REALM);

        rep = realm.toRepresentation();

        assertFalse(rep.isBruteForceProtected());
        assertEquals("dn2", rep.getDisplayName());

        assertEquals("bar11", rep.getAttributes().get("foo1"));
        assertFalse(rep.getAttributes().containsKey("foo2"));
        assertTrue(rep.getWebAuthnPolicyAcceptableAaguids().isEmpty());
    }

    @Test
    public void getRealmRepresentation() {
        RealmRepresentation rep = realm.toRepresentation();
        Assert.assertEquals(REALM_NAME, rep.getRealm());
        assertTrue(rep.isEnabled());
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
        if (realm.isLoginWithEmailAllowed() != null) assertEquals(realm.isLoginWithEmailAllowed(), storedRealm.isLoginWithEmailAllowed());
        if (realm.isDuplicateEmailsAllowed() != null) assertEquals(realm.isDuplicateEmailsAllowed(), storedRealm.isDuplicateEmailsAllowed());
        if (realm.isResetPasswordAllowed() != null) assertEquals(realm.isResetPasswordAllowed(), storedRealm.isResetPasswordAllowed());
        if (realm.isEditUsernameAllowed() != null) assertEquals(realm.isEditUsernameAllowed(), storedRealm.isEditUsernameAllowed());
        if (realm.getSslRequired() != null) assertEquals(realm.getSslRequired(), storedRealm.getSslRequired());
        if (realm.getAccessCodeLifespan() != null) assertEquals(realm.getAccessCodeLifespan(), storedRealm.getAccessCodeLifespan());
        if (realm.getAccessCodeLifespanUserAction() != null)
            assertEquals(realm.getAccessCodeLifespanUserAction(), storedRealm.getAccessCodeLifespanUserAction());
        if (realm.getActionTokenGeneratedByAdminLifespan() != null)
            assertEquals(realm.getActionTokenGeneratedByAdminLifespan(), storedRealm.getActionTokenGeneratedByAdminLifespan());
        if (realm.getActionTokenGeneratedByUserLifespan() != null)
            assertEquals(realm.getActionTokenGeneratedByUserLifespan(), storedRealm.getActionTokenGeneratedByUserLifespan());
        else
            assertEquals(realm.getAccessCodeLifespanUserAction(), storedRealm.getActionTokenGeneratedByUserLifespan());
        if (realm.getNotBefore() != null) assertEquals(realm.getNotBefore(), storedRealm.getNotBefore());
        if (realm.getAccessTokenLifespan() != null) assertEquals(realm.getAccessTokenLifespan(), storedRealm.getAccessTokenLifespan());
        if (realm.getAccessTokenLifespanForImplicitFlow() != null) assertEquals(realm.getAccessTokenLifespanForImplicitFlow(), storedRealm.getAccessTokenLifespanForImplicitFlow());
        if (realm.getSsoSessionIdleTimeout() != null) assertEquals(realm.getSsoSessionIdleTimeout(), storedRealm.getSsoSessionIdleTimeout());
        if (realm.getSsoSessionMaxLifespan() != null) assertEquals(realm.getSsoSessionMaxLifespan(), storedRealm.getSsoSessionMaxLifespan());
        if (realm.getSsoSessionIdleTimeoutRememberMe() != null) Assert.assertEquals(realm.getSsoSessionIdleTimeoutRememberMe(), storedRealm.getSsoSessionIdleTimeoutRememberMe());
        if (realm.getSsoSessionMaxLifespanRememberMe() != null) Assert.assertEquals(realm.getSsoSessionMaxLifespanRememberMe(), storedRealm.getSsoSessionMaxLifespanRememberMe());
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

        if (realm.getAttributes() != null) {
            HashMap<String, String> attributes = new HashMap<>();
            attributes.putAll(storedRealm.getAttributes());
            attributes.entrySet().retainAll(realm.getAttributes().entrySet());
            assertEquals(realm.getAttributes(), attributes);
        }

        if (realm.isUserManagedAccessAllowed() != null) assertEquals(realm.isUserManagedAccessAllowed(), storedRealm.isUserManagedAccessAllowed());
    }

    @Test
    public void clearRealmCache() {
        RealmRepresentation realmRep = realm.toRepresentation();
        assertTrue(testingClient.testing().cache("realms").contains(realmRep.getId()));

        realm.clearRealmCache();
        assertAdminEvents.assertEvent(realmId, OperationType.ACTION, "clear-realm-cache", ResourceType.REALM);

        assertFalse(testingClient.testing().cache("realms").contains(realmRep.getId()));
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

        assertTrue(testingClient.testing().cache("users").contains(userId));

        realm.clearUserCache();
        assertAdminEvents.assertEvent(realmId, OperationType.ACTION, "clear-user-cache", ResourceType.REALM);

        assertFalse(testingClient.testing().cache("users").contains(userId));
    }

    // NOTE: clearKeysCache tested in KcOIDCBrokerWithSignatureTest

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

        assertThat(globalRequestResult.getSuccessRequests(), containsInAnyOrder(oauth.AUTH_SERVER_ROOT + "/realms/master/app/admin"));
        assertNull(globalRequestResult.getFailedRequests());

        PushNotBeforeAction adminPushNotBefore = testingClient.testApp().getAdminPushNotBefore();
        assertEquals(time, adminPushNotBefore.getNotBefore());
    }

    @Test
    public void pushNotBeforeWithSamlApp() {
        setupTestAppAndUser();
        setupTestSamlApp();

        int time = Time.currentTime() - 60;

        RealmRepresentation rep = realm.toRepresentation();
        rep.setNotBefore(time);
        realm.update(rep);
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, Matchers.nullValue(String.class), rep, ResourceType.REALM);

        GlobalRequestResult globalRequestResult = realm.pushRevocation();
        assertAdminEvents.assertEvent(realmId, OperationType.ACTION, "push-revocation", globalRequestResult, ResourceType.REALM);

        assertThat(globalRequestResult.getSuccessRequests(), containsInAnyOrder(oauth.AUTH_SERVER_ROOT + "/realms/master/app/admin"));
        assertThat(globalRequestResult.getFailedRequests(), containsInAnyOrder(oauth.AUTH_SERVER_ROOT + "/realms/master/saml-app/saml"));

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
        assertEquals(oauth.AUTH_SERVER_ROOT + "/realms/master/app/admin", globalRequestResult.getSuccessRequests().get(0));
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
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.getCurrentQuery().get(OAuth2Constants.CODE),
            "secret");
        assertEquals(200, tokenResponse.getStatusCode());

        sessionStats = realm.getClientSessionStats();

        assertEquals(1, sessionStats.size());
        assertEquals("test-app", sessionStats.get(0).get("clientId"));
        assertEquals("1", sessionStats.get(0).get("active"));

        String clientUuid = sessionStats.get(0).get("id");
        realm.clients().get(clientUuid).remove();

        sessionStats = realm.getClientSessionStats();

        assertEquals(0, sessionStats.size());
    }

    private void setupTestAppAndUser() {
        testingClient.testApp().clearAdminActions();

        String redirectUri = oauth.getRedirectUri().replace("/master/", "/" + REALM_NAME + "/");

        ClientRepresentation client = new ClientRepresentation();
        client.setClientId("test-app");
        client.setAdminUrl(suiteContext.getAuthServerInfo().getContextRoot() + "/auth/realms/master/app/admin");
        client.setRedirectUris(Collections.singletonList(redirectUri));
        client.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        client.setSecret("secret");
        Response resp = realm.clients().create(client);
        String clientDbId = ApiUtil.getCreatedId(resp);
        getCleanup().addClientUuid(clientDbId);
        resp.close();
        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.clientResourcePath(clientDbId), client, ResourceType.CLIENT);

        oauth.realm(REALM_NAME);
        oauth.redirectUri(redirectUri);

        UserRepresentation userRep = UserBuilder.create().username("testuser").build();
        Response response = realm.users().create(userRep);
        String userId = ApiUtil.getCreatedId(response);
        response.close();
        getCleanup().addUserId(userId);
        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.userResourcePath(userId), userRep, ResourceType.USER);

        realm.users().get(userId).resetPassword(CredentialBuilder.create().password("password").build());
        assertAdminEvents.assertEvent(realmId, OperationType.ACTION, AdminEventPaths.userResetPasswordPath(userId), ResourceType.USER);

        testingClient.testApp().clearAdminActions();
    }

    private void setupTestSamlApp() {
        String redirectUri = oauth.getRedirectUri().replace("/master/", "/" + REALM_NAME + "/");
        ClientRepresentation client = ClientBuilder.create()
          .clientId("test-saml-app")
          .protocol(SamlProtocol.LOGIN_PROTOCOL)
          .adminUrl(suiteContext.getAuthServerInfo().getContextRoot() + "/auth/realms/master/saml-app/saml")
          .addRedirectUri(redirectUri)
          .secret("secret")
          .build();
        Response resp = realm.clients().create(client);
        String clientDbId = ApiUtil.getCreatedId(resp);
        getCleanup().addClientUuid(clientDbId);
        resp.close();
        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.clientResourcePath(clientDbId), client, ResourceType.CLIENT);
    }

}

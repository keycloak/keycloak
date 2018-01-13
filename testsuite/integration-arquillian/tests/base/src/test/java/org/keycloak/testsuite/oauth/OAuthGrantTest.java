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
package org.keycloak.testsuite.oauth;

import org.hamcrest.Matchers;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientTemplateRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.AccountApplicationsPage;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.ProtocolMapperUtil;
import org.keycloak.testsuite.util.RoleBuilder;
import org.openqa.selenium.By;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.admin.ApiUtil.findClientByClientId;
import static org.keycloak.testsuite.admin.ApiUtil.findUserByUsernameId;

/**
 * @author <a href="mailto:vrockai@redhat.com">Viliam Rockai</a>
 */
public class OAuthGrantTest extends AbstractKeycloakTest {

    public static final String THIRD_PARTY_APP = "third-party";
    public static final String REALM_NAME = "test";
    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected OAuthGrantPage grantPage;
    @Page
    protected AccountApplicationsPage accountAppsPage;
    @Page
    protected AppPage appPage;

    @Page
    protected ErrorPage errorPage;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {

        RealmRepresentation realmRepresentation = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        testRealms.add(realmRepresentation);
    }

    private static String ROLE_USER = "Have User privileges";
    private static String ROLE_CUSTOMER = "Have Customer User privileges";

    @Test
    public void oauthGrantAcceptTest() {
        oauth.clientId(THIRD_PARTY_APP);
        oauth.doLoginGrant("test-user@localhost", "password");

        grantPage.assertCurrent();
        Assert.assertTrue(driver.getPageSource().contains(ROLE_USER));
        Assert.assertTrue(driver.getPageSource().contains(ROLE_CUSTOMER));

        grantPage.accept();

        Assert.assertTrue(oauth.getCurrentQuery().containsKey(OAuth2Constants.CODE));

        EventRepresentation loginEvent = events.expectLogin()
                .client(THIRD_PARTY_APP)
                .detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED)
                .assertEvent();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);
        String sessionId = loginEvent.getSessionId();

        OAuthClient.AccessTokenResponse accessToken = oauth.doAccessTokenRequest(oauth.getCurrentQuery().get(OAuth2Constants.CODE), "password");

        String tokenString = accessToken.getAccessToken();
        Assert.assertNotNull(tokenString);
        AccessToken token = oauth.verifyToken(tokenString);
        assertEquals(sessionId, token.getSessionState());

        AccessToken.Access realmAccess = token.getRealmAccess();
        assertEquals(1, realmAccess.getRoles().size());
        Assert.assertTrue(realmAccess.isUserInRole("user"));

        Map<String, AccessToken.Access> resourceAccess = token.getResourceAccess();
        assertEquals(1, resourceAccess.size());
        assertEquals(1, resourceAccess.get("test-app").getRoles().size());
        Assert.assertTrue(resourceAccess.get("test-app").isUserInRole("customer-user"));

        events.expectCodeToToken(codeId, loginEvent.getSessionId()).client(THIRD_PARTY_APP).assertEvent();

        accountAppsPage.open();

        assertEquals(1, driver.findElements(By.id("revoke-third-party")).size());

        accountAppsPage.revokeGrant(THIRD_PARTY_APP);

        events.expect(EventType.REVOKE_GRANT)
                .client("account").detail(Details.REVOKED_CLIENT, THIRD_PARTY_APP).assertEvent();

        assertEquals(0, driver.findElements(By.id("revoke-third-party")).size());
    }

    @Test
    public void oauthGrantCancelTest() {
        oauth.clientId(THIRD_PARTY_APP);
        oauth.doLoginGrant("test-user@localhost", "password");

        grantPage.assertCurrent();
        Assert.assertTrue(driver.getPageSource().contains(ROLE_USER));
        Assert.assertTrue(driver.getPageSource().contains(ROLE_CUSTOMER));

        grantPage.cancel();

        Assert.assertTrue(oauth.getCurrentQuery().containsKey(OAuth2Constants.ERROR));
        assertEquals("access_denied", oauth.getCurrentQuery().get(OAuth2Constants.ERROR));

        events.expectLogin()
                .client(THIRD_PARTY_APP)
                .error("rejected_by_user")
                .removeDetail(Details.CONSENT)
                .session(Matchers.nullValue(String.class))
                .assertEvent();
    }

    @Test
    public void oauthGrantNotShownWhenAlreadyGranted() {
        // Grant permissions on grant screen
        oauth.clientId(THIRD_PARTY_APP);
        oauth.doLoginGrant("test-user@localhost", "password");

        grantPage.assertCurrent();
        grantPage.accept();

        events.expectLogin()
                .client(THIRD_PARTY_APP)
                .detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED)
                .assertEvent();

        // Assert permissions granted on Account mgmt. applications page
        accountAppsPage.open();
        AccountApplicationsPage.AppEntry thirdPartyEntry = accountAppsPage.getApplications().get(THIRD_PARTY_APP);
        Assert.assertTrue(thirdPartyEntry.getRolesGranted().contains(ROLE_USER));
        Assert.assertTrue(thirdPartyEntry.getRolesGranted().contains("Have Customer User privileges in test-app"));
        Assert.assertTrue(thirdPartyEntry.getProtocolMappersGranted().contains("Full name"));
        Assert.assertTrue(thirdPartyEntry.getProtocolMappersGranted().contains("Email"));

        // Open login form and assert grantPage not shown
        oauth.openLoginForm();
        appPage.assertCurrent();
        events.expectLogin()
                .detail(Details.AUTH_METHOD, OIDCLoginProtocol.LOGIN_PROTOCOL)
                .detail(Details.CONSENT, Details.CONSENT_VALUE_PERSISTED_CONSENT)
                .removeDetail(Details.USERNAME)
                .client(THIRD_PARTY_APP).assertEvent();

        // Revoke grant in account mgmt.
        accountAppsPage.open();
        accountAppsPage.revokeGrant(THIRD_PARTY_APP);

        events.expect(EventType.REVOKE_GRANT)
                .client("account").detail(Details.REVOKED_CLIENT, THIRD_PARTY_APP).assertEvent();

        // Open login form again and assert grant Page is shown
        oauth.openLoginForm();
        grantPage.assertCurrent();
        Assert.assertTrue(driver.getPageSource().contains(ROLE_USER));
        Assert.assertTrue(driver.getPageSource().contains(ROLE_CUSTOMER));
    }

    @Test
    public void oauthGrantAddAnotherRoleAndMapper() {
        // Grant permissions on grant screen
        oauth.clientId(THIRD_PARTY_APP);
        oauth.doLoginGrant("test-user@localhost", "password");
        oauth.scope(OAuth2Constants.GRANT_TYPE);

        // Add new protocolMapper and role before showing grant page
        ProtocolMapperRepresentation protocolMapper = ProtocolMapperUtil.createClaimMapper(
                KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME,
                KerberosConstants.GSS_DELEGATION_CREDENTIAL,
                KerberosConstants.GSS_DELEGATION_CREDENTIAL, "String",
                true, KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME,
                true, false);


        RealmResource appRealm = adminClient.realm(REALM_NAME);
        appRealm.roles().create(RoleBuilder.create().name("new-role").build());
        RoleRepresentation newRole = appRealm.roles().get("new-role").toRepresentation();

        ClientManager.realm(adminClient.realm(REALM_NAME)).clientId(THIRD_PARTY_APP)
                .addProtocolMapper(protocolMapper)
                .addScopeMapping(newRole);

        UserResource userResource = findUserByUsernameId(appRealm, "test-user@localhost");
        userResource.roles().realmLevel().add(Collections.singletonList(newRole));

        // Confirm grant page
        grantPage.assertCurrent();
        grantPage.accept();
        events.expectLogin()
                .client(THIRD_PARTY_APP)
                .detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED)
                .assertEvent();

        // Assert new role and protocol mapper not in account mgmt.
        accountAppsPage.open();
        AccountApplicationsPage.AppEntry appEntry = accountAppsPage.getApplications().get(THIRD_PARTY_APP);
        Assert.assertFalse(appEntry.getRolesGranted().contains("new-role"));
        Assert.assertFalse(appEntry.getProtocolMappersGranted().contains(KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME));

        // Show grant page another time. Just new role and protocol mapper are on the page
        oauth.openLoginForm();
        grantPage.assertCurrent();

        Assert.assertFalse(driver.getPageSource().contains(ROLE_USER));
        Assert.assertFalse(driver.getPageSource().contains("Full name"));
        Assert.assertTrue(driver.getPageSource().contains("new-role"));
        Assert.assertTrue(driver.getPageSource().contains(KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME));
        grantPage.accept();
        events.expectLogin()
                .client(THIRD_PARTY_APP)
                .detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED)
                .assertEvent();

        // Go to account mgmt. Everything is granted now
        accountAppsPage.open();
        appEntry = accountAppsPage.getApplications().get(THIRD_PARTY_APP);
        Assert.assertTrue(appEntry.getRolesGranted().contains("new-role"));
        Assert.assertTrue(appEntry.getProtocolMappersGranted().contains(KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME));

        // Revoke
        accountAppsPage.revokeGrant(THIRD_PARTY_APP);
        events.expect(EventType.REVOKE_GRANT)
                .client("account").detail(Details.REVOKED_CLIENT, THIRD_PARTY_APP).assertEvent();

        // Cleanup
        ClientManager.realm(adminClient.realm(REALM_NAME)).clientId(THIRD_PARTY_APP)
                .removeProtocolMapper(KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME)
                .removeScopeMapping(newRole);

        appRealm.roles().deleteRole("new-role");

    }

    @Test
    public void oauthGrantScopeParamRequired() throws Exception {

        RealmResource appRealm = adminClient.realm(REALM_NAME);

        ClientResource thirdParty = findClientByClientId(appRealm, THIRD_PARTY_APP);
        thirdParty.roles().create(RoleBuilder.create().id("bar-role").name("bar-role").scopeParamRequired(true).build());
        RoleRepresentation barAppRole = thirdParty.roles().get("bar-role").toRepresentation();

        appRealm.roles().create(RoleBuilder.create().id("foo-role").name("foo-role").scopeParamRequired(true).build());
        RoleRepresentation fooRole = appRealm.roles().get("foo-role").toRepresentation();
        ClientManager.realm(appRealm).clientId(THIRD_PARTY_APP).addScopeMapping(fooRole);

        UserResource testUser = findUserByUsernameId(appRealm, "test-user@localhost");

        testUser.roles().clientLevel(thirdParty.toRepresentation().getId()).add(Collections.singletonList(barAppRole));
        testUser.roles().realmLevel().add(Collections.singletonList(fooRole));

        // Assert roles not on grant screen when not requested

        oauth.clientId(THIRD_PARTY_APP);
        oauth.doLoginGrant("test-user@localhost", "password");
        grantPage.assertCurrent();
        Assert.assertFalse(driver.getPageSource().contains("foo-role"));
        Assert.assertFalse(driver.getPageSource().contains("bar-role"));
        grantPage.cancel();

        events.expectLogin()
                .client(THIRD_PARTY_APP)
                .error("rejected_by_user")
                .removeDetail(Details.CONSENT)
                .session(Matchers.nullValue(String.class))
                .assertEvent();

        oauth.scope("foo-role third-party/bar-role");
        oauth.doLoginGrant("test-user@localhost", "password");
        grantPage.assertCurrent();
        Assert.assertTrue(driver.getPageSource().contains("foo-role"));
        Assert.assertTrue(driver.getPageSource().contains("bar-role"));
        grantPage.accept();

        events.expectLogin()
                .client(THIRD_PARTY_APP)
                .detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED)
                .assertEvent();

        // Revoke
        accountAppsPage.open();
        accountAppsPage.revokeGrant(THIRD_PARTY_APP);
        events.expect(EventType.REVOKE_GRANT)
                .client("account").detail(Details.REVOKED_CLIENT, THIRD_PARTY_APP).assertEvent();
        // cleanup
        appRealm.roles().deleteRole(fooRole.getName());
        thirdParty.roles().deleteRole(barAppRole.getName());

    }


    // KEYCLOAK-4326
    @Test
    public void oauthGrantClientTemplateMappers() throws Exception {
        // Add client template with some protocol mapper
        RealmResource appRealm = adminClient.realm(REALM_NAME);

        ClientTemplateRepresentation template1 = new ClientTemplateRepresentation();
        template1.setName("foo");
        template1.setFullScopeAllowed(false);
        template1.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Response response = appRealm.clientTemplates().create(template1);
        String templateId = ApiUtil.getCreatedId(response);
        response.close();

        ProtocolMapperRepresentation protocolMapper = ProtocolMapperUtil.createAddressMapper(true, true);
        response = appRealm.clientTemplates().get(templateId).getProtocolMappers().createMapper(protocolMapper);
        response.close();

        // Add template to client
        ClientResource thirdParty = findClientByClientId(appRealm, THIRD_PARTY_APP);
        ClientRepresentation thirdPartyRep = thirdParty.toRepresentation();
        thirdPartyRep.setClientTemplate("foo");
        thirdPartyRep.setUseTemplateMappers(true);
        thirdParty.update(thirdPartyRep);

        // Login
        oauth.clientId(THIRD_PARTY_APP);
        oauth.doLoginGrant("test-user@localhost", "password");
        grantPage.assertCurrent();
        Assert.assertTrue(driver.getPageSource().contains("Email"));
        Assert.assertTrue(driver.getPageSource().contains("Address"));
        grantPage.accept();

        events.expectLogin()
                .client(THIRD_PARTY_APP)
                .detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED)
                .assertEvent();

        // Go to user's application screen
        accountAppsPage.open();
        Assert.assertTrue(accountAppsPage.isCurrent());
        Map<String, AccountApplicationsPage.AppEntry> apps = accountAppsPage.getApplications();
        Assert.assertTrue(apps.containsKey("third-party"));
        Assert.assertTrue(apps.get("third-party").getProtocolMappersGranted().contains("Address"));

        // Login as admin and see the consent screen of particular user
        UserResource user = ApiUtil.findUserByUsernameId(appRealm, "test-user@localhost");
        List<Map<String, Object>> consents = user.getConsents();
        Assert.assertEquals(1, consents.size());

        // Assert automatically logged another time
        oauth.openLoginForm();
        appPage.assertCurrent();
        events.expectLogin()
                .detail(Details.AUTH_METHOD, OIDCLoginProtocol.LOGIN_PROTOCOL)
                .detail(Details.CONSENT, Details.CONSENT_VALUE_PERSISTED_CONSENT)
                .removeDetail(Details.USERNAME)
                .client(THIRD_PARTY_APP).assertEvent();

        // Revoke
        accountAppsPage.open();
        accountAppsPage.revokeGrant(THIRD_PARTY_APP);
        events.expect(EventType.REVOKE_GRANT)
                .client("account").detail(Details.REVOKED_CLIENT, THIRD_PARTY_APP).assertEvent();


    }

    @Test
    public void oauthGrantExpiredAuthSession() throws Exception {
        oauth.clientId(THIRD_PARTY_APP);
        oauth.doLoginGrant("test-user@localhost", "password");

        grantPage.assertCurrent();

        // Expire cookies
        driver.manage().deleteAllCookies();

        grantPage.accept();

        // Assert link "back to application" present
        errorPage.assertCurrent();
        String backToAppLink = errorPage.getBackToApplicationLink();
        ClientRepresentation thirdParty = findClientByClientId(adminClient.realm(REALM_NAME), THIRD_PARTY_APP).toRepresentation();
        Assert.assertEquals(backToAppLink, thirdParty.getBaseUrl());
    }

}

/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.testsuite.broker;

import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.broker.provider.IdpLinkAction;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.UriUtils;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.messages.Messages;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.IdpLinkActionPage;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.oauth.OAuthClient;
import org.keycloak.utils.BrokerUtil;

import org.hamcrest.Matchers;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.keycloak.testsuite.broker.BrokerTestConstants.IDP_OIDC_ALIAS;

import static org.hamcrest.Matchers.is;

/**
 * Test for client-initiated-account linking of the custom application
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KcOidcBrokerIdpLinkActionTest extends AbstractInitializedBaseBrokerTest {

    private static final BrokerConfiguration BROKER_CONFIG_INSTANCE = new KcOidcBrokerConfiguration() {

        @Override
        public List<ClientRepresentation> createProviderClients() {
            List<ClientRepresentation> providerClients = super.createProviderClients();
            providerClients.get(0).setConsentRequired(true);
            return providerClients;
        }
    };

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    private IdpLinkActionPage idpLinkActionPage;

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return BROKER_CONFIG_INSTANCE;
    }

    @Before
    public void recreateConsumerUser() {
        RealmResource providerRealmResource = realmsResouce().realm(bc.providerRealmName());

        String consumerUserID1 = createUser(bc.consumerRealmName(), "user1", "password", "User1", "Last", "user1@keycloak.org",
                user -> user.setEmailVerified(true));
        String consumerUserID2 = createUser(bc.consumerRealmName(), "user2", "password", "User2", "Last", "user2@keycloak.org",
                user -> user.setEmailVerified(true));
        getCleanup(bc.consumerRealmName()).addUserId(consumerUserID1);
    }

    // Test deprecated mechanism for client-initiated account linking
    @Test
    public void testAccountLinkingSuccess_legacyClientInitiatedAccountLinking() throws Exception {
        String userSessionId = loginToConsumer();

        // Redirect to link account on behalf of "broker-app" and login to the IDP
        URI clientInitiatedAccountLinkUri = BrokerUtil.createClientInitiatedLinkURI("broker-app", oauth.getRedirectUri(), bc.getIDPAlias(), bc.consumerRealmName(), userSessionId, new URI(OAuthClient.AUTH_SERVER_ROOT)).getAccountLinkUri();
        driver.navigate().to(clientInitiatedAccountLinkUri.toString());
        loginPage.login(bc.getUserLogin(), bc.getUserPassword());

        grantPage.assertCurrent();
        grantPage.accept();

        appPage.assertCurrent();
        assertKcActionParams(null, null);

        // Check that user is linked to the IDP
        assertUserLinkedToIDP(true);
    }

    @Test
    public void testAccountLinkingSuccess() throws Exception {
        loginToConsumer();

        // Redirect to link account on behalf of "broker-app" and login to the IDP
        String kcAction = getKcActionParamForLinkIdp(bc.getIDPAlias());
        oauth.loginForm().kcAction(kcAction).open();
        confirmIdpLinking();

        // Login to provider
        loginPage.login(bc.getUserLogin(), bc.getUserPassword());

        events.clear();
        grantPage.assertCurrent();
        grantPage.accept();

        appPage.assertCurrent();
        assertKcActionParams(IdpLinkAction.PROVIDER_ID, RequiredActionContext.KcActionStatus.SUCCESS.name().toLowerCase());

        // Check that user is linked to the IDP
        assertUserLinkedToIDP(true);

        assertEvents((providerRealmId, providerUserId, consumerRealmId, consumerUserId, consumerUsername) -> {
            assertProviderEventsSuccess(providerRealmId, providerUserId);
            assertConsumerSuccessLinkEvents(consumerRealmId, consumerUserId, consumerUsername);
        });
    }


    @Test
    public void testAccountLinkingSuccessTriggeredWhenUserNotAuthenticated() throws Exception {
        // Check that user is not linked to the IDP
        assertUserLinkedToIDP(false);

        // Login to consumer with "kc_action" when user not authenticated yet
        String kcAction = getKcActionParamForLinkIdp(bc.getIDPAlias());
        oauth.client("broker-app");
        oauth.realm(bc.consumerRealmName());
        oauth.loginForm().kcAction(kcAction).open();
        loginPage.login("user1", "password");
        confirmIdpLinking();

        // Login to provider
        loginPage.login(bc.getUserLogin(), bc.getUserPassword());
        events.clear();
        grantPage.assertCurrent();
        grantPage.accept();

        appPage.assertCurrent();
        assertKcActionParams(IdpLinkAction.PROVIDER_ID, RequiredActionContext.KcActionStatus.SUCCESS.name().toLowerCase());

        // Check that user is linked to the IDP
        assertUserLinkedToIDP(true);

        assertEvents((providerRealmId, providerUserId, consumerRealmId, consumerUserId, consumerUsername) -> {
            assertProviderEventsSuccess(providerRealmId, providerUserId);
            assertConsumerSuccessLinkEvents(consumerRealmId, consumerUserId, consumerUsername);
        });
    }

    @Test
    public void testAccountLinkingCancel() throws Exception {
        loginToConsumer();

        // Redirect to link account on behalf of "broker-app" and login to the IDP
        String kcAction = getKcActionParamForLinkIdp(bc.getIDPAlias());
        oauth.loginForm().kcAction(kcAction).open();
        events.clear();

        idpLinkActionPage.assertCurrent();
        idpLinkActionPage.assertIdpInMessage(bc.getIDPAlias());
        idpLinkActionPage.cancel();

        appPage.assertCurrent();
        assertKcActionParams(IdpLinkAction.PROVIDER_ID, RequiredActionContext.KcActionStatus.CANCELLED.name().toLowerCase());

        // Check that user is not linked to the IDP
        assertUserLinkedToIDP(false);

        assertEvents((providerRealmId, providerUserId, consumerRealmId, consumerUserId, consumerUsername) -> {
            events.expect(EventType.CUSTOM_REQUIRED_ACTION_ERROR)
                    .realm(consumerRealmId)
                    .client("broker-app")
                    .user(consumerUserId)
                    .detail(Details.CUSTOM_REQUIRED_ACTION, IdpLinkAction.PROVIDER_ID)
                    .detail(Details.USERNAME, consumerUsername)
                    .error(Errors.REJECTED_BY_USER)
                    .assertEvent();

            events.expect(EventType.LOGIN)
                    .realm(consumerRealmId)
                    .client("broker-app")
                    .user(consumerUserId)
                    .session(Matchers.any(String.class))
                    .detail(Details.USERNAME, consumerUsername)
                    .assertEvent();

            events.assertEmpty();
        });
    }

    @Test
    public void testAccountLinkingConsentRejectedAtIdp() throws Exception {
        loginToConsumer();

        // Redirect to link account on behalf of "broker-app" and login to the IDP
        String kcAction = getKcActionParamForLinkIdp(bc.getIDPAlias());
        oauth.loginForm().kcAction(kcAction).open();
        confirmIdpLinking();

        loginPage.login(bc.getUserLogin(), bc.getUserPassword());

        events.clear();
        grantPage.assertCurrent();
        grantPage.cancel();

        appPage.assertCurrent();
        assertKcActionParams(IdpLinkAction.PROVIDER_ID, RequiredActionContext.KcActionStatus.CANCELLED.name().toLowerCase());

        // Check that user is not linked to the IDP
        assertUserLinkedToIDP(false);

        assertEvents((providerRealmId, providerUserId, consumerRealmId, consumerUserId, consumerUsername) -> {
            // Provider login - rejected consent screen
            events.expect(EventType.LOGIN_ERROR)
                    .realm(providerRealmId)
                    .user(providerUserId)
                    .client(bc.getIDPClientIdInProviderRealm())
                    .session((String)null)
                    .detail(Details.USERNAME, bc.getUserLogin())
                    .error(Errors.REJECTED_BY_USER)
                    .assertEvent();

            // Consumer - rejected provider consent screen event propagated
            assertConsumerFailedLinkEvents(consumerRealmId, consumerUserId, consumerUsername, Errors.REJECTED_BY_USER, true);

            events.assertEmpty();
        });
    }


    @Test
    public void testAccountLinkingDifferentUserLinked() throws Exception {
        // Link IDP to user "user2"
        Response response = AccountHelper.addIdentityProvider(adminClient.realm(bc.consumerRealmName()), "user2", adminClient.realm(bc.providerRealmName()), bc.getUserLogin(), bc.getIDPAlias());
        Assert.assertEquals(204, response.getStatus());

        // Linking the user "user1" to same IDP should fail
        loginToConsumer();

        String kcAction = getKcActionParamForLinkIdp(bc.getIDPAlias());
        oauth.loginForm().kcAction(kcAction).open();
        confirmIdpLinking();

        loginPage.login(bc.getUserLogin(), bc.getUserPassword());

        events.clear();
        grantPage.assertCurrent();
        grantPage.accept();

        errorPage.assertCurrent();
        Assert.assertEquals("Federated identity returned by " + bc.getIDPAlias() + " is already linked to another user.", errorPage.getError());
        Assert.assertEquals(bc.createConsumerClients().get(0).getBaseUrl(), errorPage.getBackToApplicationLink());

        // Check that user is not linked to the IDP
        assertUserLinkedToIDP(false);

        assertEvents((providerRealmId, providerUserId, consumerRealmId, consumerUserId, consumerUsername) -> {
            assertProviderEventsSuccess(providerRealmId, providerUserId);
            assertConsumerFailedLinkEvents(consumerRealmId, consumerUserId, consumerUsername, Messages.IDENTITY_PROVIDER_ALREADY_LINKED, false);

            events.assertEmpty();
        });
    }


    @Test
    public void testAccountLinkingUserNotAllowed() throws Exception {
        // Remove "manage-account" role from user
        RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());
        String user1Id = consumerRealm.users().search("user1").iterator().next().getId();

        RoleRepresentation defaultRoles = consumerRealm.roles().get(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + bc.consumerRealmName()).toRepresentation();
        consumerRealm.users().get(user1Id).roles().realmLevel().remove(Collections.singletonList(defaultRoles));

        // Linking the user "user1" to the IDP not allowed due insufficient permissions
        loginToConsumer();

        events.clear();

        String kcAction = getKcActionParamForLinkIdp(bc.getIDPAlias());
        oauth.loginForm().kcAction(kcAction).open();

        // Should be redirected to the application even before being redirected to IDP for authentication
        appPage.assertCurrent();

        // Check that user is not linked to the IDP
        assertUserLinkedToIDP(false);

        assertEvents((providerRealmId, providerUserId, consumerRealmId, consumerUserId, consumerUsername) -> {
            assertConsumerFailedLinkEvents(consumerRealmId, consumerUserId, consumerUsername, Errors.NOT_ALLOWED, true);

            events.assertEmpty();
        });

    }

    @Test
    public void testAccountLinkingWithDirectRoleManageAccount() throws Exception {
        // Remove "manage-account" role from user
        RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());
        String user1Id = consumerRealm.users().search("user1").iterator().next().getId();

        RoleRepresentation defaultRoles = consumerRealm.roles().get(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + bc.consumerRealmName()).toRepresentation();
        consumerRealm.users().get(user1Id).roles().realmLevel().remove(Collections.singletonList(defaultRoles));

        ClientRepresentation accountClient = ApiUtil.findClientResourceByClientId(consumerRealm, Constants.ACCOUNT_MANAGEMENT_CLIENT_ID).toRepresentation();
        RoleRepresentation manageAccount = consumerRealm.clients().get(accountClient.getId()).roles().get(AccountRoles.MANAGE_ACCOUNT).toRepresentation();
        consumerRealm.users().get(user1Id).roles().clientLevel(accountClient.getId()).add(Collections.singletonList(manageAccount));

        loginToConsumer();

        // Redirect to link account on behalf of "broker-app" and login to the IDP
        String kcAction = getKcActionParamForLinkIdp(bc.getIDPAlias());
        oauth.loginForm().kcAction(kcAction).open();
        confirmIdpLinking();

        // Login to provider
        loginPage.login(bc.getUserLogin(), bc.getUserPassword());

        events.clear();
        grantPage.assertCurrent();
        grantPage.accept();

        appPage.assertCurrent();
        assertKcActionParams(IdpLinkAction.PROVIDER_ID, RequiredActionContext.KcActionStatus.SUCCESS.name().toLowerCase());

        // Check that user is linked to the IDP
        assertUserLinkedToIDP(true);

        assertEvents((providerRealmId, providerUserId, consumerRealmId, consumerUserId, consumerUsername) -> {
            assertProviderEventsSuccess(providerRealmId, providerUserId);
            assertConsumerSuccessLinkEvents(consumerRealmId, consumerUserId, consumerUsername);
        });
    }

    @Test
    public void testAccountLinkingWithDirectRoleManageAccountLinks() throws Exception {
        // Remove "manage-account-link" role from user
        RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());
        String user1Id = consumerRealm.users().search("user1").iterator().next().getId();

        RoleRepresentation defaultRoles = consumerRealm.roles().get(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + bc.consumerRealmName()).toRepresentation();
        consumerRealm.users().get(user1Id).roles().realmLevel().remove(Collections.singletonList(defaultRoles));

        ClientRepresentation accountClient = ApiUtil.findClientResourceByClientId(consumerRealm, Constants.ACCOUNT_MANAGEMENT_CLIENT_ID).toRepresentation();
        RoleRepresentation manageAccountLinks = consumerRealm.clients().get(accountClient.getId()).roles().get(AccountRoles.MANAGE_ACCOUNT_LINKS).toRepresentation();
        consumerRealm.users().get(user1Id).roles().clientLevel(accountClient.getId()).add(Collections.singletonList(manageAccountLinks));

        loginToConsumer();

        // Redirect to link account on behalf of "broker-app" and login to the IDP
        String kcAction = getKcActionParamForLinkIdp(bc.getIDPAlias());
        oauth.loginForm().kcAction(kcAction).open();
        confirmIdpLinking();

        // Login to provider
        loginPage.login(bc.getUserLogin(), bc.getUserPassword());

        events.clear();
        grantPage.assertCurrent();
        grantPage.accept();

        appPage.assertCurrent();
        assertKcActionParams(IdpLinkAction.PROVIDER_ID, RequiredActionContext.KcActionStatus.SUCCESS.name().toLowerCase());

        // Check that user is linked to the IDP
        assertUserLinkedToIDP(true);

        assertEvents((providerRealmId, providerUserId, consumerRealmId, consumerUserId, consumerUsername) -> {
            assertProviderEventsSuccess(providerRealmId, providerUserId);
            assertConsumerSuccessLinkEvents(consumerRealmId, consumerUserId, consumerUsername);
        });
    }

    @Test
    public void testConsumerReauthentication() throws Exception {
        loginToConsumer();

        // Link IDP to user "user1"
        Response response = AccountHelper.addIdentityProvider(adminClient.realm(bc.consumerRealmName()), "user1", adminClient.realm(bc.providerRealmName()), bc.getUserLogin(), bc.getIDPAlias());
        Assert.assertEquals(204, response.getStatus());

        setTimeOffset(2);

        // Enforce re-authentication on "consumer" realm. Try to do re-authentication with the use of IDP, but reject consent screen on IDP side
        oauth.loginForm().maxAge(1).open();
        loginPage.assertCurrent(bc.consumerRealmName());
        loginPage.clickSocial(bc.getIDPAlias());
        loginPage.login(bc.getUserLogin(), bc.getUserPassword());

        events.clear();
        grantPage.assertCurrent();
        grantPage.cancel();

        // Should be redirected back to "consumer" login
        loginPage.assertCurrent(bc.consumerRealmName());
        Assert.assertEquals("Access denied when authenticating with kc-oidc-idp", loginPage.getError());

        assertEvents((providerRealmId, providerUserId, consumerRealmId, consumerUserId, consumerUsername) -> {
            // Provider login - rejected consent screen
            events.expect(EventType.LOGIN_ERROR)
                    .realm(providerRealmId)
                    .user(providerUserId)
                    .client(bc.getIDPClientIdInProviderRealm())
                    .session((String)null)
                    .detail(Details.USERNAME, bc.getUserLogin())
                    .error(Errors.REJECTED_BY_USER)
                    .assertEvent();

            events.assertEmpty();
        });
    }

    private String loginToConsumer() {
        // Login to "consumer" realm with password
        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        loginPage.login("user1", "password");
        appPage.assertCurrent();
        String userSessionId = oauth.parseLoginResponse().getSessionState();

        // Check that user is not linked to the IDP
        assertUserLinkedToIDP(false);

        return userSessionId;
    }

    private void confirmIdpLinking() {
        idpLinkActionPage.assertCurrent();
        idpLinkActionPage.assertIdpInMessage(bc.getIDPAlias());
        idpLinkActionPage.confirm();
    }

    private static String getKcActionParamForLinkIdp(String providerAlias) {
        return IdpLinkAction.PROVIDER_ID + ":" + providerAlias;
    }

    private void assertKcActionParams(String expectedKcAction, String expectedKcActionStatus) throws Exception {
        MultivaluedHashMap<String, String> params = UriUtils.decodeQueryString(new URL(driver.getCurrentUrl()).getQuery());
        Assert.assertEquals(expectedKcAction, params.getFirst(Constants.KC_ACTION));
        Assert.assertEquals(expectedKcActionStatus, params.getFirst(Constants.KC_ACTION_STATUS));
    }

    private void assertUserLinkedToIDP(boolean expectedLinked) {
        Assert.assertThat(expectedLinked, is(AccountHelper.isIdentityProviderLinked(adminClient.realm(bc.consumerRealmName()), "user1", bc.getIDPAlias())));
    }

    @FunctionalInterface
    public interface EventDataConsumer {
        void accept(String providerRealmId, String providerUserId, String consumerRealmId, String consumerUserId, String consumerUsername);
    }

    private void assertEvents(EventDataConsumer assertImpl) {
        RealmResource providerRealm = adminClient.realm(bc.providerRealmName());
        String providerRealmId = providerRealm.toRepresentation().getId();
        UserRepresentation providerUser = providerRealm.users().search(bc.getUserLogin()).iterator().next();
        String providerUserId = providerUser.getId();

        String username = "user1";
        RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());
        String consumerRealmId = consumerRealm.toRepresentation().getId();
        UserRepresentation consumerUser = consumerRealm.users().search(username).iterator().next();
        String consumerUserId = consumerUser.getId();

        assertImpl.accept(providerRealmId, providerUserId, consumerRealmId, consumerUserId, username);
    }

    private void assertProviderEventsSuccess(String providerRealmId, String providerUserId) {
        events.expect(EventType.LOGIN)
                .realm(providerRealmId)
                .user(providerUserId)
                .client(bc.getIDPClientIdInProviderRealm())
                .session(Matchers.any(String.class))
                .detail(Details.USERNAME, bc.getUserLogin())
                .assertEvent();

        events.expect(EventType.CODE_TO_TOKEN)
                .session(Matchers.any(String.class))
                .realm(providerRealmId)
                .user(providerUserId)
                .client(bc.getIDPClientIdInProviderRealm())
                .assertEvent();

        events.expect(EventType.USER_INFO_REQUEST)
                .session(Matchers.any(String.class))
                .realm(providerRealmId)
                .user(providerUserId)
                .client(bc.getIDPClientIdInProviderRealm())
                .assertEvent();
    }

    private void assertConsumerSuccessLinkEvents(String consumerRealmId, String consumerUserId, String username) {
        events.expect(EventType.FEDERATED_IDENTITY_LINK)
                .realm(consumerRealmId)
                .client("broker-app")
                .user(consumerUserId)
                .detail(Details.USERNAME, username)
                .detail(Details.IDENTITY_PROVIDER, IDP_OIDC_ALIAS)
                .detail(Details.IDENTITY_PROVIDER_USERNAME, bc.getUserLogin())
                .detail(Details.IDENTITY_PROVIDER_BROKER_SESSION_ID,  Matchers.startsWith(bc.getIDPAlias()))
                .assertEvent();

        events.expect(EventType.LOGIN)
                .realm(consumerRealmId)
                .client("broker-app")
                .user(consumerUserId)
                .session(Matchers.any(String.class))
                .detail(Details.USERNAME, username)
                .assertEvent();

        events.assertEmpty();
    }

    private void assertConsumerFailedLinkEvents(String consumerRealmId, String consumerUserId, String consumerUsername, String expectedError, boolean expectLoginEvent) {
        events.expect(EventType.FEDERATED_IDENTITY_LINK_ERROR)
                .realm(consumerRealmId)
                .client("broker-app")
                .user(consumerUserId)
                .detail(Details.USERNAME, consumerUsername)
                .detail(Details.IDENTITY_PROVIDER, IDP_OIDC_ALIAS)
                .error(expectedError)
                .assertEvent();

        if (expectLoginEvent) {
            events.expect(EventType.LOGIN)
                    .realm(consumerRealmId)
                    .client("broker-app")
                    .user(consumerUserId)
                    .session(Matchers.any(String.class))
                    .detail(Details.USERNAME, consumerUsername)
                    .assertEvent();
        }

        events.assertEmpty();
    }


}

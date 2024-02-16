/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.client.policies;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.admin.ApiUtil.findUserByUsername;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createAnyClientConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createClientAccessTypeConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createClientRolesConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createClientScopesConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createTestRaiseExeptionExecutorConfig;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.Profile;
import org.keycloak.events.Details;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.OAuth2DeviceConfig;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientAccessTypeConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientRolesConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientScopesConditionFactory;
import org.keycloak.services.clientpolicy.executor.SuppressRefreshTokenRotationExecutorFactory;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LogoutConfirmPage;
import org.keycloak.testsuite.pages.OAuth2DeviceVerificationPage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.services.clientpolicy.executor.TestEnhancedPluggableTokenManagerExecutorFactory;
import org.keycloak.testsuite.services.clientpolicy.executor.TestRaiseExceptionExecutorFactory;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientPoliciesBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientPolicyBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientProfileBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientProfilesBuilder;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RoleBuilder;
import org.keycloak.testsuite.util.UserBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertNull;

/**
 * This test class is for testing a newly supported event for client policies.
 * 
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
@EnableFeature(value = Profile.Feature.CLIENT_SECRET_ROTATION)
public class ClientPoliciesExtendedEventTest extends AbstractClientPoliciesTest {

    @Page
    protected OAuth2DeviceVerificationPage verificationPage;

    @Page
    protected OAuthGrantPage grantPage;

    @Page
    protected ErrorPage errorPage;

    @Page
    protected LogoutConfirmPage logoutConfirmPage;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);

        List<UserRepresentation> users = realm.getUsers();

        LinkedList<CredentialRepresentation> credentials = new LinkedList<>();
        CredentialRepresentation password = new CredentialRepresentation();
        password.setType(CredentialRepresentation.PASSWORD);
        password.setValue("password");
        credentials.add(password);

        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername("manage-clients");
        user.setCredentials(credentials);
        user.setClientRoles(Collections.singletonMap(Constants.REALM_MANAGEMENT_CLIENT_ID, Collections.singletonList(AdminRoles.MANAGE_CLIENTS)));

        users.add(user);

        user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername("create-clients");
        user.setCredentials(credentials);
        user.setClientRoles(Collections.singletonMap(Constants.REALM_MANAGEMENT_CLIENT_ID, Collections.singletonList(AdminRoles.CREATE_CLIENT)));
        user.setGroups(Arrays.asList("topGroup")); // defined in testrealm.json

        users.add(user);

        realm.setUsers(users);

        List<ClientRepresentation> clients = realm.getClients();

        ClientRepresentation app = ClientBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .clientId("test-device")
                .secret("secret")
                .attribute(OAuth2DeviceConfig.OAUTH2_DEVICE_AUTHORIZATION_GRANT_ENABLED, "true")
                .attribute(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, "+")
                .build();
        clients.add(app);

        ClientRepresentation appPublic = ClientBuilder.create().id(KeycloakModelUtils.generateId()).publicClient()
                .clientId(DEVICE_APP_PUBLIC)
                .attribute(OAuth2DeviceConfig.OAUTH2_DEVICE_AUTHORIZATION_GRANT_ENABLED, "true")
                .attribute(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, "+")
                .build();
        clients.add(appPublic);

        userId = KeycloakModelUtils.generateId();
        UserRepresentation deviceUser = UserBuilder.create()
                .id(userId)
                .username("device-login")
                .email("device-login@localhost")
                .password("password")
                .build();
        users.add(deviceUser);

        testRealms.add(realm);
    }

    @Test
    public void testExtendedClientPolicyIntefacesForClientRegistrationPolicyMigrationCreate() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Den Forste Profilen")
                        .addExecutor(TestRaiseExceptionExecutorFactory.PROVIDER_ID,
                                createTestRaiseExeptionExecutorConfig(Arrays.asList(
                                        ClientPolicyEvent.REGISTERED, ClientPolicyEvent.UPDATED, ClientPolicyEvent.UNREGISTER)))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID, createAnyClientConditionConfig())
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        String clientName = "ByAdmin-App" + KeycloakModelUtils.generateId().substring(0, 7);

        try {
            createClientByAdmin(clientName, (ClientRepresentation clientRep) -> {
            });
            fail();
        } catch (ClientPolicyException cpe) {
            assertEquals(ClientPolicyEvent.REGISTERED.toString(), cpe.getError());
        }

        assertThat(adminClient.realm(REALM_NAME).clients().findByClientId(clientName), empty());
    }

    @Test
    public void testExtendedClientPolicyIntefacesForClientRegistrationPolicyMigrationUpdate() throws Exception {
        String clientName = "ByAdmin-App" + KeycloakModelUtils.generateId().substring(0, 7);
        String clientId = null;

        clientId = createClientByAdmin(clientName, (ClientRepresentation clientRep) -> {});
        assertEquals(true, getClientByAdmin(clientId).isEnabled());

        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Den Forste Profilen")
                        .addExecutor(TestRaiseExceptionExecutorFactory.PROVIDER_ID,
                                createTestRaiseExeptionExecutorConfig(Arrays.asList(
                                        ClientPolicyEvent.REGISTERED, ClientPolicyEvent.UPDATED, ClientPolicyEvent.UNREGISTER)))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID, createAnyClientConditionConfig())
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        try {
            updateClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
                clientRep.setEnabled(false);
            });
            fail();
        } catch (ClientPolicyException cpe) {
            assertEquals(ClientPolicyEvent.UPDATED.toString(), cpe.getError());
        }

        assertEquals(true, getClientByAdmin(clientId).isEnabled());

        try {
            deleteClientByAdmin(clientId);
            fail();
        } catch (ClientPolicyException cpe) {
            assertEquals(ClientPolicyEvent.UNREGISTER.toString(), cpe.getError());
        }

        // TODO : For dynamic client registration, the existing test scheme can not distinguish when the exception happens on which event so that the migrated client policy executors test them afterwards.
    }

    @Test
    public void testExtendedClientPolicyIntefacesForDeviceAuthorizationRequest() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Den Forste Profilen")
                        .addExecutor(TestRaiseExceptionExecutorFactory.PROVIDER_ID,
                                createTestRaiseExeptionExecutorConfig(Arrays.asList(ClientPolicyEvent.DEVICE_AUTHORIZATION_REQUEST)))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID, createAnyClientConditionConfig())
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // Device Authorization Request from device
        oauth.realm(REALM_NAME);
        oauth.clientId(DEVICE_APP);
        OAuthClient.DeviceAuthorizationResponse response = oauth.doDeviceAuthorizationRequest(DEVICE_APP, "secret");
        assertEquals(400, response.getStatusCode());
        assertEquals(ClientPolicyEvent.DEVICE_AUTHORIZATION_REQUEST.toString(), response.getError());
        assertEquals("Exception thrown intentionally", response.getErrorDescription());
    }

    @Test
    public void testExtendedClientPolicyIntefacesForDeviceTokenRequest() throws Exception {
        // Device Authorization Request from device
        oauth.realm(REALM_NAME);
        oauth.clientId(DEVICE_APP);
        OAuthClient.DeviceAuthorizationResponse response = oauth.doDeviceAuthorizationRequest(DEVICE_APP, "secret");

        Assert.assertEquals(200, response.getStatusCode());
        assertNotNull(response.getDeviceCode());
        assertNotNull(response.getUserCode());
        assertNotNull(response.getVerificationUri());
        assertNotNull(response.getVerificationUriComplete());

        // Verify user code from verification page using browser
        openVerificationPage(response.getVerificationUri());
        verificationPage.assertCurrent();
        verificationPage.submit(response.getUserCode());

        loginPage.assertCurrent();

        // Do Login
        oauth.fillLoginForm("device-login", "password");

        // Consent
        grantPage.assertCurrent();
        grantPage.assertGrants(OAuthGrantPage.PROFILE_CONSENT_TEXT, OAuthGrantPage.EMAIL_CONSENT_TEXT, OAuthGrantPage.ROLES_CONSENT_TEXT);
        grantPage.accept();

        verificationPage.assertApprovedPage();

        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Den Forste Profilen")
                        .addExecutor(TestRaiseExceptionExecutorFactory.PROVIDER_ID,
                                createTestRaiseExeptionExecutorConfig(Arrays.asList(ClientPolicyEvent.DEVICE_TOKEN_REQUEST)))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID, createAnyClientConditionConfig())
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // Token request from device
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doDeviceTokenRequest(DEVICE_APP, "secret", response.getDeviceCode());
        assertEquals(400, tokenResponse.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_GRANT, tokenResponse.getError());
        assertEquals("Exception thrown intentionally", tokenResponse.getErrorDescription());
    }

    @Test
    public void testExtendedClientPolicyIntefacesForDeviceTokenResponse() throws Exception {
        // Device Authorization Request from device
        oauth.realm(REALM_NAME);
        oauth.clientId(DEVICE_APP);
        OAuthClient.DeviceAuthorizationResponse response = oauth.doDeviceAuthorizationRequest(DEVICE_APP, "secret");

        Assert.assertEquals(200, response.getStatusCode());
        assertNotNull(response.getDeviceCode());
        assertNotNull(response.getUserCode());
        assertNotNull(response.getVerificationUri());
        assertNotNull(response.getVerificationUriComplete());

        // Verify user code from verification page using browser
        openVerificationPage(response.getVerificationUri());
        verificationPage.assertCurrent();
        verificationPage.submit(response.getUserCode());

        loginPage.assertCurrent();

        // Do Login
        oauth.fillLoginForm("device-login", "password");

        // Consent
        grantPage.assertCurrent();
        grantPage.assertGrants(OAuthGrantPage.PROFILE_CONSENT_TEXT, OAuthGrantPage.EMAIL_CONSENT_TEXT, OAuthGrantPage.ROLES_CONSENT_TEXT);
        grantPage.accept();

        verificationPage.assertApprovedPage();

        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Den Forste Profilen")
                        .addExecutor(TestRaiseExceptionExecutorFactory.PROVIDER_ID,
                                createTestRaiseExeptionExecutorConfig(Arrays.asList(ClientPolicyEvent.DEVICE_TOKEN_RESPONSE)))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID, createAnyClientConditionConfig())
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // Token request from device
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doDeviceTokenRequest(DEVICE_APP, "secret", response.getDeviceCode());
        assertEquals(400, tokenResponse.getStatusCode());
        assertEquals(ClientPolicyEvent.DEVICE_TOKEN_RESPONSE.toString(), tokenResponse.getError());
        assertEquals("Exception thrown intentionally", tokenResponse.getErrorDescription());
    }

    @Test
    public void testExtendedClientPolicyIntefacesForTokenResponse() throws Exception {
        // register a confidential client
        String clientId = generateSuffixedName(CLIENT_NAME);
        String clientSecret = "secret";
        createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret(clientSecret);
            clientRep.setPublicClient(Boolean.FALSE);
            clientRep.setBearerOnly(Boolean.FALSE);
        });

        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Den Forste Profilen")
                        .addExecutor(TestRaiseExceptionExecutorFactory.PROVIDER_ID,
                                createTestRaiseExeptionExecutorConfig(Arrays.asList(ClientPolicyEvent.TOKEN_RESPONSE)))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Primera Plitica", Boolean.TRUE)
                        .addCondition(ClientAccessTypeConditionFactory.PROVIDER_ID,
                                createClientAccessTypeConditionConfig(Arrays.asList(ClientAccessTypeConditionFactory.TYPE_CONFIDENTIAL)))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        oauth.clientId(clientId);
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

        events.expectLogin().client(clientId).assertEvent();
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, clientSecret);
        assertEquals(400, response.getStatusCode());
        assertEquals(ClientPolicyEvent.TOKEN_RESPONSE.toString(), response.getError());
        assertEquals("Exception thrown intentionally", response.getErrorDescription());
    }

    @Test
    public void testExtendedClientPolicyIntefacesForTokenRefreshResponse() throws Exception {
        String clientId = generateSuffixedName(CLIENT_NAME);
        String clientSecret = "secret";
        String cid = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret(clientSecret);
            clientRep.setStandardFlowEnabled(Boolean.TRUE);
            clientRep.setImplicitFlowEnabled(Boolean.TRUE);
            clientRep.setPublicClient(Boolean.FALSE);
        });
        adminClient.realm(REALM_NAME).clients().get(cid).roles().create(RoleBuilder.create().name(SAMPLE_CLIENT_ROLE).build());

        oauth.clientId(clientId);
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

        EventRepresentation loginEvent = events.expectLogin().client(clientId).assertEvent();
        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);
        String code = new OAuthClient.AuthorizationEndpointResponse(oauth).getCode();

        OAuthClient.AccessTokenResponse res = oauth.doAccessTokenRequest(code, clientSecret);
        assertEquals(200, res.getStatusCode());
        events.expectCodeToToken(codeId, sessionId).client(clientId).assertEvent();

        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(SuppressRefreshTokenRotationExecutorFactory.PROVIDER_ID, null)
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Den Forste Politikken", Boolean.TRUE)
                        .addCondition(ClientRolesConditionFactory.PROVIDER_ID,
                                createClientRolesConditionConfig(Arrays.asList(SAMPLE_CLIENT_ROLE)))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        String refreshTokenString = res.getRefreshToken();
        OAuthClient.AccessTokenResponse accessTokenResponseRefreshed = oauth.doRefreshTokenRequest(refreshTokenString, clientSecret);
        assertEquals(200, accessTokenResponseRefreshed.getStatusCode());
        assertEquals(null, accessTokenResponseRefreshed.getRefreshToken());

        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Den Forste Politikken", Boolean.TRUE)
                        .addCondition(ClientRolesConditionFactory.PROVIDER_ID,
                                createClientRolesConditionConfig(Arrays.asList("other" + SAMPLE_CLIENT_ROLE)))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        accessTokenResponseRefreshed = oauth.doRefreshTokenRequest(refreshTokenString, clientSecret);
        assertEquals(200, accessTokenResponseRefreshed.getStatusCode());
        RefreshToken refreshedRefreshToken = oauth.parseRefreshToken(accessTokenResponseRefreshed.getRefreshToken());
        assertEquals(sessionId, refreshedRefreshToken.getSessionState());
        assertEquals(sessionId, refreshedRefreshToken.getSessionState());
        assertEquals(findUserByUsername(adminClient.realm(REALM_NAME), TEST_USER_NAME).getId(), refreshedRefreshToken.getSubject());
    }

    @Test
    public void testExtendedClientPolicyIntefacesForServiceAccountTokenRequeponse() throws Exception {
        String clientId = "service-account-app";
        String clientSecret = "app-secret";
        createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret(clientSecret);
            clientRep.setStandardFlowEnabled(Boolean.FALSE);
            clientRep.setImplicitFlowEnabled(Boolean.FALSE);
            clientRep.setServiceAccountsEnabled(Boolean.TRUE);
            clientRep.setPublicClient(Boolean.FALSE);
            clientRep.setBearerOnly(Boolean.FALSE);
        });

        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Den Forste Profilen")
                        .addExecutor(TestRaiseExceptionExecutorFactory.PROVIDER_ID,
                                createTestRaiseExeptionExecutorConfig(Arrays.asList(ClientPolicyEvent.SERVICE_ACCOUNT_TOKEN_RESPONSE)))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Het Eerste Beleid", Boolean.TRUE)
                        .addCondition(ClientScopesConditionFactory.PROVIDER_ID,
                                createClientScopesConditionConfig(ClientScopesConditionFactory.OPTIONAL, Arrays.asList("offline_access", "microprofile-jwt")))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);


        String origClientId = oauth.getClientId();
        oauth.clientId("service-account-app");
        oauth.scope("offline_access");
        try {
            OAuthClient.AccessTokenResponse response = oauth.doClientCredentialsGrantAccessTokenRequest("app-secret");
            assertEquals(400, response.getStatusCode());
            assertEquals(ClientPolicyEvent.SERVICE_ACCOUNT_TOKEN_RESPONSE.toString(), response.getError());
            assertEquals("Exception thrown intentionally", response.getErrorDescription());
        } finally {
            oauth.clientId(origClientId);
        }
    }

    @Test
    public void testExtendedClientPolicyIntefacesForResourceOwnerPasswordCredentialsResponse() throws Exception {

        String clientId = generateSuffixedName(CLIENT_NAME);
        String clientSecret = "secret";

        createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret(clientSecret);
            clientRep.setStandardFlowEnabled(Boolean.TRUE);
            clientRep.setDirectAccessGrantsEnabled(Boolean.TRUE);
            clientRep.setPublicClient(Boolean.FALSE);
        });

        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Den Forste Profilen")
                        .addExecutor(TestRaiseExceptionExecutorFactory.PROVIDER_ID,
                                createTestRaiseExeptionExecutorConfig(Arrays.asList(ClientPolicyEvent.RESOURCE_OWNER_PASSWORD_CREDENTIALS_RESPONSE)))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Porisii desu", Boolean.TRUE)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID,
                                createAnyClientConditionConfig())
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        oauth.clientId(clientId);
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest(clientSecret, TEST_USER_NAME, TEST_USER_PASSWORD, null);

        assertEquals(400, response.getStatusCode());
        assertEquals(ClientPolicyEvent.RESOURCE_OWNER_PASSWORD_CREDENTIALS_RESPONSE.toString(), response.getError());
        assertEquals("Exception thrown intentionally", response.getErrorDescription());
    }

    @Test
    public void testExtendedClientPolicyIntefacesForPreAuthorizationRequest() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Den Forste Profilen")
                        .addExecutor(TestRaiseExceptionExecutorFactory.PROVIDER_ID,
                                createTestRaiseExeptionExecutorConfig(Arrays.asList(ClientPolicyEvent.PRE_AUTHORIZATION_REQUEST)))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        String clientId = generateSuffixedName(CLIENT_NAME);
        String clientSecret = "secret";
        String cid = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret(clientSecret);
        });
        adminClient.realm(REALM_NAME).clients().get(cid).roles().create(RoleBuilder.create().name(SAMPLE_CLIENT_ROLE).build());

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Dei Eischt Politik", Boolean.TRUE)
                        .addCondition(ClientRolesConditionFactory.PROVIDER_ID,
                                createClientRolesConditionConfig(Arrays.asList(SAMPLE_CLIENT_ROLE)))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // Authorization Request
        oauth.realm(REALM_NAME);
        oauth.clientId(clientId);
        oauth.openLoginForm();
        assertTrue(errorPage.isCurrent());
        assertEquals("Exception thrown intentionally", errorPage.getError());
    }

    @Test
    public void testEnhancedPluggableTokenManagerForTokenResponse() throws Exception {
        // register a confidential client
        String clientId = generateSuffixedName(CLIENT_NAME);
        String clientSecret = "secret";
        createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret(clientSecret);
            clientRep.setPublicClient(Boolean.FALSE);
            clientRep.setBearerOnly(Boolean.FALSE);
        });

        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Den Forste Profilen")
                        .addExecutor(TestEnhancedPluggableTokenManagerExecutorFactory.PROVIDER_ID, null)
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Primera Plitica", Boolean.TRUE)
                        .addCondition(ClientAccessTypeConditionFactory.PROVIDER_ID,
                                createClientAccessTypeConditionConfig(Arrays.asList(ClientAccessTypeConditionFactory.TYPE_CONFIDENTIAL)))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        oauth.clientId(clientId);
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

        events.expectLogin().client(clientId).assertEvent();
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, clientSecret);
        assertEquals(200, response.getStatusCode());
        AccessToken token = oauth.verifyToken(response.getAccessToken());
        assertNull(token.getSubject());
    }
}

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

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import jakarta.ws.rs.BadRequestException;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientSecretAuthenticator;
import org.keycloak.authentication.authenticators.client.X509ClientAuthenticator;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.common.Profile;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.CibaConfig;
import org.keycloak.models.Constants;
import org.keycloak.models.OAuth2DeviceConfig;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.OIDCResponseMode;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.AuthorizationResponseToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientRolesConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdaterContextConditionFactory;
import org.keycloak.services.clientpolicy.executor.SecureClientAuthenticationAssertionExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureClientAuthenticatorExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureClientUrisExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureLogoutExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureParContentsExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureRequestObjectExecutor;
import org.keycloak.services.clientpolicy.executor.SecureRequestObjectExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureResponseTypeExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureSessionEnforceExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureSigningAlgorithmExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureSigningAlgorithmForSignedJwtExecutorFactory;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.client.resources.TestApplicationResourceUrls;
import org.keycloak.testsuite.client.resources.TestOIDCEndpointsApplicationResource;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LogoutConfirmPage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.rest.resource.TestingOIDCEndpointsApplicationResource.AuthorizationEndpointRequestObject;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientPoliciesBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientPolicyBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientProfileBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientProfilesBuilder;
import org.keycloak.testsuite.util.RoleBuilder;
import org.keycloak.testsuite.util.SignatureSignerUtil;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.OAuthClient;
import org.keycloak.testsuite.util.oauth.ParResponse;
import org.keycloak.testsuite.util.oauth.PkceGenerator;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Test;

import static org.keycloak.testsuite.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createAnyClientConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createClientRolesConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createClientUpdateContextConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createSecureClientAuthenticatorExecutorConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createSecureRequestObjectExecutorConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createSecureResponseTypeExecutor;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createSecureSigningAlgorithmEnforceExecutorConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createSecureSigningAlgorithmForSignedJwtEnforceExecutorConfig;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * This test class is for testing an executor of client policies.
 * 
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
@EnableFeature(value = Profile.Feature.CLIENT_SECRET_ROTATION)
public class ClientPoliciesExecutorTest extends AbstractClientPoliciesTest {

    @Page
    protected OAuthGrantPage grantPage;

    @Page
    protected AppPage appPage;

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
        user.setGroups(List.of("topGroup")); // defined in testrealm.json

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

    // Tests that secured client authenticator is enforced also during client authentication itself (during token request after successful login)
    @Test
    public void testSecureClientAuthenticatorDuringLogin() throws Exception {
        // register profile to NOT allow authentication with ClientIdAndSecret
        String profileName = "MyProfile";
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(profileName, "Primum Profile")
                        .addExecutor(SecureClientAuthenticatorExecutorFactory.PROVIDER_ID,
                                createSecureClientAuthenticatorExecutorConfig(
                                        Arrays.asList(JWTClientAuthenticator.PROVIDER_ID, JWTClientSecretAuthenticator.PROVIDER_ID, X509ClientAuthenticator.PROVIDER_ID),
                                        null))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register role policy
        String roleAlphaName = "sample-client-role-alpha";
        String roleZetaName = "sample-client-role-zeta";
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Den Forste Politikken", Boolean.TRUE)
                        .addCondition(ClientRolesConditionFactory.PROVIDER_ID,
                                createClientRolesConditionConfig(Arrays.asList(roleAlphaName, roleZetaName)))
                        .addProfile(profileName)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // create a client without client role. It should be successful (policy not applied)
        String clientId = generateSuffixedName(CLIENT_NAME);
        createClientByAdmin(clientId, (ClientRepresentation clientRep) -> clientRep.setSecret("secret"));

        // Login with clientIdAndSecret. It should be successful (policy not applied)
        successfulLoginAndLogout(clientId, "secret");

        // Add role to the client
        ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm(REALM_NAME), clientId);
        assert clientResource != null;
        ClientRepresentation clientRep = clientResource.toRepresentation();
        Assert.assertEquals(ClientIdAndSecretAuthenticator.PROVIDER_ID, clientRep.getClientAuthenticatorType());
        clientResource.roles().create(RoleBuilder.create().name(roleAlphaName).build());

        // Not allowed to client authentication with clientIdAndSecret anymore. Client matches policy now
        oauth.client(clientId, "secret");
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse res = oauth.doAccessTokenRequest(code);
        assertEquals(400, res.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_GRANT, res.getError());
        assertEquals("Configured client authentication method not allowed for client", res.getErrorDescription());
    }

    @Test
    public void testSecureResponseTypeExecutor() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "O Primeiro Perfil")
                        .addExecutor(SecureResponseTypeExecutorFactory.PROVIDER_ID, null)
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "A Primeira Politica", Boolean.TRUE)
                        .addCondition(ClientRolesConditionFactory.PROVIDER_ID,
                                createClientRolesConditionConfig(List.of(SAMPLE_CLIENT_ROLE)))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        String clientId = generateSuffixedName(CLIENT_NAME);
        String clientSecret = "secret";
        String cid = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret(clientSecret);
            clientRep.setStandardFlowEnabled(Boolean.TRUE);
            clientRep.setImplicitFlowEnabled(Boolean.TRUE);
            clientRep.setPublicClient(Boolean.FALSE);
        });
        adminClient.realm(REALM_NAME).clients().get(cid).roles().create(RoleBuilder.create().name(SAMPLE_CLIENT_ROLE).build());

        oauth.client(clientId, clientSecret);
        oauth.openLoginForm();

        AuthorizationEndpointResponse authorizationEndpointResponse = oauth.parseLoginResponse();
        assertEquals(OAuthErrorException.INVALID_REQUEST, authorizationEndpointResponse.getError());
        assertEquals("invalid response_type", authorizationEndpointResponse.getErrorDescription());
        events.expectClientPolicyError(EventType.LOGIN_ERROR, OAuthErrorException.INVALID_REQUEST,
                        Details.CLIENT_POLICY_ERROR, OAuthErrorException.INVALID_REQUEST,
                        "invalid response_type").client(clientId).user((String) null).assertEvent();

        oauth.responseType(OIDCResponseType.CODE + " " + OIDCResponseType.ID_TOKEN);
        oauth.loginForm().nonce("vbwe566fsfffds").doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

        EventRepresentation loginEvent = events.expectLogin().client(clientId).assertEvent();
        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse res = oauth.doAccessTokenRequest(code);
        assertEquals(200, res.getStatusCode());
        events.expectCodeToToken(codeId, sessionId).client(clientId).assertEvent();

        oauth.doLogout(res.getRefreshToken());
        events.expectLogout(sessionId).client(clientId).clearDetails().assertEvent();

        // update profiles
        json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "O Primeiro Perfil")
                        .addExecutor(SecureResponseTypeExecutorFactory.PROVIDER_ID, createSecureResponseTypeExecutor(Boolean.FALSE, Boolean.TRUE))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        oauth.responseType(OIDCResponseType.CODE + " " + OIDCResponseType.ID_TOKEN + " " + OIDCResponseType.TOKEN); // token response type allowed
        oauth.loginForm().nonce("cie8cjcwiw").doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

        loginEvent = events.expectLogin().client(clientId).assertEvent();
        sessionId = loginEvent.getSessionId();
        codeId = loginEvent.getDetails().get(Details.CODE_ID);
        code = oauth.parseLoginResponse().getCode();
        res = oauth.doAccessTokenRequest(code);
        assertEquals(200, res.getStatusCode());
        events.expectCodeToToken(codeId, sessionId).client(clientId).assertEvent();

        oauth.doLogout(res.getRefreshToken());
        events.expectLogout(sessionId).client(clientId).clearDetails().assertEvent();

        // shall allow code using response_mode jwt
        oauth.responseType(OIDCResponseType.CODE);
        oauth.responseMode("jwt");
        AuthorizationEndpointResponse authzResponse = oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
        String jwsResponse = authzResponse.getResponse();
        AuthorizationResponseToken responseObject = oauth.verifyAuthorizationResponseToken(jwsResponse);
        code = (String) responseObject.getOtherClaims().get(OAuth2Constants.CODE);
        res = oauth.doAccessTokenRequest(code);
        assertEquals(200, res.getStatusCode());

        // update profiles
        json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "O Primeiro Perfil")
                        .addExecutor(SecureResponseTypeExecutorFactory.PROVIDER_ID, createSecureResponseTypeExecutor(Boolean.FALSE, Boolean.FALSE))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        oauth.openLogoutForm();
        oauth.responseType(OIDCResponseType.CODE + " " + OIDCResponseType.ID_TOKEN + " " + OIDCResponseType.TOKEN); // token response type allowed
        oauth.responseMode("jwt");
        oauth.openLoginForm();
        final JWSInput errorJws = new JWSInput(oauth.parseLoginResponse().getResponse());
        JsonNode errorClaims = JsonSerialization.readValue(errorJws.getContent(), JsonNode.class);
        assertEquals(OAuthErrorException.INVALID_REQUEST, errorClaims.get("error").asText());
    }

    @Test
    public void testSecureResponseTypeExecutorAllowTokenResponseType() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "O Primeiro Perfil")
                        .addExecutor(SecureResponseTypeExecutorFactory.PROVIDER_ID, createSecureResponseTypeExecutor(null, Boolean.TRUE))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Den Forsta Policyn", Boolean.TRUE)
                        .addCondition(ClientUpdaterContextConditionFactory.PROVIDER_ID,
                                createClientUpdateContextConditionConfig(Arrays.asList(
                                        ClientUpdaterContextConditionFactory.BY_AUTHENTICATED_USER,
                                        ClientUpdaterContextConditionFactory.BY_INITIAL_ACCESS_TOKEN,
                                        ClientUpdaterContextConditionFactory.BY_REGISTRATION_ACCESS_TOKEN)))
                        .addCondition(ClientRolesConditionFactory.PROVIDER_ID,
                                createClientRolesConditionConfig(List.of(SAMPLE_CLIENT_ROLE)))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // create by Admin REST API
        try {
            createClientByAdmin(generateSuffixedName("App-by-Admin"), (ClientRepresentation clientRep) -> clientRep.setSecret("secret"));
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getMessage());
        }

        // update profiles
        json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "O Primeiro Perfil")
                        .addExecutor(SecureResponseTypeExecutorFactory.PROVIDER_ID, createSecureResponseTypeExecutor(Boolean.TRUE, null))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        String cId = null;
        String clientId = generateSuffixedName(CLIENT_NAME);
        String clientSecret = "secret";
        try {
            cId = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
                clientRep.setSecret(clientSecret);
                clientRep.setStandardFlowEnabled(Boolean.TRUE);
                clientRep.setImplicitFlowEnabled(Boolean.TRUE);
                clientRep.setPublicClient(Boolean.FALSE);
            });
        } catch (ClientPolicyException e) {
            fail();
        }
        ClientRepresentation cRep = getClientByAdmin(cId);
        assertEquals(Boolean.TRUE.toString(), cRep.getAttributes().get(OIDCConfigAttributes.ID_TOKEN_AS_DETACHED_SIGNATURE));

        adminClient.realm(REALM_NAME).clients().get(cId).roles().create(RoleBuilder.create().name(SAMPLE_CLIENT_ROLE).build());

        oauth.client(clientId, clientSecret);
        oauth.openLoginForm();
        AuthorizationEndpointResponse authorizationEndpointResponse = oauth.parseLoginResponse();
        assertEquals(OAuthErrorException.INVALID_REQUEST, authorizationEndpointResponse.getError());
        assertEquals("invalid response_type", authorizationEndpointResponse.getErrorDescription());
        events.expectClientPolicyError(EventType.LOGIN_ERROR, OAuthErrorException.INVALID_REQUEST,
                Details.CLIENT_POLICY_ERROR, OAuthErrorException.INVALID_REQUEST,
                "invalid response_type").client(clientId).user((String) null).assertEvent();

        oauth.responseType(OIDCResponseType.CODE + " " + OIDCResponseType.ID_TOKEN);
        oauth.loginForm().nonce("LIVieviDie028f").doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

        EventRepresentation loginEvent = events.expectLogin().client(clientId).assertEvent();
        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);
        authorizationEndpointResponse = oauth.parseLoginResponse();
        String code = authorizationEndpointResponse.getCode();

        IDToken idToken = oauth.verifyIDToken(authorizationEndpointResponse.getIdToken());
        // confirm ID token as detached signature does not include authenticated user's claims
        Assert.assertNull(idToken.getEmailVerified());
        Assert.assertNull(idToken.getName());
        Assert.assertNull(idToken.getPreferredUsername());
        Assert.assertNull(idToken.getGivenName());
        Assert.assertNull(idToken.getFamilyName());
        Assert.assertNull(idToken.getEmail());
        assertEquals("LIVieviDie028f", idToken.getNonce());
        // confirm an access token not returned
        Assert.assertNull(authorizationEndpointResponse.getAccessToken());

        AccessTokenResponse res = oauth.doAccessTokenRequest(code);
        assertEquals(200, res.getStatusCode());
        events.expectCodeToToken(codeId, sessionId).client(clientId).assertEvent();

        oauth.doLogout(res.getRefreshToken());
        events.expectLogout(sessionId).client(clientId).clearDetails().assertEvent();
    }

    @Test
    public void testSecureRequestObjectExecutor() throws Exception {
        Integer availablePeriod = SecureRequestObjectExecutor.DEFAULT_AVAILABLE_PERIOD + 400;
        Integer allowedClockSkew = SecureRequestObjectExecutor.DEAULT_ALLOWED_CLOCK_SKEW + 15; // 30 sec

        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Prvy Profil")
                        .addExecutor(SecureRequestObjectExecutorFactory.PROVIDER_ID,
                                createSecureRequestObjectExecutorConfig(availablePeriod, null, null, allowedClockSkew))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Prva Politika", Boolean.TRUE)
                        .addCondition(ClientRolesConditionFactory.PROVIDER_ID,
                                createClientRolesConditionConfig(List.of(SAMPLE_CLIENT_ROLE)))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        String clientId = generateSuffixedName(CLIENT_NAME);
        String clientSecret = "secret";
        String cid = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret(clientSecret);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestUris(Collections.singletonList(TestApplicationResourceUrls.clientRequestUri()));
        });
        adminClient.realm(REALM_NAME).clients().get(cid).roles().create(RoleBuilder.create().name(SAMPLE_CLIENT_ROLE).build());

        oauth.client(clientId);
        AuthorizationEndpointRequestObject requestObject;

        // check whether request object exists
        oauth.openLoginForm();
        AuthorizationEndpointResponse authorizationEndpointResponse = oauth.parseLoginResponse();
        assertEquals(OAuthErrorException.INVALID_REQUEST, authorizationEndpointResponse.getError());
        assertEquals("Missing parameter: 'request' or 'request_uri'", authorizationEndpointResponse.getErrorDescription());
        events.expectClientPolicyError(EventType.LOGIN_ERROR, OAuthErrorException.INVALID_REQUEST,
                Details.CLIENT_POLICY_ERROR, OAuthErrorException.INVALID_REQUEST,
                "Missing parameter: 'request' or 'request_uri'").client(clientId).user((String) null)
                .assertEvent();

        // check whether request_uri is https scheme
        // cannot test because existing AuthorizationEndpoint check and return error before executing client policy

        // check whether request object can be retrieved from request_uri
        // cannot test because existing AuthorizationEndpoint check and return error before executing client policy

        // check whether request object can be parsed successfully
        // cannot test because existing AuthorizationEndpoint check and return error before executing client policy

        // check whether scope exists in both query parameter and request object
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.setScope(null);
        registerRequestObject(requestObject, clientId, Algorithm.ES256, true);
        oauth.loginForm().requestUri(requestUri).open();
        authorizationEndpointResponse = oauth.parseLoginResponse();
        assertEquals(OAuthErrorException.INVALID_REQUEST, authorizationEndpointResponse.getError());
        assertEquals("Invalid parameter. Parameters in 'request' object not matching with request parameters", authorizationEndpointResponse.getErrorDescription());
        events.expectClientPolicyError(EventType.LOGIN_ERROR, OAuthErrorException.INVALID_REQUEST,
                Details.CLIENT_POLICY_ERROR, OAuthErrorException.INVALID_REQUEST,
                "Invalid parameter. Parameters in 'request' object not matching with request parameters")
                .client(clientId).user((String) null).assertEvent();

        // check whether client_id exists in both query parameter and request object
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.setClientId(null);
        registerRequestObject(requestObject, clientId, Algorithm.ES256, true);
        oauth.loginForm().requestUri(requestUri).open();
        authorizationEndpointResponse = oauth.parseLoginResponse();
        assertEquals(OAuthErrorException.INVALID_REQUEST, authorizationEndpointResponse.getError());
        assertEquals("Invalid parameter. Parameters in 'request' object not matching with request parameters", authorizationEndpointResponse.getErrorDescription());
        events.expectClientPolicyError(EventType.LOGIN_ERROR, OAuthErrorException.INVALID_REQUEST,
                Details.CLIENT_POLICY_ERROR, OAuthErrorException.INVALID_REQUEST,
                "Invalid parameter. Parameters in 'request' object not matching with request parameters")
                .client(clientId).user((String) null).assertEvent();

        // check whether response_type exists in both query parameter and request object
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.setResponseType(null);
        registerRequestObject(requestObject, clientId, Algorithm.ES256, true);
        oauth.loginForm().requestUri(requestUri).open();
        authorizationEndpointResponse = oauth.parseLoginResponse();
        assertEquals(OAuthErrorException.INVALID_REQUEST, authorizationEndpointResponse.getError());
        assertEquals("Invalid parameter. Parameters in 'request' object not matching with request parameters", authorizationEndpointResponse.getErrorDescription());
        events.expectClientPolicyError(EventType.LOGIN_ERROR, OAuthErrorException.INVALID_REQUEST,
                Details.CLIENT_POLICY_ERROR, OAuthErrorException.INVALID_REQUEST,
                "Invalid parameter. Parameters in 'request' object not matching with request parameters")
                .client(clientId).user((String) null).assertEvent();

        // Check scope required
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.setScope(null);
        registerRequestObject(requestObject, clientId, Algorithm.ES256, true);
        oauth.scope(null);
        oauth.openid(false);
        oauth.loginForm().requestUri(requestUri).open();
        authorizationEndpointResponse = oauth.parseLoginResponse();
        assertEquals(OAuthErrorException.INVALID_REQUEST, authorizationEndpointResponse.getError());
        assertEquals("Parameter 'scope' missing in the request parameters or in 'request' object", authorizationEndpointResponse.getErrorDescription());
        events.expectClientPolicyError(EventType.LOGIN_ERROR, OAuthErrorException.INVALID_REQUEST,
                Details.CLIENT_POLICY_ERROR, OAuthErrorException.INVALID_REQUEST,
                "Parameter 'scope' missing in the request parameters or in 'request' object")
                .client(clientId).user((String) null).assertEvent();
        oauth.openid(true);

        // check whether "exp" claim exists
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.exp(null);
        registerRequestObject(requestObject, clientId, Algorithm.ES256, false);
        oauth.loginForm().request(request).open();
        authorizationEndpointResponse = oauth.parseLoginResponse();
        assertEquals(OAuthErrorException.INVALID_REQUEST_OBJECT, authorizationEndpointResponse.getError());
        assertEquals("Missing parameter in the 'request' object: exp", authorizationEndpointResponse.getErrorDescription());
        events.expectClientPolicyError(EventType.LOGIN_ERROR, OAuthErrorException.INVALID_REQUEST_OBJECT,
                Details.CLIENT_POLICY_ERROR, OAuthErrorException.INVALID_REQUEST_OBJECT,
                "Missing parameter in the 'request' object: exp").client(clientId).user((String) null)
                .assertEvent();

        // check whether request object not expired
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.exp(0L);
        registerRequestObject(requestObject, clientId, Algorithm.ES256, true);
        oauth.loginForm().requestUri(requestUri).open();
        authorizationEndpointResponse = oauth.parseLoginResponse();
        assertEquals(OAuthErrorException.INVALID_REQUEST_OBJECT, authorizationEndpointResponse.getError());
        assertEquals("Request Expired", authorizationEndpointResponse.getErrorDescription());
        events.expectClientPolicyError(EventType.LOGIN_ERROR, OAuthErrorException.INVALID_REQUEST_OBJECT,
                Details.CLIENT_POLICY_ERROR, OAuthErrorException.INVALID_REQUEST_OBJECT,
                "Request Expired").client(clientId).user((String) null).assertEvent();

        // check whether "nbf" claim exists
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.nbf(null);
        registerRequestObject(requestObject, clientId, Algorithm.ES256, false);
        oauth.loginForm().request(request).open();
        authorizationEndpointResponse = oauth.parseLoginResponse();
        assertEquals(OAuthErrorException.INVALID_REQUEST_OBJECT, authorizationEndpointResponse.getError());
        assertEquals("Missing parameter in the 'request' object: nbf", authorizationEndpointResponse.getErrorDescription());
        events.expectClientPolicyError(EventType.LOGIN_ERROR, OAuthErrorException.INVALID_REQUEST_OBJECT,
                Details.CLIENT_POLICY_ERROR, OAuthErrorException.INVALID_REQUEST_OBJECT,
                "Missing parameter in the 'request' object: nbf").client(clientId).user((String) null)
                .assertEvent();

        // check whether request object not yet being processed
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.nbf(requestObject.getNbf() + allowedClockSkew + 10);
        registerRequestObject(requestObject, clientId, Algorithm.ES256, false);
        oauth.loginForm().request(request).open();
        authorizationEndpointResponse = oauth.parseLoginResponse();
        assertEquals(OAuthErrorException.INVALID_REQUEST_OBJECT, authorizationEndpointResponse.getError());
        assertEquals("Request not yet being processed", authorizationEndpointResponse.getErrorDescription());
        events.expectClientPolicyError(EventType.LOGIN_ERROR, OAuthErrorException.INVALID_REQUEST_OBJECT,
                Details.CLIENT_POLICY_ERROR, OAuthErrorException.INVALID_REQUEST_OBJECT,
                "Request not yet being processed").client(clientId).user((String) null).assertEvent();

        // nbf ahead within allowed clock skew
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.nbf(requestObject.getNbf() + allowedClockSkew - 10);
        registerRequestObject(requestObject, clientId, Algorithm.ES256, false);
        successfulLoginAndLogout(clientId, clientSecret);

        // check whether request object issued in the future
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.iat(requestObject.getIat() + allowedClockSkew + 10);
        registerRequestObject(requestObject, clientId, Algorithm.ES256, false);
        oauth.loginForm().request(request).open();
        authorizationEndpointResponse = oauth.parseLoginResponse();
        assertEquals(OAuthErrorException.INVALID_REQUEST_OBJECT, authorizationEndpointResponse.getError());
        assertEquals("Request issued in the future", authorizationEndpointResponse.getErrorDescription());
        events.expectClientPolicyError(EventType.LOGIN_ERROR, OAuthErrorException.INVALID_REQUEST_OBJECT,
                Details.CLIENT_POLICY_ERROR, OAuthErrorException.INVALID_REQUEST_OBJECT,
                "Request issued in the future").client(clientId).user((String) null).assertEvent();

        // iat ahead within allowed clock skew
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.iat(requestObject.getIat() + allowedClockSkew - 10);
        registerRequestObject(requestObject, clientId, Algorithm.ES256, false);
        successfulLoginAndLogout(clientId, clientSecret);
        
        // check whether request object's available period is short
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.exp(requestObject.getNbf() + availablePeriod + 1);
        registerRequestObject(requestObject, clientId, Algorithm.ES256, false);
        oauth.loginForm().request(request).open();
        authorizationEndpointResponse = oauth.parseLoginResponse();
        assertEquals(OAuthErrorException.INVALID_REQUEST_OBJECT, authorizationEndpointResponse.getError());
        assertEquals("Request's available period is long", authorizationEndpointResponse.getErrorDescription());
        events.expectClientPolicyError(EventType.LOGIN_ERROR, OAuthErrorException.INVALID_REQUEST_OBJECT,
                Details.CLIENT_POLICY_ERROR, OAuthErrorException.INVALID_REQUEST_OBJECT,
                "Request's available period is long").client(clientId).user((String) null)
                .assertEvent();

        // check whether "aud" claim exists
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.audience((String) null);
        registerRequestObject(requestObject, clientId, Algorithm.ES256, false);
        oauth.loginForm().request(request).open();
        authorizationEndpointResponse = oauth.parseLoginResponse();
        assertEquals(OAuthErrorException.INVALID_REQUEST_OBJECT, authorizationEndpointResponse.getError());
        assertEquals("Missing parameter in the 'request' object: aud", authorizationEndpointResponse.getErrorDescription());
        events.expectClientPolicyError(EventType.LOGIN_ERROR, OAuthErrorException.INVALID_REQUEST_OBJECT,
                        Details.CLIENT_POLICY_ERROR, OAuthErrorException.INVALID_REQUEST_OBJECT,
                        "Missing parameter in the 'request' object: aud").client(clientId)
                .user((String) null).assertEvent();

        // check whether "aud" claim points to this keycloak as authz server
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.audience(suiteContext.getAuthServerInfo().getContextRoot().toString());
        registerRequestObject(requestObject, clientId, Algorithm.ES256, true);
        oauth.loginForm().requestUri(requestUri).open();
        authorizationEndpointResponse = oauth.parseLoginResponse();
        assertEquals(OAuthErrorException.INVALID_REQUEST_URI, authorizationEndpointResponse.getError());
        assertEquals("Invalid parameter in the 'request' object: aud", authorizationEndpointResponse.getErrorDescription());
        events.expectClientPolicyError(EventType.LOGIN_ERROR, OAuthErrorException.INVALID_REQUEST_URI,
                        Details.CLIENT_POLICY_ERROR, OAuthErrorException.INVALID_REQUEST_URI,
                        "Invalid parameter in the 'request' object: aud").client(clientId)
                .user((String) null).assertEvent();

        // confirm whether all parameters in query string are included in the request object, and have the same values
        // argument "request" are parameters overridden by parameters in request object
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.setState("notmatchstate");
        registerRequestObject(requestObject, clientId, Algorithm.ES256, false);
        oauth.loginForm().state("wrongstate").request(request).open();
        authorizationEndpointResponse = oauth.parseLoginResponse();
        assertEquals(OAuthErrorException.INVALID_REQUEST, authorizationEndpointResponse.getError());
        assertEquals("Invalid parameter. Parameters in 'request' object not matching with request parameters", authorizationEndpointResponse.getErrorDescription());
        events.expectClientPolicyError(EventType.LOGIN_ERROR, OAuthErrorException.INVALID_REQUEST,
                        Details.CLIENT_POLICY_ERROR, OAuthErrorException.INVALID_REQUEST,
                        "Invalid parameter. Parameters in 'request' object not matching with request parameters")
                .client(clientId).user((String) null).assertEvent();

        // valid request object
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        registerRequestObject(requestObject, clientId, Algorithm.ES256, true);

        successfulLoginAndLogout(clientId, clientSecret);

        // update profile : no configuration - "nbf" check and available period is 3600 sec
        json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Prvy Profil")
                        .addExecutor(SecureRequestObjectExecutorFactory.PROVIDER_ID, null)
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // check whether "nbf" claim exists
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.nbf(null);
        registerRequestObject(requestObject, clientId, Algorithm.ES256, false);
        oauth.loginForm().request(request).open();
        authorizationEndpointResponse = oauth.parseLoginResponse();
        assertEquals(OAuthErrorException.INVALID_REQUEST_OBJECT, authorizationEndpointResponse.getError());
        assertEquals("Missing parameter in the 'request' object: nbf", authorizationEndpointResponse.getErrorDescription());
        events.expectClientPolicyError(EventType.LOGIN_ERROR, OAuthErrorException.INVALID_REQUEST_OBJECT,
                        Details.CLIENT_POLICY_ERROR, OAuthErrorException.INVALID_REQUEST_OBJECT,
                        "Missing parameter in the 'request' object: nbf").client(clientId)
                .user((String) null).assertEvent();

        // check whether request object not yet being processed
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.nbf(requestObject.getNbf() + 600);
        registerRequestObject(requestObject, clientId, Algorithm.ES256, false);
        oauth.loginForm().request(request).open();
        authorizationEndpointResponse = oauth.parseLoginResponse();
        assertEquals(OAuthErrorException.INVALID_REQUEST_OBJECT, authorizationEndpointResponse.getError());
        assertEquals("Request not yet being processed", authorizationEndpointResponse.getErrorDescription());
        events.expectClientPolicyError(EventType.LOGIN_ERROR, OAuthErrorException.INVALID_REQUEST_OBJECT,
                        Details.CLIENT_POLICY_ERROR, OAuthErrorException.INVALID_REQUEST_OBJECT,
                        "Request not yet being processed").client(clientId).user((String) null)
                .assertEvent();

        // check whether request object's available period is short
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.exp(requestObject.getNbf() + SecureRequestObjectExecutor.DEFAULT_AVAILABLE_PERIOD + 1);
        registerRequestObject(requestObject, clientId, Algorithm.ES256, false);
        oauth.loginForm().request(request).open();
        authorizationEndpointResponse = oauth.parseLoginResponse();
        assertEquals(OAuthErrorException.INVALID_REQUEST_OBJECT, authorizationEndpointResponse.getError());
        assertEquals("Request's available period is long", authorizationEndpointResponse.getErrorDescription());
        events.expectClientPolicyError(EventType.LOGIN_ERROR, OAuthErrorException.INVALID_REQUEST_OBJECT,
                        Details.CLIENT_POLICY_ERROR, OAuthErrorException.INVALID_REQUEST_OBJECT,
                        "Request's available period is long").client(clientId).user((String) null)
                .assertEvent();

        // update profile : not check "nbf"
        json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Prvy Profil")
                        .addExecutor(SecureRequestObjectExecutorFactory.PROVIDER_ID,
                                createSecureRequestObjectExecutorConfig(null, Boolean.FALSE))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // not check whether "nbf" claim exists
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.nbf(null);
        registerRequestObject(requestObject, clientId, Algorithm.ES256, false);
        successfulLoginAndLogout(clientId, clientSecret);

        // not check whether request object not yet being processed
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.nbf(requestObject.getNbf() + 600);
        registerRequestObject(requestObject, clientId, Algorithm.ES256, false);
        successfulLoginAndLogout(clientId, clientSecret);

        // not check whether request object's available period is short
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.exp(requestObject.getNbf() + SecureRequestObjectExecutor.DEFAULT_AVAILABLE_PERIOD + 1);
        registerRequestObject(requestObject, clientId, Algorithm.ES256, false);
        successfulLoginAndLogout(clientId, clientSecret);

        // update profile : force request object encryption
        json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Prvy Profil")
                        .addExecutor(SecureRequestObjectExecutorFactory.PROVIDER_ID, createSecureRequestObjectExecutorConfig(null, null, true))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        registerRequestObject(requestObject, clientId, Algorithm.ES256, false);
        oauth.loginForm().request(request).open();
        authorizationEndpointResponse = oauth.parseLoginResponse();
        assertEquals(OAuthErrorException.INVALID_REQUEST_OBJECT, authorizationEndpointResponse.getError());
        assertEquals("Request object not encrypted", authorizationEndpointResponse.getErrorDescription());
        events.expectClientPolicyError(EventType.LOGIN_ERROR, OAuthErrorException.INVALID_REQUEST_OBJECT,
                        Details.CLIENT_POLICY_ERROR, OAuthErrorException.INVALID_REQUEST_OBJECT,
                        "Request object not encrypted").client(clientId).user((String) null)
                .assertEvent();
    }

    @Test
    public void testParSecureRequestObjectExecutor() throws Exception {
        Integer availablePeriod = SecureRequestObjectExecutor.DEFAULT_AVAILABLE_PERIOD + 400;
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Prvy Profil")
                        .addExecutor(SecureRequestObjectExecutorFactory.PROVIDER_ID,
                                createSecureRequestObjectExecutorConfig(availablePeriod, true))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Prva Politika", Boolean.TRUE)
                        .addCondition(ClientRolesConditionFactory.PROVIDER_ID,
                                createClientRolesConditionConfig(List.of(SAMPLE_CLIENT_ROLE)))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        String clientId = generateSuffixedName(CLIENT_NAME);
        String clientSecret = "secret";
        String cid = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret(clientSecret);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestUris(Collections.singletonList(TestApplicationResourceUrls.clientRequestUri()));
        });

        oauth.realm(REALM_NAME);
        oauth.client(clientId, clientSecret);

        adminClient.realm(REALM_NAME).clients().get(cid).roles().create(RoleBuilder.create().name(SAMPLE_CLIENT_ROLE).build());

        AuthorizationEndpointRequestObject requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);

        ParResponse pResp = oauth.pushedAuthorizationRequest().request(signRequestObject(requestObject)).send();
        assertEquals(201, pResp.getStatusCode());
        String requestUri = pResp.getRequestUri();

        oauth.scope(null);
        oauth.responseType(null);
        AuthorizationEndpointResponse loginResponse = oauth.loginForm().requestUri(requestUri).doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
        assertNotNull(loginResponse.getCode());
        oauth.openLogoutForm();

        requestObject.exp(null);
        pResp = oauth.pushedAuthorizationRequest().request(signRequestObject(requestObject)).send();
        requestUri = pResp.getRequestUri();
        oauth.loginForm().requestUri(requestUri).open();
        assertEquals(OAuthErrorException.INVALID_REQUEST_URI, oauth.parseLoginResponse().getError());

        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.nbf(null);
        pResp = oauth.pushedAuthorizationRequest().request(signRequestObject(requestObject)).send();
        requestUri = pResp.getRequestUri();
        oauth.loginForm().requestUri(requestUri).open();
        assertEquals(OAuthErrorException.INVALID_REQUEST_URI, oauth.parseLoginResponse().getError());

        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.audience("https://www.other1.example.com/");
        pResp = oauth.pushedAuthorizationRequest().request(signRequestObject(requestObject)).send();
        requestUri = pResp.getRequestUri();
        oauth.loginForm().requestUri(requestUri).open();
        assertEquals(OAuthErrorException.INVALID_REQUEST_URI, oauth.parseLoginResponse().getError());

        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.setOtherClaims(OIDCLoginProtocol.REQUEST_URI_PARAM, "foo");
        pResp = oauth.pushedAuthorizationRequest().request(signRequestObject(requestObject)).send();
        assertEquals(OAuthErrorException.INVALID_REQUEST_OBJECT, pResp.getError());
    }

    @Test
    public void testSecureSessionEnforceExecutor() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Den Forste Profilen")
                        .addExecutor(SecureSessionEnforceExecutorFactory.PROVIDER_ID, null)
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        String roleAlphaName = "sample-client-role-alpha";
        String roleBetaName = "sample-client-role-beta";
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Den Forste Politikken", Boolean.TRUE)
                        .addCondition(ClientRolesConditionFactory.PROVIDER_ID,
                                createClientRolesConditionConfig(List.of(roleBetaName)))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        String clientAlphaId = generateSuffixedName("Alpha-App");
        String clientAlphaSecret = "secretAlpha";
        String cAlphaId = createClientByAdmin(clientAlphaId, (ClientRepresentation clientRep) -> clientRep.setSecret(clientAlphaSecret));
        adminClient.realm(REALM_NAME).clients().get(cAlphaId).roles().create(RoleBuilder.create().name(roleAlphaName).build());

        String clientBetaId = generateSuffixedName("Beta-App");
        String clientBetaSecret = "secretBeta";
        String cBetaId = createClientByAdmin(clientBetaId, (ClientRepresentation clientRep) -> clientRep.setSecret(clientBetaSecret));
        adminClient.realm(REALM_NAME).clients().get(cBetaId).roles().create(RoleBuilder.create().name(roleBetaName).build());

        successfulLoginAndLogout(clientAlphaId, clientAlphaSecret);

        oauth.openid(false);
        successfulLoginAndLogout(clientAlphaId, clientAlphaSecret);

        oauth.openid(true);
        failLoginWithoutSecureSessionParameter(clientBetaId, ERR_MSG_MISSING_NONCE);

        successfulLoginAndLogout(clientBetaId, clientBetaSecret, "yesitisnonce", "somestate");

        oauth.openid(false);
        failLoginWithoutSecureSessionParameter(clientBetaId, ERR_MSG_MISSING_STATE);

        successfulLoginAndLogout(clientBetaId, clientBetaSecret, "somenonce", "somestate");
    }

    // GH issue 37447
    @Test
    public void testSecureSessionEnforceExecutorWithAccountConsole() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Den Forste Profilen")
                        .addExecutor(SecureSessionEnforceExecutorFactory.PROVIDER_ID, null)
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Den Forste Politikken", Boolean.TRUE)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID,
                                createAnyClientConditionConfig())
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // Test account-console is loaded successfully when "secure-session-enforce" executor is present
        oauth.client(Constants.ACCOUNT_CONSOLE_CLIENT_ID)
                .redirectUri(OAuthClient.AUTH_SERVER_ROOT + "/realms/test/account/")
                .responseMode(OIDCResponseMode.QUERY.value())
                .loginForm()
                .state(KeycloakModelUtils.generateId())
                .nonce(KeycloakModelUtils.generateId())
                .codeChallenge(PkceGenerator.s256())
                .open();
        loginPage.assertCurrent();
        Assert.assertEquals("Sign in to your account", loginPage.getTitleText());
    }

    @Test
    public void testSecureSigningAlgorithmEnforceExecutor() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Den Forsta Profilen")
                        .addExecutor(SecureSigningAlgorithmExecutorFactory.PROVIDER_ID, null)
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Den Forsta Policyn", Boolean.TRUE)
                        .addCondition(ClientUpdaterContextConditionFactory.PROVIDER_ID,
                                createClientUpdateContextConditionConfig(Arrays.asList(
                                        ClientUpdaterContextConditionFactory.BY_AUTHENTICATED_USER,
                                        ClientUpdaterContextConditionFactory.BY_INITIAL_ACCESS_TOKEN,
                                        ClientUpdaterContextConditionFactory.BY_REGISTRATION_ACCESS_TOKEN)))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // create by Admin REST API - fail
        try {
            createClientByAdmin(generateSuffixedName("App-by-Admin"), (ClientRepresentation clientRep) -> {
                clientRep.setSecret("secret");
                clientRep.setAttributes(new HashMap<>());
                clientRep.getAttributes().put(OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG, "none");
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_REQUEST, e.getMessage());
        }

        // create by Admin REST API - success
        String cAppAdminId = createClientByAdmin(generateSuffixedName("App-by-Admin"), (ClientRepresentation clientRep) -> {
            clientRep.setAttributes(new HashMap<>());
            clientRep.getAttributes().put(OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG, Algorithm.PS256);
            clientRep.getAttributes().put(OIDCConfigAttributes.REQUEST_OBJECT_SIGNATURE_ALG, Algorithm.ES256);
            clientRep.getAttributes().put(OIDCConfigAttributes.ID_TOKEN_SIGNED_RESPONSE_ALG, Algorithm.ES256);
            clientRep.getAttributes().put(OIDCConfigAttributes.TOKEN_ENDPOINT_AUTH_SIGNING_ALG, Algorithm.ES256);
            clientRep.getAttributes().put(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG, Algorithm.ES256);
        });

        // create by Admin REST API - success, PS256 enforced
        String cAppAdmin2Id = createClientByAdmin(generateSuffixedName("App-by-Admin2"), (ClientRepresentation client2Rep) -> {
        });
        ClientRepresentation cRep2 = getClientByAdmin(cAppAdmin2Id);
        assertEquals(Algorithm.PS256, cRep2.getAttributes().get(OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG));
        assertEquals(Algorithm.PS256, cRep2.getAttributes().get(OIDCConfigAttributes.REQUEST_OBJECT_SIGNATURE_ALG));
        assertEquals(Algorithm.PS256, cRep2.getAttributes().get(OIDCConfigAttributes.ID_TOKEN_SIGNED_RESPONSE_ALG));
        assertEquals(Algorithm.PS256, cRep2.getAttributes().get(OIDCConfigAttributes.TOKEN_ENDPOINT_AUTH_SIGNING_ALG));
        assertEquals(Algorithm.PS256, cRep2.getAttributes().get(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG));

        // update by Admin REST API - fail
        try {
            updateClientByAdmin(cAppAdminId, (ClientRepresentation clientRep) -> {
                clientRep.setAttributes(new HashMap<>());
                clientRep.getAttributes().put(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG, Algorithm.RS512);
            });
        } catch (ClientPolicyException cpe) {
            assertEquals(Errors.INVALID_REQUEST, cpe.getError());
        }
        ClientRepresentation cRep = getClientByAdmin(cAppAdminId);
        assertEquals(Algorithm.ES256, cRep.getAttributes().get(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG));

        // update by Admin REST API - success
        updateClientByAdmin(cAppAdminId, (ClientRepresentation clientRep) -> {
            clientRep.setAttributes(new HashMap<>());
            clientRep.getAttributes().put(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG, Algorithm.PS384);
        });
        cRep = getClientByAdmin(cAppAdminId);
        assertEquals(Algorithm.PS384, cRep.getAttributes().get(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG));

        // update profiles, ES256 enforced
        json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Den Forsta Profilen")
                        .addExecutor(SecureSigningAlgorithmExecutorFactory.PROVIDER_ID,
                                createSecureSigningAlgorithmEnforceExecutorConfig(Algorithm.ES256))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // update by Admin REST API - success
        updateClientByAdmin(cAppAdmin2Id, (ClientRepresentation client2Rep) -> {
            client2Rep.getAttributes().remove(OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG);
            client2Rep.getAttributes().remove(OIDCConfigAttributes.REQUEST_OBJECT_SIGNATURE_ALG);
            client2Rep.getAttributes().remove(OIDCConfigAttributes.ID_TOKEN_SIGNED_RESPONSE_ALG);
            client2Rep.getAttributes().remove(OIDCConfigAttributes.TOKEN_ENDPOINT_AUTH_SIGNING_ALG);
            client2Rep.getAttributes().remove(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG);
        });
        cRep2 = getClientByAdmin(cAppAdmin2Id);
        assertEquals(Algorithm.ES256, cRep2.getAttributes().get(OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG));
        assertEquals(Algorithm.ES256, cRep2.getAttributes().get(OIDCConfigAttributes.REQUEST_OBJECT_SIGNATURE_ALG));
        assertEquals(Algorithm.ES256, cRep2.getAttributes().get(OIDCConfigAttributes.ID_TOKEN_SIGNED_RESPONSE_ALG));
        assertEquals(Algorithm.ES256, cRep2.getAttributes().get(OIDCConfigAttributes.TOKEN_ENDPOINT_AUTH_SIGNING_ALG));
        assertEquals(Algorithm.ES256, cRep2.getAttributes().get(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG));

        // update profiles, fall back to PS256
        json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Den Forsta Profilen")
                        .addExecutor(SecureSigningAlgorithmExecutorFactory.PROVIDER_ID,
                                createSecureSigningAlgorithmEnforceExecutorConfig(Algorithm.RS512))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // create dynamically - fail
        try {
            createClientByAdmin(generateSuffixedName("App-in-Dynamic"), (ClientRepresentation clientRep) -> {
                clientRep.setSecret("secret");
                clientRep.setAttributes(new HashMap<>());
                clientRep.getAttributes().put(OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG, Algorithm.RS384);
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_REQUEST, e.getMessage());
        }

        // create dynamically - success
        String cAppDynamicClientId = createClientDynamically(generateSuffixedName("App-in-Dynamic"), (OIDCClientRepresentation clientRep) -> {
            clientRep.setUserinfoSignedResponseAlg(Algorithm.ES256);
            clientRep.setRequestObjectSigningAlg(Algorithm.ES256);
            clientRep.setIdTokenSignedResponseAlg(Algorithm.PS256);
            clientRep.setTokenEndpointAuthSigningAlg(Algorithm.PS256);
        });
        events.expect(EventType.CLIENT_REGISTER).client(cAppDynamicClientId).user(is(emptyOrNullString())).assertEvent();

        // update dynamically - fail
        try {
            updateClientDynamically(cAppDynamicClientId, (OIDCClientRepresentation clientRep) -> clientRep.setIdTokenSignedResponseAlg(Algorithm.RS256));
            fail();
        } catch (ClientRegistrationException e) {
            assertEquals(ERR_MSG_CLIENT_REG_FAIL, e.getMessage());
        }
        assertEquals(Algorithm.PS256, getClientDynamically(cAppDynamicClientId).getIdTokenSignedResponseAlg());

        // update dynamically - success
        updateClientDynamically(cAppDynamicClientId, (OIDCClientRepresentation clientRep) -> clientRep.setIdTokenSignedResponseAlg(Algorithm.ES384));
        assertEquals(Algorithm.ES384, getClientDynamically(cAppDynamicClientId).getIdTokenSignedResponseAlg());

        // create dynamically - success, PS256 enforced
        restartAuthenticatedClientRegistrationSetting();
        String cAppDynamicClient2Id = createClientDynamically(generateSuffixedName("App-in-Dynamic"), (OIDCClientRepresentation client2Rep) -> {
        });
        OIDCClientRepresentation cAppDynamicClient2Rep = getClientDynamically(cAppDynamicClient2Id);
        assertEquals(Algorithm.PS256, cAppDynamicClient2Rep.getUserinfoSignedResponseAlg());
        assertEquals(Algorithm.PS256, cAppDynamicClient2Rep.getRequestObjectSigningAlg());
        assertEquals(Algorithm.PS256, cAppDynamicClient2Rep.getIdTokenSignedResponseAlg());
        assertEquals(Algorithm.PS256, cAppDynamicClient2Rep.getTokenEndpointAuthSigningAlg());

        // update profiles, enforce ES256
        json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Den Forsta Profilen")
                        .addExecutor(SecureSigningAlgorithmExecutorFactory.PROVIDER_ID,
                                createSecureSigningAlgorithmEnforceExecutorConfig(Algorithm.ES256))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // update dynamically - success, ES256 enforced
        updateClientDynamically(cAppDynamicClient2Id, (OIDCClientRepresentation client2Rep) -> {
            client2Rep.setUserinfoSignedResponseAlg(null);
            client2Rep.setRequestObjectSigningAlg(null);
            client2Rep.setIdTokenSignedResponseAlg(null);
            client2Rep.setTokenEndpointAuthSigningAlg(null);
        });
        cAppDynamicClient2Rep = getClientDynamically(cAppDynamicClient2Id);
        assertEquals(Algorithm.ES256, cAppDynamicClient2Rep.getUserinfoSignedResponseAlg());
        assertEquals(Algorithm.ES256, cAppDynamicClient2Rep.getRequestObjectSigningAlg());
        assertEquals(Algorithm.ES256, cAppDynamicClient2Rep.getIdTokenSignedResponseAlg());
        assertEquals(Algorithm.ES256, cAppDynamicClient2Rep.getTokenEndpointAuthSigningAlg());
    }

    @Test
    public void testSecureClientRegisteringUriEnforceExecutor() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Ensimmainen Profiili")
                        .addExecutor(SecureClientUrisExecutorFactory.PROVIDER_ID, null)
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Ensimmainen Politiikka", Boolean.TRUE)
                        .addCondition(ClientUpdaterContextConditionFactory.PROVIDER_ID,
                                createClientUpdateContextConditionConfig(Arrays.asList(
                                        ClientUpdaterContextConditionFactory.BY_AUTHENTICATED_USER,
                                        ClientUpdaterContextConditionFactory.BY_INITIAL_ACCESS_TOKEN,
                                        ClientUpdaterContextConditionFactory.BY_REGISTRATION_ACCESS_TOKEN)))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        try {
            createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> clientRep.setRedirectUris(Collections.singletonList("http://newredirect")));
            fail();
        } catch (ClientRegistrationException e) {
            assertEquals(ERR_MSG_CLIENT_REG_FAIL, e.getMessage());
        }

        String cid = null;
        String clientId = generateSuffixedName(CLIENT_NAME);
        try {
            cid = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
                clientRep.setServiceAccountsEnabled(Boolean.TRUE);
                clientRep.setRedirectUris(null);
            });
        } catch (Exception e) {
            fail();
        }

        updateClientByAdmin(cid, (ClientRepresentation clientRep) -> {
            clientRep.setRedirectUris(null);
            clientRep.setServiceAccountsEnabled(Boolean.FALSE);
        });
        assertEquals(false, getClientByAdmin(cid).isServiceAccountsEnabled());

        // update policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Paivitetyn Ensimmaisen Politiikka", Boolean.TRUE)
                        .addCondition(ClientUpdaterContextConditionFactory.PROVIDER_ID,
                                createClientUpdateContextConditionConfig(Arrays.asList(
                                        ClientUpdaterContextConditionFactory.BY_AUTHENTICATED_USER,
                                        ClientUpdaterContextConditionFactory.BY_REGISTRATION_ACCESS_TOKEN)))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        try {
            updateClientDynamically(clientId, (OIDCClientRepresentation clientRep) -> clientRep.setRedirectUris(Collections.singletonList("https://newredirect/*")));
            fail();
        } catch (ClientRegistrationException e) {
            assertEquals(ERR_MSG_CLIENT_REG_FAIL, e.getMessage());
        }

        try {
            updateClientByAdmin(cid, (ClientRepresentation clientRep) -> {
                // rootUrl
                clientRep.setRootUrl("https://client.example.com/");
                // adminUrl
                clientRep.setAdminUrl("https://client.example.com/admin/");
                // baseUrl
                clientRep.setBaseUrl("https://client.example.com/base/");
                // web origins
                clientRep.setWebOrigins(Arrays.asList("https://valid.other.client.example.com/", "https://valid.another.client.example.com/"));
                // backchannel logout URL
                Map<String, String> attributes = Optional.ofNullable(clientRep.getAttributes()).orElse(new HashMap<>());
                attributes.put(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL, "https://client.example.com/logout/");
                clientRep.setAttributes(attributes);
                // OAuth2 : redirectUris
                clientRep.setRedirectUris(Arrays.asList("https://client.example.com/redirect/", "https://client.example.com/callback/"));
                // OAuth2 : jwks_uri
                attributes.put(OIDCConfigAttributes.JWKS_URL, "https://client.example.com/jwks/");
                clientRep.setAttributes(attributes);
                // OIDD : requestUris
                setAttributeMultivalued(clientRep, OIDCConfigAttributes.REQUEST_URIS, Arrays.asList("https://client.example.com/request/", "https://client.example.com/reqobj/"));
                // CIBA Client Notification Endpoint
                attributes.put(CibaConfig.CIBA_BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT, "https://client.example.com/client-notification/");
                clientRep.setAttributes(attributes);
            });
        } catch (Exception e) {
            fail();
        }

        try {
            updateClientByAdmin(cid, (ClientRepresentation clientRep) -> {
                // rootUrl
                clientRep.setRootUrl("http://client.example.com/*/");
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getError());
            assertEquals("Invalid rootUrl", e.getErrorDetail());
        }

        try {
            updateClientByAdmin(cid, (ClientRepresentation clientRep) -> {
                // adminUrl
                clientRep.setAdminUrl("http://client.example.com/admin/");
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getError());
            assertEquals("Invalid adminUrl", e.getErrorDetail());
        }

        try {
            updateClientByAdmin(cid, (ClientRepresentation clientRep) -> {
                // baseUrl
                clientRep.setBaseUrl("https://client.example.com/base/*");
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getError());
            assertEquals("Invalid baseUrl", e.getErrorDetail());
        }

        try {
            updateClientByAdmin(cid, (ClientRepresentation clientRep) -> {
                // web origins
                clientRep.setWebOrigins(List.of("http://valid.another.client.example.com/"));
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getError());
            assertEquals("Invalid webOrigins", e.getErrorDetail());
        }

        try {
            updateClientByAdmin(cid, (ClientRepresentation clientRep) -> {
                // backchannel logout URL
                Map<String, String> attributes = Optional.ofNullable(clientRep.getAttributes()).orElse(new HashMap<>());
                attributes.put(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL, "httpss://client.example.com/logout/");
                clientRep.setAttributes(attributes);
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getError());
            assertEquals("Invalid logoutUrl", e.getErrorDetail());
        }

        try {
            updateClientByAdmin(cid, (ClientRepresentation clientRep) -> {
                // OAuth2 : redirectUris
                clientRep.setRedirectUris(Arrays.asList("https://client.example.com/redirect/", "ftp://client.example.com/callback/"));
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getError());
            assertEquals("Invalid redirectUris", e.getErrorDetail());
        }

        try {
            updateClientByAdmin(cid, (ClientRepresentation clientRep) -> {
                // OAuth2 : jwks_uri
                Map<String, String> attributes = Optional.ofNullable(clientRep.getAttributes()).orElse(new HashMap<>());
                attributes.put(OIDCConfigAttributes.JWKS_URL, "http s://client.example.com/jwks/");
                clientRep.setAttributes(attributes);
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getError());
            assertEquals("Invalid jwksUri", e.getErrorDetail());
        }

        try {
            updateClientByAdmin(cid, (ClientRepresentation clientRep) -> {
                // OIDD : requestUris
                setAttributeMultivalued(clientRep, OIDCConfigAttributes.REQUEST_URIS, Arrays.asList("https://client.example.com/request/*", "https://client.example.com/reqobj/"));
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getError());
            assertEquals("Invalid requestUris", e.getErrorDetail());
        }

        try {
            updateClientByAdmin(cid, (ClientRepresentation clientRep) -> {
                // CIBA Client Notification Endpoint
                Map<String, String> attributes = Optional.ofNullable(clientRep.getAttributes()).orElse(new HashMap<>());
                attributes.put(CibaConfig.CIBA_BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT, "http://client.example.com/client-notification/");
                clientRep.setAttributes(attributes);
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getError());
            assertEquals("Invalid cibaClientNotificationEndpoint", e.getErrorDetail());
        }
    }

    @Test
    public void testSecureSigningAlgorithmForSignedJwtEnforceExecutorWithSecureAlg() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                        (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Ensimmainen Profiili")
                                .addExecutor(SecureSigningAlgorithmForSignedJwtExecutorFactory.PROVIDER_ID, createSecureSigningAlgorithmForSignedJwtEnforceExecutorConfig(Boolean.TRUE)
                                ).toRepresentation()
                )
                .toString();
        updateProfiles(json);

        // register policies
        String roleAlphaName = "sample-client-role-alpha";
        String roleZetaName = "sample-client-role-zeta";
        String roleCommonName = "sample-client-role-common";
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Den Forste Politikken", Boolean.TRUE)
                        .addCondition(ClientRolesConditionFactory.PROVIDER_ID,
                                createClientRolesConditionConfig(Arrays.asList(roleAlphaName, roleZetaName)))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // create a client with client role
        String clientId = generateSuffixedName(CLIENT_NAME);
        String cid = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret("secret");
            clientRep.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID);
            clientRep.setAttributes(new HashMap<>());
            clientRep.getAttributes().put(OIDCConfigAttributes.TOKEN_ENDPOINT_AUTH_SIGNING_ALG, Algorithm.ES256);
        });
        adminClient.realm(REALM_NAME).clients().get(cid).roles().create(RoleBuilder.create().name(roleAlphaName).build());
        adminClient.realm(REALM_NAME).clients().get(cid).roles().create(RoleBuilder.create().name(roleCommonName).build());


        ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm(REALM_NAME), clientId);
        assert clientResource != null;
        ClientRepresentation clientRep = clientResource.toRepresentation();

        KeyPair keyPair = setupJwksUrl(Algorithm.ES256, clientRep, clientResource);
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        String signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, Algorithm.ES256);

        oauth.client(clientId);
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
        EventRepresentation loginEvent = events.expectLogin()
                .client(clientId)
                .assertEvent();
        String sessionId = loginEvent.getSessionId();
        String code = oauth.parseLoginResponse().getCode();

        // obtain access token
        AccessTokenResponse response = doAccessTokenRequestWithSignedJWT(code, signedJwt);

        assertEquals(200, response.getStatusCode());
        oauth.verifyToken(response.getAccessToken());
        RefreshToken refreshToken = oauth.parseRefreshToken(response.getRefreshToken());
        assertEquals(sessionId, refreshToken.getSessionId());
        assertEquals(sessionId, refreshToken.getSessionId());
        events.expectCodeToToken(loginEvent.getDetails().get(Details.CODE_ID), loginEvent.getSessionId())
                .client(clientId)
                .detail(Details.CLIENT_AUTH_METHOD, JWTClientAuthenticator.PROVIDER_ID)
                .assertEvent();

        // refresh token
        signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, Algorithm.ES256);
        AccessTokenResponse refreshedResponse = doRefreshTokenRequestWithSignedJWT(response.getRefreshToken(), signedJwt);
        assertEquals(200, refreshedResponse.getStatusCode());

        // introspect token
        signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, Algorithm.ES256);
        HttpResponse tokenIntrospectionResponse = doTokenIntrospectionWithSignedJWT("access_token", refreshedResponse.getAccessToken(), signedJwt);
        assertEquals(200, tokenIntrospectionResponse.getStatusLine().getStatusCode());

        // revoke token
        signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, Algorithm.ES256);
        HttpResponse revokeTokenResponse = doTokenRevokeWithSignedJWT("refresh_toke", refreshedResponse.getRefreshToken(), signedJwt);
        assertEquals(200, revokeTokenResponse.getStatusLine().getStatusCode());

        signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, Algorithm.ES256);
        AccessTokenResponse tokenRes = doRefreshTokenRequestWithSignedJWT(refreshedResponse.getRefreshToken(), signedJwt);
        assertEquals(400, tokenRes.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_GRANT, tokenRes.getError());

        // logout
        signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, Algorithm.ES256);
        HttpResponse logoutResponse = doLogoutWithSignedJWT(refreshedResponse.getRefreshToken(), signedJwt);
        assertEquals(204, logoutResponse.getStatusLine().getStatusCode());
    }

    @Test
    public void testSecureSigningAlgorithmForSignedJwtEnforceExecutorWithNotSecureAlg() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Ensimmainen Profiili")
                        .addExecutor(SecureSigningAlgorithmForSignedJwtExecutorFactory.PROVIDER_ID, createSecureSigningAlgorithmForSignedJwtEnforceExecutorConfig(Boolean.FALSE))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        String roleAlphaName = "sample-client-role-alpha";
        String roleZetaName = "sample-client-role-zeta";
        String roleCommonName = "sample-client-role-common";
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Den Forste Politikken", Boolean.TRUE)
                        .addCondition(ClientRolesConditionFactory.PROVIDER_ID,
                                createClientRolesConditionConfig(Arrays.asList(roleAlphaName, roleZetaName)))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // create a client with client role
        String clientId = generateSuffixedName(CLIENT_NAME);
        String cid = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret("secret");
            clientRep.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID);
            clientRep.setAttributes(new HashMap<>());
            clientRep.getAttributes().put(OIDCConfigAttributes.TOKEN_ENDPOINT_AUTH_SIGNING_ALG, Algorithm.RS256);
        });
        adminClient.realm(REALM_NAME).clients().get(cid).roles().create(RoleBuilder.create().name(roleAlphaName).build());
        adminClient.realm(REALM_NAME).clients().get(cid).roles().create(RoleBuilder.create().name(roleCommonName).build());

        ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm(REALM_NAME), clientId);
        assert clientResource != null;
        ClientRepresentation clientRep = clientResource.toRepresentation();

        KeyPair keyPair = setupJwksUrl(Algorithm.RS256, clientRep, clientResource);
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        String signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, Algorithm.RS256);

        oauth.client(clientId);
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
        events.expectLogin().client(clientId).assertEvent();
        String code = oauth.parseLoginResponse().getCode();

        // obtain access token
        AccessTokenResponse response = doAccessTokenRequestWithSignedJWT(code, signedJwt);

        assertEquals(400, response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_GRANT, response.getError());
        assertEquals("not allowed signature algorithm.", response.getErrorDescription());
    }

    @Test
    public void testSecureLogoutExecutor() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Logout Test")
                        .addExecutor(SecureLogoutExecutorFactory.PROVIDER_ID, null)
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Logout Policy", Boolean.TRUE)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID,
                                createAnyClientConditionConfig())
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        String clientId = generateSuffixedName(CLIENT_NAME);
        String clientSecret = "secret";
        try {
            createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
                clientRep.setSecret(clientSecret);
                clientRep.setStandardFlowEnabled(Boolean.TRUE);
                clientRep.setImplicitFlowEnabled(Boolean.TRUE);
                clientRep.setPublicClient(Boolean.FALSE);
                clientRep.setFrontchannelLogout(true);
            });
        } catch (ClientPolicyException cpe) {
            assertEquals("Front-channel logout is not allowed for this client", cpe.getErrorDetail());
        }

        String cid = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret(clientSecret);
            clientRep.setStandardFlowEnabled(Boolean.TRUE);
            clientRep.setImplicitFlowEnabled(Boolean.TRUE);
            clientRep.setPublicClient(Boolean.FALSE);
        });

        ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(cid);
        ClientRepresentation clientRep = clientResource.toRepresentation();

        clientRep.setFrontchannelLogout(true);

        try {
            clientResource.update(clientRep);
        } catch (BadRequestException bre) {
            assertEquals("Front-channel logout is not allowed for this client", bre.getResponse().readEntity(OAuth2ErrorRepresentation.class).getErrorDescription());
        }

        ClientPolicyExecutorConfigurationRepresentation config = new ClientPolicyExecutorConfigurationRepresentation();

        config.setConfigAsMap(SecureLogoutExecutorFactory.ALLOW_FRONT_CHANNEL_LOGOUT, true);

        json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Logout Test")
                        .addExecutor(SecureLogoutExecutorFactory.PROVIDER_ID, config)
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setFrontChannelLogoutUrl(oauth.getRedirectUri());
        clientResource.update(clientRep);

        config.setConfigAsMap(SecureLogoutExecutorFactory.ALLOW_FRONT_CHANNEL_LOGOUT, Boolean.FALSE.toString());

        json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Logout Test")
                        .addExecutor(SecureLogoutExecutorFactory.PROVIDER_ID, config)
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        AccessTokenResponse response = successfulLogin(clientId, clientSecret);

        oauth.logoutForm().idTokenHint(response.getIdToken()).open();

        assertTrue(Objects.requireNonNull(driver.getPageSource()).contains("Front-channel logout is not allowed for this client"));
    }

    @Test
    public void testSecureParContentsExecutor() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(SecureParContentsExecutorFactory.PROVIDER_ID, null)
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        String clientBetaId = generateSuffixedName("Beta-App");
        createClientByAdmin(clientBetaId, (ClientRepresentation clientRep) -> clientRep.setSecret("secretBeta"));

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID,
                                createAnyClientConditionConfig())
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        oauth.client(clientBetaId, "secretBeta");

        // Pushed Authorization Request
        ParResponse pResp = oauth.pushedAuthorizationRequest().send();
        assertEquals(201, pResp.getStatusCode());
        requestUri = pResp.getRequestUri();

        oauth.client(clientBetaId);
        oauth.loginForm().state("randomstatesomething").requestUri(requestUri).open();
        assertTrue(errorPage.isCurrent());
        assertEquals("PAR request did not include necessary parameters", errorPage.getError());
        events.expectClientPolicyError(EventType.LOGIN_ERROR, OAuthErrorException.INVALID_REQUEST,
                        Details.CLIENT_POLICY_ERROR, OAuthErrorException.INVALID_REQUEST,
                        "PAR request did not include necessary parameters").client((String) null)
                .user((String) null).assertEvent();

        oauth.client(clientBetaId, "secretBeta");
        pResp = oauth.doPushedAuthorizationRequest();
        assertEquals(201, pResp.getStatusCode());
        requestUri = pResp.getRequestUri();

        successfulLoginAndLogout(clientBetaId, "secretBeta");
    }

    @Test
    public void testSecureParContentsExecutorWithRequestObject() throws Exception {
        // Set up a request object
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
        oidcClientEndpointsResource.setOIDCRequest(REALM_NAME, TEST_CLIENT, oauth.getRedirectUri(), "10", null, "none");
        String encodedRequestObject = oidcClientEndpointsResource.getOIDCRequest();

        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(SecureParContentsExecutorFactory.PROVIDER_ID, null)
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID,
                                createAnyClientConditionConfig())
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        oauth.client(TEST_CLIENT, TEST_CLIENT_SECRET);

        // Pushed Authorization Request without state parameter
        ParResponse pResp = oauth.pushedAuthorizationRequest().request(encodedRequestObject).send();
        assertEquals(201, pResp.getStatusCode());
        String requestUri = pResp.getRequestUri();

        // only query parameters include state parameter
        oauth.loginForm().requestUri(requestUri).state("mystate2").open();
        assertTrue(errorPage.isCurrent());
        assertEquals("PAR request did not include necessary parameters", errorPage.getError());
        events.expectClientPolicyError(EventType.LOGIN_ERROR, OAuthErrorException.INVALID_REQUEST,
                        Details.CLIENT_POLICY_ERROR, OAuthErrorException.INVALID_REQUEST,
                        "PAR request did not include necessary parameters").client((String) null)
                .user((String) null).assertEvent();

        // Pushed Authorization Request with state parameter
        oidcClientEndpointsResource.setOIDCRequest(REALM_NAME, TEST_CLIENT, oauth.getRedirectUri(), "10", "mystate2", "none");
        encodedRequestObject = oidcClientEndpointsResource.getOIDCRequest();

        pResp = oauth.pushedAuthorizationRequest().request(encodedRequestObject).state("mystate2").send();
        assertEquals(201, pResp.getStatusCode());
        requestUri = pResp.getRequestUri();

        // both query parameters and PAR requests include state parameter
        this.requestUri = requestUri;
        successfulLoginAndLogout(TEST_CLIENT, TEST_CLIENT_SECRET);

    }

    @Test
    public void testSecureClientAuthenticationAssertionExecutor() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(SecureClientAuthenticationAssertionExecutorFactory.PROVIDER_ID, null)
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID,
                                createAnyClientConditionConfig())
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // Register client with private-key-jwt
        String clientId = generateSuffixedName(CLIENT_NAME);
        createClientByAdmin(clientId, (ClientRepresentation clientRep) -> clientRep.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID));

        ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm(REALM_NAME), clientId);
        assert clientResource != null;
        ClientRepresentation clientRep = clientResource.toRepresentation();

        oauth.client(clientId);

        // Get keys of client. Will be used for client authentication and signing of request object
        KeyPair keyPair = setupJwksUrl(Algorithm.RS256, clientRep, clientResource);
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        // Send a push authorization request with invalid 'aud' . Should fail
        String[] audienceUrls = {getRealmInfoUrl(), getRealmInfoUrl() + "/protocol/openid-connect/ext/par/request"};
        String signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, Algorithm.RS256, audienceUrls);
        //     for the test, allow multiple audiences for JWT client authentication temporarily.
        allowMultipleAudiencesForClientJWTOnServer(true);
        ParResponse pResp = null;
        try {
            pResp = oauth.pushedAuthorizationRequest().request(request).signedJwt(signedJwt).send();
        } finally {
            allowMultipleAudiencesForClientJWTOnServer(false);
        }
        assertEquals(400, pResp.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, pResp.getError());

        signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, Algorithm.RS256, getRealmInfoUrl() + "/protocol/openid-connect/ext/par/request");
        pResp = oauth.pushedAuthorizationRequest().request(request).signedJwt(signedJwt).send();
        assertEquals(400, pResp.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, pResp.getError());

        // Send a push authorization request with valid 'aud' . Should succeed
        signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, Algorithm.RS256, getRealmInfoUrl());
        pResp = oauth.pushedAuthorizationRequest().request(request).signedJwt(signedJwt).send();
        assertEquals(201, pResp.getStatusCode());
        requestUri = pResp.getRequestUri();

        // Send an authorization request . Should succeed
        AuthorizationEndpointResponse loginResponse = oauth.loginForm().requestUri(requestUri).doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
        String code = loginResponse.getCode();
        assertNotNull(code);

        // Send a token request with invalid 'aud' . Should fail
        signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, Algorithm.RS256, getRealmInfoUrl() + "/protocol/openid-connect/token");
        AccessTokenResponse tokenResponse = doAccessTokenRequestWithClientSignedJWT(code, signedJwt, DefaultHttpClient::new);
        assertEquals(400, tokenResponse.getStatusCode());

        // Send a token request with valid 'aud' . Should succeed
        UserResource user = ApiUtil.findUserByUsernameId(adminClient.realm(REALM_NAME), TEST_USER_NAME);
        user.logout();

        loginResponse = oauth.loginForm().doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
        code = loginResponse.getCode();

        signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, Algorithm.RS256, getRealmInfoUrl());

        tokenResponse = doAccessTokenRequestWithClientSignedJWT(code, signedJwt, DefaultHttpClient::new);
        assertEquals(200, tokenResponse.getStatusCode());
        MatcherAssert.assertThat(tokenResponse.getAccessToken(), Matchers.notNullValue());

        // Send a token refresh request with invalid 'aud' . Should fail
        signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, Algorithm.RS256, getRealmInfoUrl() + "/protocol/openid-connect/token");
        AccessTokenResponse refreshedResponse = doRefreshTokenRequestWithSignedJWT(tokenResponse.getRefreshToken(), signedJwt);
        assertEquals(400, refreshedResponse.getStatusCode());

        // Send a token refresh request with valid 'aud' . Should succeed
        signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, Algorithm.RS256, getRealmInfoUrl());
        refreshedResponse = doRefreshTokenRequestWithSignedJWT(tokenResponse.getRefreshToken(), signedJwt);
        assertEquals(200, refreshedResponse.getStatusCode());

        // Send a token introspection request with invalid 'aud' . Should fail
        signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, Algorithm.RS256, getRealmInfoUrl() + "/protocol/openid-connect/introspect");
        HttpResponse tokenIntrospectionResponse = doTokenIntrospectionWithSignedJWT("access_token", refreshedResponse.getAccessToken(), signedJwt);
        assertEquals(401, tokenIntrospectionResponse.getStatusLine().getStatusCode());

        // Send a token introspection request with valid 'aud' . Should succeed
        signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, Algorithm.RS256, getRealmInfoUrl());
        tokenIntrospectionResponse = doTokenIntrospectionWithSignedJWT("access_token", refreshedResponse.getAccessToken(), signedJwt);
        assertEquals(200, tokenIntrospectionResponse.getStatusLine().getStatusCode());

        // Send a token revoke request with invalid 'aud' . Should fail
        signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, Algorithm.RS256, getRealmInfoUrl() + "/protocol/openid-connect/revoke");
        HttpResponse revokeTokenResponse = doTokenRevokeWithSignedJWT("refresh_token", refreshedResponse.getRefreshToken(), signedJwt);
        assertEquals(400, revokeTokenResponse.getStatusLine().getStatusCode());

        // Send a token revoke request with valid 'aud' . Should succeed
        signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, Algorithm.RS256, getRealmInfoUrl());
        revokeTokenResponse = doTokenRevokeWithSignedJWT("refresh_token", refreshedResponse.getRefreshToken(), signedJwt);
        assertEquals(200, revokeTokenResponse.getStatusLine().getStatusCode());
    }

    private String createSignedRequestToken(String clientId, PrivateKey privateKey, PublicKey publicKey, String algorithm, String audUrl) {
        JsonWebToken jwt = createRequestToken(clientId, audUrl);
        String kid = KeyUtils.createKeyId(publicKey);
        SignatureSignerContext signer = SignatureSignerUtil.createSigner(privateKey, kid, algorithm);
        return new JWSBuilder().kid(kid).jsonContent(jwt).sign(signer);
    }

    private String createSignedRequestToken(String clientId, PrivateKey privateKey, PublicKey publicKey, String algorithm, String[] audienceUrls) {
        JsonWebToken jwt = createRequestToken(clientId, audienceUrls);
        String kid = KeyUtils.createKeyId(publicKey);
        SignatureSignerContext signer = SignatureSignerUtil.createSigner(privateKey, kid, algorithm);
        return new JWSBuilder().kid(kid).jsonContent(jwt).sign(signer);
    }

    private AccessTokenResponse doAccessTokenRequestWithClientSignedJWT(String code, String signedJwt, Supplier<CloseableHttpClient> httpClientSupplier) {
        try {
            List<NameValuePair> parameters = new LinkedList<>();
            parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.AUTHORIZATION_CODE));
            parameters.add(new BasicNameValuePair(OAuth2Constants.CODE, code));

            parameters.add(new BasicNameValuePair(OAuth2Constants.REDIRECT_URI, oauth.getRedirectUri()));
            parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
            parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, signedJwt));

            CloseableHttpResponse response = sendRequest(oauth.getEndpoints().getToken(), parameters, httpClientSupplier);
            return new AccessTokenResponse(response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private CloseableHttpResponse sendRequest(String requestUrl, List<NameValuePair> parameters, Supplier<CloseableHttpClient> httpClientSupplier) throws Exception {
        try (CloseableHttpClient client = httpClientSupplier.get()) {
            HttpPost post = new HttpPost(requestUrl);
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
            post.setEntity(formEntity);
            return client.execute(post);
        }
    }
}

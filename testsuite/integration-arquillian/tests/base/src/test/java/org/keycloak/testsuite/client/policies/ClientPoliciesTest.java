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

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientSecretAuthenticator;
import org.keycloak.authentication.authenticators.client.X509ClientAuthenticator;
import org.keycloak.common.Profile;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.Time;
import org.keycloak.common.util.UriUtils;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.OAuth2DeviceConfig;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.ClaimsParameterWithValueIdTokenMapper;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.ClaimsRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientAccessTypeConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientRolesConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientScopesConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdaterContextConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdaterSourceGroupsConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdaterSourceRolesConditionFactory;
import org.keycloak.services.clientpolicy.condition.GrantTypeConditionFactory;
import org.keycloak.services.clientpolicy.executor.ConfidentialClientAcceptExecutorFactory;
import org.keycloak.services.clientpolicy.executor.ConsentRequiredExecutorFactory;
import org.keycloak.services.clientpolicy.executor.FullScopeDisabledExecutorFactory;
import org.keycloak.services.clientpolicy.executor.HolderOfKeyEnforcerExecutorFactory;
import org.keycloak.services.clientpolicy.executor.IntentClientBindCheckExecutorFactory;
import org.keycloak.services.clientpolicy.executor.PKCEEnforcerExecutorFactory;
import org.keycloak.services.clientpolicy.executor.RejectImplicitGrantExecutorFactory;
import org.keycloak.services.clientpolicy.executor.RejectRequestExecutorFactory;
import org.keycloak.services.clientpolicy.executor.RejectResourceOwnerPasswordCredentialsGrantExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureClientAuthenticatorExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureSessionEnforceExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureSigningAlgorithmForSignedJwtExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SuppressRefreshTokenRotationExecutorFactory;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.client.resources.TestApplicationResourceUrls;
import org.keycloak.testsuite.services.clientpolicy.condition.TestRaiseExceptionConditionFactory;
import org.keycloak.testsuite.services.clientpolicy.executor.TestRaiseExceptionExecutorFactory;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientPoliciesBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientPolicyBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientProfileBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientProfilesBuilder;
import org.keycloak.testsuite.util.MutualTLSUtils;
import org.keycloak.testsuite.util.RoleBuilder;
import org.keycloak.testsuite.util.ServerURLs;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.ParResponse;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import static org.keycloak.testsuite.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.admin.ApiUtil.findClientResourceByClientId;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createAnyClientConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createClientRolesConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createClientScopesConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createClientUpdateContextConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createConsentRequiredExecutorConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createFullScopeDisabledExecutorConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createGrantTypeConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createHolderOfKeyEnforceExecutorConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createIntentClientBindCheckExecutorConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createPKCEEnforceExecutorConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createRejectImplicitGrantExecutorConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createRejectisResourceOwnerPasswordCredentialsGrantExecutorConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createSecureClientAuthenticatorExecutorConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createSecureSigningAlgorithmForSignedJwtEnforceExecutorConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createTestRaiseExeptionConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createTestRaiseExeptionExecutorConfig;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
@EnableFeature(value = Profile.Feature.CLIENT_SECRET_ROTATION)
public class ClientPoliciesTest extends AbstractClientPoliciesTest {

    private static final Logger logger = Logger.getLogger(ClientPoliciesTest.class);

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

    // KEYCLOAK-18108
    @Test
    public void testTwoProfilesWithDifferentConfigurationOfSameExecutorType() throws Exception {
        setupPolicyClientIdAndSecretNotAcceptableAuthType(POLICY_NAME);

        // register another profile with "SecureClientAuthEnforceExecutorFactory", but use different configuration of client authenticator.
        // This profile won't allow JWTClientSecretAuthenticator.PROVIDER_ID
        String profileName = "UnusedProfile";
        String json = (new ClientProfilesBuilder(getProfilesWithoutGlobals())).addProfile(
                (new ClientProfileBuilder()).createProfile(profileName, "Profile with SecureClientAuthEnforceExecutorFactory")
                        .addExecutor(SecureClientAuthenticatorExecutorFactory.PROVIDER_ID,
                                createSecureClientAuthenticatorExecutorConfig(
                                        Arrays.asList(JWTClientAuthenticator.PROVIDER_ID, X509ClientAuthenticator.PROVIDER_ID),
                                        null))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // Make sure it is still possible to create client with JWTClientSecretAuthenticator. The "UnusedProfile" should not be used as it is not referenced from any client policy
        String cId = createClientByAdmin(generateSuffixedName(CLIENT_NAME), (ClientRepresentation clientRep) -> clientRep.setClientAuthenticatorType(JWTClientSecretAuthenticator.PROVIDER_ID));
        assertEquals(JWTClientSecretAuthenticator.PROVIDER_ID, getClientByAdmin(cId).getClientAuthenticatorType());
    }

    @Test
    public void testDynamicClientRegisterAndUpdate() throws Exception {
        setupPolicyClientIdAndSecretNotAcceptableAuthType(POLICY_NAME);

        String clientId = createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {
        });
        assertEquals(OIDCLoginProtocol.CLIENT_SECRET_BASIC, getClientDynamically(clientId).getTokenEndpointAuthMethod());
        assertEquals(Boolean.FALSE, getClientDynamically(clientId).getTlsClientCertificateBoundAccessTokens());

        updateClientDynamically(clientId, (OIDCClientRepresentation clientRep) -> {
            clientRep.setTokenEndpointAuthMethod(OIDCLoginProtocol.CLIENT_SECRET_BASIC);
            clientRep.setTlsClientCertificateBoundAccessTokens(Boolean.TRUE);
        });
        assertEquals(OIDCLoginProtocol.CLIENT_SECRET_BASIC, getClientDynamically(clientId).getTokenEndpointAuthMethod());
        assertEquals(Boolean.TRUE, getClientDynamically(clientId).getTlsClientCertificateBoundAccessTokens());
    }

    @Test
    public void testCreateDeletePolicyRuntime() throws Exception {
        String clientId = createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {
        });
        OIDCClientRepresentation clientRep = getClientDynamically(clientId);
        assertEquals(OIDCLoginProtocol.CLIENT_SECRET_BASIC, clientRep.getTokenEndpointAuthMethod());
        events.expect(EventType.CLIENT_REGISTER).client(clientId).user(is(emptyOrNullString())).assertEvent();
        events.expect(EventType.CLIENT_INFO).client(clientId).user(is(emptyOrNullString())).assertEvent();
        adminClient.realm(REALM_NAME).clients().get(clientId).roles().create(RoleBuilder.create().name(SAMPLE_CLIENT_ROLE).build());

        successfulLoginAndLogout(clientId, clientRep.getClientSecret());

        setupPolicyAuthzCodeFlowUnderMultiPhasePolicy(POLICY_NAME);

        failLoginByNotFollowingPKCE(clientId);

        deletePolicy(POLICY_NAME);
        logger.info("... Deleted Policy : " + POLICY_NAME);

        successfulLoginAndLogout(clientId, clientRep.getClientSecret());
    }

    @Test
    public void testCreateUpdateDeleteConditionRuntime() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Den Eichte profil")
                        .addExecutor(PKCEEnforcerExecutorFactory.PROVIDER_ID,
                                createPKCEEnforceExecutorConfig(Boolean.TRUE))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        String clientId = generateSuffixedName(CLIENT_NAME);
        String clientSecret = "secret";
        String cid = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> clientRep.setSecret(clientSecret));
        adminClient.realm(REALM_NAME).clients().get(cid).roles().create(RoleBuilder.create().name(SAMPLE_CLIENT_ROLE).build());

        successfulLoginAndLogout(clientId, clientSecret);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Dei Eischt Politik", Boolean.TRUE)
                        .addCondition(ClientRolesConditionFactory.PROVIDER_ID,
                                createClientRolesConditionConfig(List.of(SAMPLE_CLIENT_ROLE)))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        failLoginByNotFollowingPKCE(clientId);

        // update policies
        updatePolicy((new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Dei Aktualiseiert Eischt Politik", Boolean.TRUE)
                .addCondition(ClientRolesConditionFactory.PROVIDER_ID,
                        createClientRolesConditionConfig(List.of("anothor-client-role")))
                .addProfile(PROFILE_NAME)
                .toRepresentation());

        successfulLoginAndLogout(clientId, clientSecret);

        // update policies
        updatePolicy((new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Dei Aktualiseiert Eischt Politik", Boolean.TRUE)
                .addProfile(PROFILE_NAME)
                .toRepresentation());

        successfulLoginAndLogout(clientId, clientSecret);
    }

    @Test
    public void testCreateUpdateDeleteExecutorRuntime() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Purofairu Sono Ichi")
                        .addExecutor(PKCEEnforcerExecutorFactory.PROVIDER_ID,
                                createPKCEEnforceExecutorConfig(Boolean.FALSE))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Porishii Sono Ichi", Boolean.TRUE)
                        .addCondition(ClientRolesConditionFactory.PROVIDER_ID,
                                createClientRolesConditionConfig(List.of(SAMPLE_CLIENT_ROLE)))
                        .addCondition(ClientUpdaterContextConditionFactory.PROVIDER_ID,
                                createClientUpdateContextConditionConfig(List.of(ClientUpdaterContextConditionFactory.BY_AUTHENTICATED_USER)))
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        String clientId = generateSuffixedName(CLIENT_NAME);
        String clientSecret = "secret";
        String cid = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> clientRep.setSecret(clientSecret));
        adminClient.realm(REALM_NAME).clients().get(cid).roles().create(RoleBuilder.create().name(SAMPLE_CLIENT_ROLE).build());

        successfulLoginAndLogout(clientId, clientSecret);

        // update policies
        updatePolicy((new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Koushinsareta Porishii Sono Ichi", Boolean.TRUE)
                .addCondition(ClientRolesConditionFactory.PROVIDER_ID,
                        createClientRolesConditionConfig(List.of(SAMPLE_CLIENT_ROLE)))
                .addCondition(ClientUpdaterContextConditionFactory.PROVIDER_ID,
                        createClientUpdateContextConditionConfig(List.of(ClientUpdaterContextConditionFactory.BY_AUTHENTICATED_USER)))
                .addProfile(PROFILE_NAME)
                .toRepresentation());

        failLoginByNotFollowingPKCE(clientId);

        // update profiles
        updateProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Koushinsareta Purofairu Sono Ichi")
                        .addExecutor(PKCEEnforcerExecutorFactory.PROVIDER_ID,
                                createPKCEEnforceExecutorConfig(Boolean.TRUE))
                        .toRepresentation());

        updateClientByAdmin(cid, (ClientRepresentation clientRep) -> clientRep.setServiceAccountsEnabled(Boolean.FALSE));
        assertEquals(false, getClientByAdmin(cid).isServiceAccountsEnabled());
        assertEquals(OAuth2Constants.PKCE_METHOD_S256, OIDCAdvancedConfigWrapper.fromClientRepresentation(getClientByAdmin(cid)).getPkceCodeChallengeMethod());

        // update profiles
        updateProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Sarani Koushinsareta Purofairu Sono Ichi").toRepresentation());

        updateClientByAdmin(cid, (ClientRepresentation clientRep) -> OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setPkceCodeChallengeMethod(null));
        assertNull(OIDCAdvancedConfigWrapper.fromClientRepresentation(getClientByAdmin(cid)).getPkceCodeChallengeMethod());

        successfulLoginAndLogout(clientId, clientSecret);
    }

    @Test
    public void testAuthzCodeFlowUnderMultiPhasePolicy() throws Exception {
        setupPolicyAuthzCodeFlowUnderMultiPhasePolicy(POLICY_NAME);

        String clientName = generateSuffixedName(CLIENT_NAME);
        String clientId = createClientDynamically(clientName, (OIDCClientRepresentation clientRep) -> {
        });
        events.expect(EventType.CLIENT_REGISTER).client(clientId).user(is(emptyOrNullString())).assertEvent();
        OIDCClientRepresentation response = getClientDynamically(clientId);
        String clientSecret = response.getClientSecret();
        assertEquals(clientName, response.getClientName());
        assertEquals(OIDCLoginProtocol.CLIENT_SECRET_BASIC, response.getTokenEndpointAuthMethod());
        events.expect(EventType.CLIENT_INFO).client(clientId).user(is(emptyOrNullString())).assertEvent();

        adminClient.realm(REALM_NAME).clients().get(clientId).roles().create(RoleBuilder.create().name(SAMPLE_CLIENT_ROLE).build());

        successfulLoginAndLogoutWithPKCE(response.getClientId(), clientSecret, TEST_USER_NAME, TEST_USER_PASSWORD);
    }

    @Test
    public void testMultiplePolicies() throws Exception {
        String roleAlphaName = "sample-client-role-alpha";
        String roleBetaName = "sample-client-role-beta";
        String roleZetaName = "sample-client-role-zeta";
        String roleCommonName = "sample-client-role-common";

        // register profiles
        String profileAlphaName = "MyProfile-alpha";
        String profileBetaName = "MyProfile-beta";
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(profileAlphaName, "Pierwszy Profil")
                        .addExecutor(SecureClientAuthenticatorExecutorFactory.PROVIDER_ID,
                                createSecureClientAuthenticatorExecutorConfig(List.of(ClientIdAndSecretAuthenticator.PROVIDER_ID), ClientIdAndSecretAuthenticator.PROVIDER_ID))
                        .toRepresentation()).addProfile(
                (new ClientProfileBuilder()).createProfile(profileBetaName, "Drugi Profil")
                        .addExecutor(PKCEEnforcerExecutorFactory.PROVIDER_ID,
                                createPKCEEnforceExecutorConfig(Boolean.TRUE))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        String policyAlphaName = "MyPolicy-alpha";
        String policyBetaName = "MyPolicy-beta";
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(policyAlphaName, "Pierwsza Zasada", Boolean.TRUE)
                        .addCondition(ClientRolesConditionFactory.PROVIDER_ID,
                                createClientRolesConditionConfig(Arrays.asList(roleAlphaName, roleZetaName)))
                        .addCondition(ClientUpdaterContextConditionFactory.PROVIDER_ID,
                                createClientUpdateContextConditionConfig(List.of(ClientUpdaterContextConditionFactory.BY_AUTHENTICATED_USER)))
                        .addProfile(profileAlphaName)
                        .toRepresentation()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(policyBetaName, "Drugi Zasada", Boolean.TRUE)
                        .addCondition(ClientRolesConditionFactory.PROVIDER_ID,
                                createClientRolesConditionConfig(Arrays.asList(roleBetaName, roleZetaName)))
                        .addProfile(profileBetaName)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        String clientAlphaId = generateSuffixedName("Alpha-App");
        String clientAlphaSecret = "secretAlpha";

        // Not allowed client authenticator should fail
        try {
            createClientByAdmin(generateSuffixedName(CLIENT_NAME), (ClientRepresentation clientRep) -> {
                clientRep.setSecret(clientAlphaSecret);
                clientRep.setClientAuthenticatorType(JWTClientSecretAuthenticator.PROVIDER_ID);
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getMessage());
        }

        String cAlphaId = createClientByAdmin(clientAlphaId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret(clientAlphaSecret);
            clientRep.setClientAuthenticatorType(ClientIdAndSecretAuthenticator.PROVIDER_ID);
        });
        RolesResource rolesResourceAlpha = adminClient.realm(REALM_NAME).clients().get(cAlphaId).roles();
        rolesResourceAlpha.create(RoleBuilder.create().name(roleAlphaName).build());
        rolesResourceAlpha.create(RoleBuilder.create().name(roleCommonName).build());

        String clientBetaId = generateSuffixedName("Beta-App");
        String cBetaId = createClientByAdmin(clientBetaId, (ClientRepresentation clientRep) -> clientRep.setSecret("secretBeta"));
        RolesResource rolesResourceBeta = adminClient.realm(REALM_NAME).clients().get(cBetaId).roles();
        rolesResourceBeta.create(RoleBuilder.create().name(roleBetaName).build());
        rolesResourceBeta.create(RoleBuilder.create().name(roleCommonName).build());

        assertEquals(ClientIdAndSecretAuthenticator.PROVIDER_ID, getClientByAdmin(cAlphaId).getClientAuthenticatorType());
        successfulLoginAndLogout(clientAlphaId, clientAlphaSecret);
        failLoginByNotFollowingPKCE(clientBetaId);
    }

    @Test
    public void testIntentionalExceptionOnCondition() throws Exception {
        // register policies
        String json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Fyrsta Stefnan", Boolean.TRUE)
                        .addCondition(TestRaiseExceptionConditionFactory.PROVIDER_ID,
                                createTestRaiseExeptionConditionConfig())
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        try {
            createClientByAdmin(generateSuffixedName(CLIENT_NAME), (ClientRepresentation clientRep) -> {
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.SERVER_ERROR, e.getMessage());
        }
    }

    @Test
    public void testConditionWithoutNoConfiguration() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Die Erste Politik")
                        .addExecutor(SecureClientAuthenticatorExecutorFactory.PROVIDER_ID, null)
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy("MyPolicy-ClientAccessTypeCondition", "Die Erste Politik", Boolean.TRUE)
                        .addCondition(ClientAccessTypeConditionFactory.PROVIDER_ID, null)
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).addPolicy(
                (new ClientPolicyBuilder()).createPolicy("MyPolicy-ClientUpdateSourceGroupsCondition", "Die Zweite Politik", Boolean.TRUE)
                        .addCondition(ClientUpdaterSourceGroupsConditionFactory.PROVIDER_ID, null)
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).addPolicy(
                (new ClientPolicyBuilder()).createPolicy("MyPolicy-ClientUpdateSourceRolesCondition", "Die Dritte Politik", Boolean.TRUE)
                        .addCondition(ClientUpdaterSourceRolesConditionFactory.PROVIDER_ID, null)
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).addPolicy(
                (new ClientPolicyBuilder()).createPolicy("MyPolicy-ClientUpdateContextCondition", "Die Vierte Politik", Boolean.TRUE)
                        .addCondition(ClientUpdaterContextConditionFactory.PROVIDER_ID, null)
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        String clientId = generateSuffixedName(CLIENT_NAME);
        String clientSecret = "secret";
        createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret(clientSecret);
            clientRep.setBearerOnly(Boolean.FALSE);
            clientRep.setPublicClient(Boolean.FALSE);
        });

        successfulLoginAndLogout(clientId, clientSecret);
    }

    @Test
    public void testHolderOfKeyEnforceExecutor() throws Exception {
        Assume.assumeTrue("This test must be executed with enabled TLS.", ServerURLs.AUTH_SERVER_SSL_REQUIRED);

        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Az Elso Profil")
                        .addExecutor(HolderOfKeyEnforcerExecutorFactory.PROVIDER_ID,
                                createHolderOfKeyEnforceExecutorConfig(Boolean.TRUE))
                        .addExecutor(SecureSigningAlgorithmForSignedJwtExecutorFactory.PROVIDER_ID,
                                createSecureSigningAlgorithmForSignedJwtEnforceExecutorConfig(Boolean.FALSE))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Az Elso Politika", Boolean.TRUE)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID,
                                createAnyClientConditionConfig())
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        try (ClientAttributeUpdater cau = ClientAttributeUpdater.forClient(adminClient, REALM_NAME, TEST_CLIENT)) {
            ClientRepresentation clientRep = cau.getResource().toRepresentation();
            Assert.assertNotNull(clientRep);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUseMtlsHoKToken(true);
            cau.update();
            checkMtlsFlow();
        }
    }

    @Test
    public void testSuppressRefreshTokenRotationWithHolderOfKeyToken() throws Exception {
        Assume.assumeTrue("This test must be executed with enabled TLS.", ServerURLs.AUTH_SERVER_SSL_REQUIRED);

        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(SuppressRefreshTokenRotationExecutorFactory.PROVIDER_ID, null)
                        .addExecutor(HolderOfKeyEnforcerExecutorFactory.PROVIDER_ID,
                                createHolderOfKeyEnforceExecutorConfig(Boolean.TRUE))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Den Forste Politikken", Boolean.TRUE)
                        .addCondition(ClientRolesConditionFactory.PROVIDER_ID,
                                createClientRolesConditionConfig(List.of(SAMPLE_CLIENT_ROLE)))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        try (ClientAttributeUpdater cau = ClientAttributeUpdater.forClient(adminClient, REALM_NAME, TEST_CLIENT)) {
            ClientRepresentation clientRep = cau.getResource().toRepresentation();
            Assert.assertNotNull(clientRep);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUseMtlsHoKToken(true);
            cau.update();
            // Check login.
            AuthorizationEndpointResponse loginResponse = oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
            Assert.assertNull(loginResponse.getError());

            String code = oauth.parseLoginResponse().getCode();

            // Check token obtaining.
            AccessTokenResponse accessTokenResponse;
            try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore()) {
                oauth.httpClient().set(client);
                accessTokenResponse = oauth.doAccessTokenRequest(code);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            } finally {
                oauth.httpClient().reset();
            }
            assertEquals(200, accessTokenResponse.getStatusCode());

            // Check token refresh.
            AccessTokenResponse accessTokenResponseRefreshed;
            try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore()) {
                oauth.httpClient().set(client);
                accessTokenResponseRefreshed = oauth.doRefreshTokenRequest(accessTokenResponse.getRefreshToken());
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            } finally {
                oauth.httpClient().reset();
            }
            assertEquals(200, accessTokenResponseRefreshed.getStatusCode());
            assertNull(accessTokenResponseRefreshed.getRefreshToken());
        }
    }

    @Test
    public void testNegativeLogicCondition() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Den Forste Profilen")
                        .addExecutor(SecureSessionEnforceExecutorFactory.PROVIDER_ID, null)
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

        String clientId = generateSuffixedName(CLIENT_NAME);
        String clientSecret = "secretBeta";
        createClientByAdmin(clientId, (ClientRepresentation clientRep) -> clientRep.setSecret(clientSecret));

        try {
            failLoginWithoutSecureSessionParameter(clientId, ERR_MSG_MISSING_NONCE);

            // update policies
            updatePolicy((new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                    .addCondition(AnyClientConditionFactory.PROVIDER_ID, createAnyClientConditionConfig(Boolean.TRUE))
                    .addProfile(PROFILE_NAME)
                    .toRepresentation());

            successfulLoginAndLogout(clientId, clientSecret);

            // update policies
            updatePolicy((new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                    .addCondition(AnyClientConditionFactory.PROVIDER_ID, createAnyClientConditionConfig(Boolean.FALSE))
                    .addProfile(PROFILE_NAME)
                    .toRepresentation());

            failLoginWithoutSecureSessionParameter(clientId, ERR_MSG_MISSING_NONCE);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testUpdatePolicyWithoutNameNotAllowed() throws Exception {
        // register policies
        String json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(null, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID, createAnyClientConditionConfig())
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        try {
            updatePolicies(json);
            fail();
        } catch (ClientPolicyException cpe) {
            assertEquals("update policies failed", cpe.getError());
        }
    }

    @Test
    public void testConfidentialClientAcceptExecutorExecutor() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Erstes Profil")
                        .addExecutor(ConfidentialClientAcceptExecutorFactory.PROVIDER_ID, null)
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Erstes Politik", Boolean.TRUE)
                        .addCondition(ClientRolesConditionFactory.PROVIDER_ID,
                                createClientRolesConditionConfig(List.of(SAMPLE_CLIENT_ROLE)))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        String clientConfidentialId = generateSuffixedName("confidential-app");
        String clientConfidentialSecret = "app-secret";
        String cidConfidential = createClientByAdmin(clientConfidentialId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret(clientConfidentialSecret);
            clientRep.setStandardFlowEnabled(Boolean.TRUE);
            clientRep.setImplicitFlowEnabled(Boolean.TRUE);
            clientRep.setPublicClient(Boolean.FALSE);
            clientRep.setBearerOnly(Boolean.FALSE);
        });
        adminClient.realm(REALM_NAME).clients().get(cidConfidential).roles().create(RoleBuilder.create().name(SAMPLE_CLIENT_ROLE).build());

        successfulLoginAndLogout(clientConfidentialId, clientConfidentialSecret);

        String clientPublicId = generateSuffixedName("public-app");
        String cidPublic = createClientByAdmin(clientPublicId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret(clientConfidentialSecret);
            clientRep.setStandardFlowEnabled(Boolean.TRUE);
            clientRep.setImplicitFlowEnabled(Boolean.TRUE);
            clientRep.setPublicClient(Boolean.TRUE);
            clientRep.setBearerOnly(Boolean.FALSE);
        });
        adminClient.realm(REALM_NAME).clients().get(cidPublic).roles().create(RoleBuilder.create().name(SAMPLE_CLIENT_ROLE).build());

        oauth.client(clientPublicId);
        oauth.openLoginForm();
        assertEquals(OAuthErrorException.INVALID_CLIENT, oauth.parseLoginResponse().getError());
        assertEquals("invalid client access type", oauth.parseLoginResponse().getErrorDescription());
    }

    @Test
    public void testConsentRequiredExecutorExecutor() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Test Profile")
                        .addExecutor(ConsentRequiredExecutorFactory.PROVIDER_ID, createConsentRequiredExecutorConfig(true))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Test Policy", Boolean.TRUE)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID,
                                createAnyClientConditionConfig())
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // Client will be auto-configured to enable consentRequired
        String clientId = generateSuffixedName("aaa-app");
        String cid = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setImplicitFlowEnabled(Boolean.FALSE);
            clientRep.setConsentRequired(Boolean.FALSE);
        });
        ClientRepresentation clientRep = getClientByAdmin(cid);
        assertEquals(Boolean.TRUE, clientRep.isConsentRequired());

        // Client cannot be updated to disable consentRequired
        updateClientByAdmin(cid, (ClientRepresentation cRep) -> cRep.setConsentRequired(Boolean.FALSE));
        clientRep = getClientByAdmin(cid);
        assertEquals(Boolean.TRUE, clientRep.isConsentRequired());

        // Switch auto-configure to false. Auto-configuration won't happen, but validation will still be here, so should not be possible to disable consentRequired
        json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Test Profile")
                        .addExecutor(ConsentRequiredExecutorFactory.PROVIDER_ID, createConsentRequiredExecutorConfig(false))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // Not possible to register client with consentRequired due the validation
        try {
            createClientByAdmin(clientId, (ClientRepresentation clientRep2) -> clientRep2.setConsentRequired(Boolean.FALSE));
            fail();
        } catch (ClientPolicyException cpe) {
            assertEquals(Errors.INVALID_REGISTRATION, cpe.getError());
        }

        // Not possible to update existing client to consentRequired due the validation
        try {
            updateClientByAdmin(cid, (ClientRepresentation cRep) -> cRep.setConsentRequired(Boolean.FALSE));
            fail();
        } catch (ClientPolicyException cpe) {
            assertEquals(Errors.INVALID_REGISTRATION, cpe.getError());
        }
        clientRep = getClientByAdmin(cid);
        assertEquals(Boolean.TRUE, clientRep.isConsentRequired());

        try {
            updateClientByAdmin(cid, (ClientRepresentation cRep) -> cRep.setImplicitFlowEnabled(Boolean.TRUE));
            clientRep = getClientByAdmin(cid);
            assertEquals(Boolean.TRUE, clientRep.isImplicitFlowEnabled());
            assertEquals(Boolean.TRUE, clientRep.isConsentRequired());
        } catch (ClientPolicyException cpe) {
            fail();
        }
    }

    @Test
    public void testConsentRequiredExecutorWithClientRolesCondition() throws Exception {
        // register profiles with consent-required executor
        updateProfiles(new ClientProfilesBuilder().addProfile(
                new ClientProfileBuilder().createProfile(PROFILE_NAME, "Test Profile")
                        .addExecutor(ConsentRequiredExecutorFactory.PROVIDER_ID, createConsentRequiredExecutorConfig(true))
                        .toRepresentation()).toString());

        // register policies with the client-roles condition to sample-client-role
        updatePolicies(new ClientPoliciesBuilder().addPolicy(
                new ClientPolicyBuilder().createPolicy(POLICY_NAME, "Test Policy", Boolean.TRUE)
                        .addCondition(ClientRolesConditionFactory.PROVIDER_ID,
                                createClientRolesConditionConfig(List.of(SAMPLE_CLIENT_ROLE)))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()).toString());

        // Client is allowed to be created without consent because no roles at creation time
        String clientId = generateSuffixedName("consent-app");
        String cid = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setImplicitFlowEnabled(Boolean.FALSE);
            clientRep.setConsentRequired(Boolean.FALSE);
        });
        Assert.assertFalse(getClientByAdmin(cid).isConsentRequired());

        // add the role to the client to execute condition
        adminClient.realm(REALM_NAME).clients().get(cid).roles().create(RoleBuilder.create().name(SAMPLE_CLIENT_ROLE).build());

        // update with consent to false should be updated to true by autoconfigure
        updateClientByAdmin(cid, (ClientRepresentation cRep) -> cRep.setConsentRequired(Boolean.FALSE));
        Assert.assertTrue(getClientByAdmin(cid).isConsentRequired());
    }

    @Test
    public void testFullScopeDisabledExecutor() throws Exception {
        // register profiles - client autoConfigured to disable fullScopeAllowed
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Test Profile")
                        .addExecutor(FullScopeDisabledExecutorFactory.PROVIDER_ID, createFullScopeDisabledExecutorConfig(true))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Test Policy", Boolean.TRUE)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID,
                                createAnyClientConditionConfig())
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // Client will be auto-configured to disable fullScopeAllowed
        String clientId = generateSuffixedName("aaa-app");
        String cid = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setImplicitFlowEnabled(Boolean.FALSE);
            clientRep.setFullScopeAllowed(Boolean.TRUE);
        });
        ClientRepresentation clientRep = getClientByAdmin(cid);
        assertEquals(Boolean.FALSE, clientRep.isFullScopeAllowed());

        // Client cannot be updated to disable fullScopeAllowed
        updateClientByAdmin(cid, (ClientRepresentation cRep) -> cRep.setFullScopeAllowed(Boolean.TRUE));
        clientRep = getClientByAdmin(cid);
        assertEquals(Boolean.FALSE, clientRep.isFullScopeAllowed());

        // Switch auto-configure to false. Auto-configuration won't happen, but validation will still be here, so should not be possible to enable fullScopeAllowed
        json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Test Profile")
                        .addExecutor(FullScopeDisabledExecutorFactory.PROVIDER_ID, createFullScopeDisabledExecutorConfig(false))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // Not possible to register client with fullScopeAllowed due the validation
        try {
            createClientByAdmin(clientId, (ClientRepresentation clientRep2) -> clientRep2.setFullScopeAllowed(Boolean.TRUE));
            fail();
        } catch (ClientPolicyException cpe) {
            assertEquals(Errors.INVALID_REGISTRATION, cpe.getError());
        }

        // Not possible to update existing client to fullScopeAllowed due the validation
        try {
            updateClientByAdmin(cid, (ClientRepresentation cRep) -> cRep.setFullScopeAllowed(Boolean.TRUE));
            fail();
        } catch (ClientPolicyException cpe) {
            assertEquals(Errors.INVALID_REGISTRATION, cpe.getError());
        }
        clientRep = getClientByAdmin(cid);
        assertEquals(Boolean.FALSE, clientRep.isFullScopeAllowed());

        try {
            updateClientByAdmin(cid, (ClientRepresentation cRep) -> cRep.setImplicitFlowEnabled(Boolean.TRUE));
            clientRep = getClientByAdmin(cid);
            assertEquals(Boolean.TRUE, clientRep.isImplicitFlowEnabled());
            assertEquals(Boolean.FALSE, clientRep.isFullScopeAllowed());
        } catch (ClientPolicyException cpe) {
            fail();
        }
    }

    @Test
    public void testRejectResourceOwnerCredentialsGrantExecutor() throws Exception {

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
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Purofairu desu")
                        .addExecutor(RejectResourceOwnerPasswordCredentialsGrantExecutorFactory.PROVIDER_ID,
                                createRejectisResourceOwnerPasswordCredentialsGrantExecutorConfig(Boolean.TRUE))
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

        oauth.client(clientId, clientSecret);
        AccessTokenResponse response = oauth.doPasswordGrantRequest(TEST_USER_NAME, TEST_USER_PASSWORD);

        assertEquals(400, response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_GRANT, response.getError());
        assertEquals("resource owner password credentials grant is prohibited.", response.getErrorDescription());

    }

    @Test
    public void testRejectRequestExecutor() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(RejectRequestExecutorFactory.PROVIDER_ID, null)
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

        try {
            oauth.client(clientBetaId);
            oauth.openLoginForm();
            assertTrue(errorPage.isCurrent());
            assertEquals(ERR_MSG_REQ_NOT_ALLOWED, errorPage.getError());
            events.expectClientPolicyError(EventType.LOGIN_ERROR, OAuthErrorException.INVALID_REQUEST,
                            Details.CLIENT_POLICY_ERROR, OAuthErrorException.INVALID_REQUEST,
                            ERR_MSG_REQ_NOT_ALLOWED).client((String) null).user((String) null).assertEvent();

            revertToBuiltinProfiles();
            successfulLoginAndLogout(clientBetaId, "secretBeta");
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * When creating a dynamic client the secret expiration date must be defined
     *
     * @throws Exception
     */
    @Test
    public void whenCreateDynamicClientSecretExpirationDateMustExist() throws Exception {

        //enable policy
        configureCustomProfileAndPolicy(60, 30, 20);

        String clientId = createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {
        });
        OIDCClientRepresentation response = getClientDynamically(clientId);
        assertThat(response.getClientSecret(), notNullValue());
        assertThat(response.getClientSecretExpiresAt(), greaterThan(0));

    }

    /**
     * When update a dynamic client the secret expiration date must be defined and the rotation process must obey the policy configuration
     *
     * @throws Exception
     */
    @Test
    public void whenUpdateDynamicClientRotationMustFollowConfiguration() throws Exception {

        //enable policy
        configureCustomProfileAndPolicy(60, 30, 20);

        String clientId = createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {
        });
        OIDCClientRepresentation response = getClientDynamically(clientId);

        String firstSecret = response.getClientSecret();
        Integer firstSecretExpiration = response.getClientSecretExpiresAt();

        updateClientDynamically(clientId, (OIDCClientRepresentation clientRep) -> clientRep.setContacts(Collections.singletonList("keycloak@keycloak.org")));

        OIDCClientRepresentation updated = getClientDynamically(clientId);

        //secret rotation must NOT occur
        assertThat(updated.getClientSecret(), equalTo(firstSecret));
        assertThat(updated.getClientSecretExpiresAt(), equalTo(firstSecretExpiration));

        //force secret expiration
        setTimeOffset(61);

        updateClientDynamically(clientId, (OIDCClientRepresentation clientRep) -> clientRep.setClientName(generateSuffixedName(CLIENT_NAME)));

        updated = getClientDynamically(clientId);
        String updatedSecret = updated.getClientSecret();

        //secret rotation must occur
        assertThat(updatedSecret, not(equalTo(firstSecret)));
        assertThat(updated.getClientSecretExpiresAt(), not(equalTo(firstSecretExpiration)));

        //login with updated secret
        assertLoginAndLogoutStatus(clientId, updatedSecret, Response.Status.OK);

        //login with rotated secret
        assertLoginAndLogoutStatus(clientId, firstSecret, Response.Status.OK);

        //force rotated secret expiration
        setTimeOffset(100);

        //login with updated secret (remains valid)
        assertLoginAndLogoutStatus(clientId, updatedSecret, Response.Status.OK);

        //try to log in with rotated secret (must fail)
        assertLoginAndLogoutStatus(clientId, firstSecret, Response.Status.UNAUTHORIZED);

    }

    /**
     * When updating a dynamic client within the "time remaining to expiration" period the client secret must be rotated and
     * the new secret must be sent along with the new expiration date.
     * Even though the client secret is still valid, the time remaining setting should force rotation
     *
     * @throws Exception
     */
    @Test
    public void whenUpdateDynamicClientDuringRemainingExpirationPeriodMustRotateSecret() throws Exception {

        //enable policy
        configureCustomProfileAndPolicy(60, 30, 20);

        String clientId = createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {
        });
        OIDCClientRepresentation response = getClientDynamically(clientId);

        String firstSecret = response.getClientSecret();
        Integer firstSecretExpiration = response.getClientSecretExpiresAt();

        assertThat(firstSecretExpiration, is(greaterThan(Time.currentTime())));

        //Enter in Remaining expiration window
        setTimeOffset(41);

        //update client to force rotation (due to remaining expiration)
        updateClientDynamically(clientId, (OIDCClientRepresentation clientRep) -> clientRep.setContacts(Collections.singletonList("keycloak@keycloak.org")));

        OIDCClientRepresentation updated = getClientDynamically(clientId);

        //secret rotation must occur
        assertThat(updated.getClientSecret(), not(equalTo(firstSecret)));
        assertThat(updated.getClientSecretExpiresAt(), not(equalTo(firstSecretExpiration)));

    }

    @Test
    public void testIntentClientBindCheck() throws Exception {
        final String intentName = "openbanking_intent_id";

        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Het Eerste Profiel")
                        .addExecutor(IntentClientBindCheckExecutorFactory.PROVIDER_ID,
                                createIntentClientBindCheckExecutorConfig(intentName, TestApplicationResourceUrls.checkIntentClientBoundUri()))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Het Eerste Beleid", Boolean.TRUE)
                        .addCondition(ClientScopesConditionFactory.PROVIDER_ID,
                                createClientScopesConditionConfig(ClientScopesConditionFactory.OPTIONAL, List.of("microprofile-jwt")))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // create a client
        String clientId = generateSuffixedName(CLIENT_NAME);
        String clientSecret = "secret";
        createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret(clientSecret);
            clientRep.setStandardFlowEnabled(Boolean.TRUE);
            clientRep.setImplicitFlowEnabled(Boolean.TRUE);
        });
        ClientResource app = findClientResourceByClientId(adminClient.realm("test"), clientId);
        assert app != null;
        ProtocolMappersResource res = app.getProtocolMappers();
        res.createMapper(ModelToRepresentation.toRepresentation(ClaimsParameterWithValueIdTokenMapper.createMapper("claimsParameterWithValueIdTokenMapper", "openbanking_intent_id", true))).close();

        // register a binding of an intent with different client
        String intentId = "123abc456xyz";
        String differentClientId = "test-app";
        Response r = testingClient.testApp().oidcClientEndpoints().bindIntentWithClient(intentId, differentClientId);
        assertEquals(204, r.getStatus());

        // create a request object with claims
        String nonce = "naodfejawi37d";

        ClaimsRepresentation claimsRep = new ClaimsRepresentation();
        ClaimsRepresentation.ClaimValue<String> claimValue = new ClaimsRepresentation.ClaimValue<>();
        claimValue.setEssential(Boolean.TRUE);
        claimValue.setValue(intentId);
        claimsRep.setIdTokenClaims(Collections.singletonMap(intentName, claimValue));

        Map<String, Object> oidcRequest = new HashMap<>();
        oidcRequest.put(OIDCLoginProtocol.CLIENT_ID_PARAM, clientId);
        oidcRequest.put(OIDCLoginProtocol.NONCE_PARAM, nonce);
        oidcRequest.put(OIDCLoginProtocol.RESPONSE_TYPE_PARAM, OIDCResponseType.CODE + " " + OIDCResponseType.ID_TOKEN);
        oidcRequest.put(OIDCLoginProtocol.REDIRECT_URI_PARAM, oauth.getRedirectUri());
        oidcRequest.put(OIDCLoginProtocol.CLAIMS_PARAM, claimsRep);
        oidcRequest.put(OIDCLoginProtocol.SCOPE_PARAM, "openid" + " " + "microprofile-jwt");
        String request = new JWSBuilder().jsonContent(oidcRequest).none();

        // send an authorization request
        oauth.scope("openid" + " " + "microprofile-jwt");
        oauth.client(clientId, clientSecret);
        oauth.responseType(OIDCResponseType.CODE + " " + OIDCResponseType.ID_TOKEN);
        oauth.loginForm().nonce(nonce).request(request).open();
        AuthorizationEndpointResponse authorizationEndpointResponse = oauth.parseLoginResponse();
        assertEquals(OAuthErrorException.INVALID_REQUEST, authorizationEndpointResponse.getError());
        assertEquals("The intent is not bound with the client", authorizationEndpointResponse.getErrorDescription());
        events.expectClientPolicyError(EventType.LOGIN_ERROR, OAuthErrorException.INVALID_REQUEST,
                Details.CLIENT_POLICY_ERROR, OAuthErrorException.INVALID_REQUEST,
                "The intent is not bound with the client").client(clientId).user((String) null)
                .assertEvent();

        // register a binding of an intent with a valid client
        r = testingClient.testApp().oidcClientEndpoints().bindIntentWithClient(intentId, clientId);
        assertEquals(204, r.getStatus());

        // send an authorization request
        oauth.loginForm().request(request).doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

        // check an authorization response
        EventRepresentation loginEvent = events.expectLogin().client(clientId).assertEvent();
        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);
        String code = oauth.parseLoginResponse().getCode();
        AuthorizationEndpointResponse authzResponse = oauth.parseLoginResponse();
        JWSInput idToken = new JWSInput(authzResponse.getIdToken());
        ObjectMapper mapper = JsonSerialization.mapper;
        JsonParser parser = mapper.getFactory().createParser(idToken.readContentAsString());
        TreeNode treeNode = mapper.readTree(parser);
        String clientBoundIntentId = ((TextNode) treeNode.get(intentName)).asText();
        assertEquals(intentId, clientBoundIntentId);

        // send a token request
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);

        // check a token response
        assertEquals(200, response.getStatusCode());
        events.expectCodeToToken(codeId, sessionId).client(clientId).assertEvent();
        idToken = new JWSInput(response.getIdToken());
        parser = mapper.getFactory().createParser(idToken.readContentAsString());
        treeNode = mapper.readTree(parser);
        clientBoundIntentId = ((TextNode) treeNode.get(intentName)).asText();
        assertEquals(intentId, clientBoundIntentId);

        // logout
        oauth.doLogout(response.getRefreshToken());
        events.expectLogout(response.getSessionState()).client(clientId).clearDetails().assertEvent();

        // create a request object with invalid claims
        claimsRep = new ClaimsRepresentation();
        claimValue = new ClaimsRepresentation.ClaimValue<>();
        claimValue.setEssential(Boolean.TRUE);
        claimValue.setValue(intentId);
        claimsRep.setIdTokenClaims(Collections.singletonMap("other_intent_id", claimValue));
        oidcRequest.put(OIDCLoginProtocol.CLAIMS_PARAM, claimsRep);
        request = new JWSBuilder().jsonContent(oidcRequest).none();

        // send an authorization request
        oauth.loginForm().request(request).open();
        authorizationEndpointResponse = oauth.parseLoginResponse();
        assertEquals(OAuthErrorException.INVALID_REQUEST, authorizationEndpointResponse.getError());
        assertEquals("no claim for an intent value for ID token" , authorizationEndpointResponse.getErrorDescription());
        events.expectClientPolicyError(EventType.LOGIN_ERROR, OAuthErrorException.INVALID_REQUEST,
                        Details.CLIENT_POLICY_ERROR, OAuthErrorException.INVALID_REQUEST,
                        "no claim for an intent value for ID token").client(clientId)
                .user((String) null).assertEvent();
    }

    @Test
    public void testRejectImplicitGrantExecutor() throws Exception {

        String clientId = generateSuffixedName(CLIENT_NAME);
        String clientSecret = "secret";

        createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret(clientSecret);
            clientRep.setStandardFlowEnabled(Boolean.TRUE);
            clientRep.setImplicitFlowEnabled(Boolean.TRUE);
            clientRep.setPublicClient(Boolean.FALSE);
        });

        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Az Elso Profil")
                    .addExecutor(RejectImplicitGrantExecutorFactory.PROVIDER_ID,
                        createRejectImplicitGrantExecutorConfig(Boolean.TRUE))
                    .toRepresentation()
                ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Az Elso Politika", Boolean.TRUE)
                    .addCondition(AnyClientConditionFactory.PROVIDER_ID, 
                        createAnyClientConditionConfig())
                    .addProfile(PROFILE_NAME)
                    .toRepresentation()
                ).toString();
        updatePolicies(json);

        try {
            String expectedErrorDescription = "Implicit/Hybrid flow is prohibited.";
            oauth.client(clientId, clientSecret);

            // implicit grant
            testProhibitedImplicitOrHybridFlow(false, OIDCResponseType.TOKEN, null, OAuthErrorException.INVALID_REQUEST, expectedErrorDescription);

            // hybrid grant
            testProhibitedImplicitOrHybridFlow(true, OIDCResponseType.TOKEN + " " + OIDCResponseType.ID_TOKEN, "exsefweag", OAuthErrorException.INVALID_REQUEST, expectedErrorDescription);

            // hybrid grant
            testProhibitedImplicitOrHybridFlow(true, OIDCResponseType.TOKEN + " " + OIDCResponseType.CODE, "exsefweag", OAuthErrorException.INVALID_REQUEST, expectedErrorDescription);

            // hybrid grant
            testProhibitedImplicitOrHybridFlow(true, OIDCResponseType.TOKEN + " " + OIDCResponseType.CODE + " " + OIDCResponseType.ID_TOKEN, "exsefweag", OAuthErrorException.INVALID_REQUEST, expectedErrorDescription);

            //
            // Pushed Authorization Request
            //

            // implicit grant
            testProhibitedImplicitOrHybridFlowOnPARRequest(false, OIDCResponseType.TOKEN, "evawieak39j", OAuthErrorException.INVALID_REQUEST, expectedErrorDescription);

            // hybrid grant
            testProhibitedImplicitOrHybridFlowOnPARRequest(true, OIDCResponseType.TOKEN + " " + OIDCResponseType.ID_TOKEN, "ob937kcoiei3", OAuthErrorException.INVALID_REQUEST, expectedErrorDescription);

            // hybrid grant
            testProhibitedImplicitOrHybridFlowOnPARRequest(true, OIDCResponseType.TOKEN + " " + OIDCResponseType.CODE, "xiensoi3", OAuthErrorException.INVALID_REQUEST, expectedErrorDescription);

            // hybrid grant
            testProhibitedImplicitOrHybridFlowOnPARRequest(true, OIDCResponseType.TOKEN + " " + OIDCResponseType.CODE + " " + OIDCResponseType.ID_TOKEN, "bor9v8uoan", OAuthErrorException.INVALID_REQUEST, expectedErrorDescription);

            // authorization code grant
            testAllowedAuthorizationCodeFlowOnPARRequest(true, "ddab9e88");
        } finally {
            // revert test client instance settings the same as OAuthClient.init
            oauth.openid(true);
            oauth.responseType(OIDCResponseType.CODE);
        }
    }

    @Test
    public void testClientGrantTypeCondition() throws Exception {

        String clientId = generateSuffixedName(CLIENT_NAME);
        String clientSecret = "secret";
        createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret(clientSecret);
            clientRep.setServiceAccountsEnabled(true);
            clientRep.setImplicitFlowEnabled(true);
            clientRep.setStandardFlowEnabled(true);
            clientRep.setAttributes(Map.of(OIDCConfigAttributes.STANDARD_TOKEN_EXCHANGE_ENABLED, "true"));
        });
        oauth.client(clientId, clientSecret);

        //auth code
        successfulLogin(clientId, clientSecret);

        configureClientPolicyToBlockGrantTypes(ClientPolicyEvent.AUTHORIZATION_REQUEST, List.of(OAuth2Constants.AUTHORIZATION_CODE));
        oauth.openLogoutForm();
        oauth.openLoginForm();
        MultivaluedHashMap<String, String> queryParams = UriUtils.decodeQueryString(new URL(Objects.requireNonNull(driver.getCurrentUrl())).getQuery());
        assertEquals(ClientPolicyEvent.AUTHORIZATION_REQUEST.toString(), queryParams.getFirst("error"));
        assertEquals("Exception thrown intentionally", queryParams.getFirst("error_description"));
        revertToBuiltinPolicies();

        //password
        AccessTokenResponse response = oauth.doPasswordGrantRequest(TEST_USER_NAME, TEST_USER_PASSWORD);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());

        configureClientPolicyToBlockGrantTypes(ClientPolicyEvent.RESOURCE_OWNER_PASSWORD_CREDENTIALS_REQUEST, List.of(OAuth2Constants.PASSWORD));
        response = oauth.doPasswordGrantRequest(TEST_USER_NAME, TEST_USER_PASSWORD);
        assertGrantTypeBlock(response, ClientPolicyEvent.RESOURCE_OWNER_PASSWORD_CREDENTIALS_REQUEST);
        revertToBuiltinPolicies();

        //client credentials
        response = oauth.clientCredentialsGrantRequest().send();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());

        configureClientPolicyToBlockGrantTypes(ClientPolicyEvent.SERVICE_ACCOUNT_TOKEN_REQUEST, List.of(OAuth2Constants.CLIENT_CREDENTIALS));
        response = oauth.clientCredentialsGrantRequest().send();
        assertGrantTypeBlock(response, ClientPolicyEvent.SERVICE_ACCOUNT_TOKEN_REQUEST);
        revertToBuiltinPolicies();

        //refresh
        response = oauth.doPasswordGrantRequest(TEST_USER_NAME, TEST_USER_PASSWORD);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());
        response = oauth.refreshRequest(response.getRefreshToken()).send();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());

        configureClientPolicyToBlockGrantTypes(ClientPolicyEvent.TOKEN_REFRESH, List.of(OAuth2Constants.REFRESH_TOKEN));
        response = oauth.refreshRequest(response.getRefreshToken()).send();
        assertGrantTypeBlock(response, ClientPolicyEvent.TOKEN_REFRESH);
        revertToBuiltinPolicies();

        //token exchange
        response = oauth.doPasswordGrantRequest(TEST_USER_NAME, TEST_USER_PASSWORD);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());
        response = oauth.tokenExchangeRequest(response.getAccessToken()).send();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());

        configureClientPolicyToBlockGrantTypes(ClientPolicyEvent.TOKEN_EXCHANGE_REQUEST, List.of(OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE));
        response = oauth.tokenExchangeRequest(response.getAccessToken()).send();
        assertGrantTypeBlock(response, ClientPolicyEvent.TOKEN_EXCHANGE_REQUEST);
        revertToBuiltinPolicies();
    }

    private void assertGrantTypeBlock(AccessTokenResponse response, ClientPolicyEvent event){
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals(event.toString(), response.getError());
        assertEquals("Exception thrown intentionally", response.getErrorDescription());
    }

    private void configureClientPolicyToBlockGrantTypes(ClientPolicyEvent event, List<String> grantTypes) throws Exception {

        String json = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                (new ClientPoliciesUtil.ClientProfileBuilder()).createProfile(PROFILE_NAME, "Profilo")
                        .addExecutor(TestRaiseExceptionExecutorFactory.PROVIDER_ID,
                                createTestRaiseExeptionExecutorConfig(List.of(event)))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policy with condition on client scope optional-scope2
        json = (new ClientPoliciesUtil.ClientPoliciesBuilder()).addPolicy(
                (new ClientPoliciesUtil.ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Client Policy", Boolean.TRUE)
                        .addCondition(GrantTypeConditionFactory.PROVIDER_ID,
                                createGrantTypeConditionConfig(grantTypes))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);
    }

    private void testProhibitedImplicitOrHybridFlow(boolean isOpenid, String responseType, String nonce, String expectedError, String expectedErrorDescription) {
        oauth.openid(isOpenid);
        oauth.responseType(responseType);
        oauth.loginForm().nonce(nonce).open();
        AuthorizationEndpointResponse authorizationEndpointResponse = oauth.parseLoginResponse();
        assertEquals(expectedError, authorizationEndpointResponse.getError());
        assertEquals(expectedErrorDescription, authorizationEndpointResponse.getErrorDescription());
    }

    private void testProhibitedImplicitOrHybridFlowOnPARRequest(boolean isOpenid, String responseType, String nonce, String expectedError, String expectedErrorDescription) {
        oauth.openid(isOpenid);
        oauth.responseType(responseType);
        ParResponse pResp = oauth.pushedAuthorizationRequest().nonce(nonce).send();
        assertEquals(expectedError, pResp.getError());
        assertEquals(expectedErrorDescription, pResp.getErrorDescription());
    }

    private void testAllowedAuthorizationCodeFlowOnPARRequest(boolean isOpenid, String nonce) {
        oauth.openid(isOpenid);
        oauth.responseType(OAuth2Constants.CODE);
        ParResponse pResp = oauth.pushedAuthorizationRequest().nonce(nonce).send();
        assertEquals(201, pResp.getStatusCode());
    }
}

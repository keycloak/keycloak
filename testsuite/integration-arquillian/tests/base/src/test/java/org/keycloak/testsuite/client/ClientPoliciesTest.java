/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.admin.ApiUtil.findUserByUsername;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.BadRequestException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.hamcrest.Matchers;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientSecretAuthenticator;
import org.keycloak.authentication.authenticators.client.X509ClientAuthenticator;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.common.Profile;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.Algorithm;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.representations.oidc.TokenMetadataRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.DefaultClientPolicyProviderFactory;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientAccessTypeConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdateContextConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdateSourceGroupsConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdateSourceHostsConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdateSourceRolesConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientRolesConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientScopesConditionFactory;
import org.keycloak.services.clientpolicy.executor.HolderOfKeyEnforceExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureClientAuthEnforceExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureRedirectUriEnforceExecutorFactory;
import org.keycloak.services.clientpolicy.executor.PKCEEnforceExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureRequestObjectExecutor;
import org.keycloak.services.clientpolicy.executor.SecureRequestObjectExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureResponseTypeExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureSessionEnforceExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureSigningAlgorithmEnforceExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureSigningAlgorithmForSignedJwtEnforceExecutorFactory;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.client.resources.TestApplicationResourceUrls;
import org.keycloak.testsuite.rest.resource.TestingOIDCEndpointsApplicationResource.AuthorizationEndpointRequestObject;
import org.keycloak.testsuite.services.clientpolicy.condition.TestRaiseExeptionConditionFactory;
import org.keycloak.testsuite.util.MutualTLSUtils;
import org.keycloak.testsuite.util.OAuthClient;

import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.testsuite.util.RoleBuilder;

import org.keycloak.util.JsonSerialization;

@EnableFeature(value = Profile.Feature.CLIENT_POLICIES, skipRestart = true)
public class ClientPoliciesTest extends AbstractClientPoliciesTest {

    private static final Logger logger = Logger.getLogger(ClientPoliciesTest.class);

    private static final String POLICY_NAME = "MyPolicy";
    private static final String CLIENT_NAME = "Zahlungs-App";
    private static final String SAMPLE_CLIENT_ROLE = "sample-client-role";
    private static final String TEST_USER_NAME = "test-user@localhost";
    private static final String TEST_USER_PASSWORD = "password";

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

        testRealms.add(realm);
    }

    @Test
    public void testAdminClientRegisterUnacceptableAuthType() {
        setupPolicyClientIdAndSecretNotAcceptableAuthType(POLICY_NAME);
        try {
            createClientByAdmin(generateSuffixedName(CLIENT_NAME), (ClientRepresentation clientRep) -> {
                clientRep.setClientAuthenticatorType(ClientIdAndSecretAuthenticator.PROVIDER_ID);
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(Errors.INVALID_REGISTRATION, e.getMessage());
        }
    }

    @Test
    public void testAdminClientRegisterAcceptableAuthType() throws ClientPolicyException {
        setupPolicyClientIdAndSecretNotAcceptableAuthType(POLICY_NAME);
        String cId = createClientByAdmin(generateSuffixedName(CLIENT_NAME), (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(JWTClientSecretAuthenticator.PROVIDER_ID);
        });
        assertEquals(JWTClientSecretAuthenticator.PROVIDER_ID, getClientByAdmin(cId).getClientAuthenticatorType());
    }

    @Test
    public void testAdminClientRegisterDefaultAuthType() {
        setupPolicyClientIdAndSecretNotAcceptableAuthType(POLICY_NAME);
        try {
            createClientByAdmin(generateSuffixedName(CLIENT_NAME), (ClientRepresentation clientRep) -> {});
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(Errors.INVALID_REGISTRATION, e.getMessage());
        }
    }

    @Test
    public void testAdminClientUpdateUnacceptableAuthType() throws ClientPolicyException {
        setupPolicyClientIdAndSecretNotAcceptableAuthType(POLICY_NAME);
        String cId = createClientByAdmin(generateSuffixedName(CLIENT_NAME), (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(JWTClientSecretAuthenticator.PROVIDER_ID);
        });
        assertEquals(JWTClientSecretAuthenticator.PROVIDER_ID, getClientByAdmin(cId).getClientAuthenticatorType());
        try {
            updateClientByAdmin(cId, (ClientRepresentation clientRep) -> {
                clientRep.setClientAuthenticatorType(ClientIdAndSecretAuthenticator.PROVIDER_ID);
            });
            fail();
        } catch (BadRequestException bre) {
            assertEquals("HTTP 400 Bad Request", bre.getMessage());
        }
        assertEquals(JWTClientSecretAuthenticator.PROVIDER_ID, getClientByAdmin(cId).getClientAuthenticatorType());
    }

    @Test
    public void testAdminClientUpdateAcceptableAuthType() throws ClientPolicyException {
        setupPolicyClientIdAndSecretNotAcceptableAuthType(POLICY_NAME);

        String cId = createClientByAdmin(generateSuffixedName(CLIENT_NAME), (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(JWTClientSecretAuthenticator.PROVIDER_ID);
        });

        assertEquals(JWTClientSecretAuthenticator.PROVIDER_ID, getClientByAdmin(cId).getClientAuthenticatorType());

        updateClientByAdmin(cId, (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID);
        });
        assertEquals(JWTClientAuthenticator.PROVIDER_ID, getClientByAdmin(cId).getClientAuthenticatorType());
    }

    @Test
    public void testAdminClientUpdateDefaultAuthType() throws ClientPolicyException {
        setupPolicyClientIdAndSecretNotAcceptableAuthType(POLICY_NAME);

        String cId = createClientByAdmin(generateSuffixedName(CLIENT_NAME), (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(JWTClientSecretAuthenticator.PROVIDER_ID);
        });

        assertEquals(JWTClientSecretAuthenticator.PROVIDER_ID, getClientByAdmin(cId).getClientAuthenticatorType());

        updateClientByAdmin(cId, (ClientRepresentation clientRep) -> {
            clientRep.setServiceAccountsEnabled(Boolean.FALSE);
        });
        assertEquals(JWTClientSecretAuthenticator.PROVIDER_ID, getClientByAdmin(cId).getClientAuthenticatorType());
        assertEquals(Boolean.FALSE, getClientByAdmin(cId).isServiceAccountsEnabled());
    }

    @Test
    public void testAdminClientAugmentedAuthType() throws ClientPolicyException {
        setupPolicyClientIdAndSecretNotAcceptableAuthType(POLICY_NAME);

        updateExecutor(SecureClientAuthEnforceExecutor_NAME, (ComponentRepresentation provider) -> {
            setExecutorAugmentActivate(provider);
            setExecutorAugmentedClientAuthMethod(provider, X509ClientAuthenticator.PROVIDER_ID);
        });

        String cId = createClientByAdmin(generateSuffixedName(CLIENT_NAME), (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(ClientIdAndSecretAuthenticator.PROVIDER_ID);
        });

        assertEquals(X509ClientAuthenticator.PROVIDER_ID, getClientByAdmin(cId).getClientAuthenticatorType());

        updateExecutor(SecureClientAuthEnforceExecutor_NAME, (ComponentRepresentation provider) -> {
            setExecutorAugmentedClientAuthMethod(provider, JWTClientAuthenticator.PROVIDER_ID);
        });

        updateClientByAdmin(cId, (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(JWTClientSecretAuthenticator.PROVIDER_ID);
        });
        assertEquals(JWTClientAuthenticator.PROVIDER_ID, getClientByAdmin(cId).getClientAuthenticatorType());
    }

    @Test
    public void testDynamicClientRegisterAndUpdate() throws ClientRegistrationException {
        setupPolicyClientIdAndSecretNotAcceptableAuthType(POLICY_NAME);

        String clientId = createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {});
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
    public void testCreateDeletePolicyRuntime() throws ClientRegistrationException {
        String clientId = createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {});
        OIDCClientRepresentation clientRep = getClientDynamically(clientId);
        assertEquals(OIDCLoginProtocol.CLIENT_SECRET_BASIC, clientRep.getTokenEndpointAuthMethod());
        events.expect(EventType.CLIENT_REGISTER).client(clientId).user(Matchers.isEmptyOrNullString()).assertEvent();
        events.expect(EventType.CLIENT_INFO).client(clientId).user(Matchers.isEmptyOrNullString()).assertEvent();
        adminClient.realm(REALM_NAME).clients().get(clientId).roles().create(RoleBuilder.create().name(SAMPLE_CLIENT_ROLE).build());

        successfulLoginAndLogout(clientId, clientRep.getClientSecret());

        setupPolicyAuthzCodeFlowUnderMultiPhasePolicy(POLICY_NAME);

        failLoginByNotFollowingPKCE(clientId);

        deletePolicy(POLICY_NAME);
        logger.info("... Deleted Policy : " + POLICY_NAME);

        successfulLoginAndLogout(clientId, clientRep.getClientSecret());
    }

    @Test
    public void testCreateUpdateDeleteConditionRuntime() throws ClientRegistrationException, ClientPolicyException {
        String policyName = POLICY_NAME;
        createPolicy(policyName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyName);

        String executorName = PKCEEnforceExecutor_NAME;
        createExecutor(executorName, PKCEEnforceExecutorFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setExecutorAugmentActivate(provider);
        });
        registerExecutor(executorName, policyName);
        logger.info("... Registered Executor : " + executorName);

        String clientId = generateSuffixedName(CLIENT_NAME);
        String clientSecret = "secret";
        String cid = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret(clientSecret);
        });
        adminClient.realm(REALM_NAME).clients().get(cid).roles().create(RoleBuilder.create().name(SAMPLE_CLIENT_ROLE).build());

        successfulLoginAndLogout(clientId, clientSecret);
 
        String conditionName = ClientRolesCondition_NAME;
        createCondition(conditionName, ClientRolesConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setConditionClientRoles(provider, new ArrayList<>(Arrays.asList(SAMPLE_CLIENT_ROLE)));
        });
        registerCondition(conditionName, policyName);
        logger.info("... Registered Condition : " + conditionName);

        failLoginByNotFollowingPKCE(clientId);

        updateCondition(conditionName, (ComponentRepresentation provider) -> {
            setConditionClientRoles(provider, new ArrayList<>(Arrays.asList("anothor-client-role")));
        });

        successfulLoginAndLogout(clientId, clientSecret);

        deleteCondition(conditionName, policyName);

        successfulLoginAndLogout(clientId, clientSecret);
    }

    @Test
    public void testCreateUpdateDeleteExecutorRuntime() throws ClientRegistrationException, ClientPolicyException {
        String policyName = generateSuffixedName(CLIENT_NAME);
        createPolicy(policyName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyName);

        String roleConditionName = ClientRolesCondition_NAME;
        createCondition(roleConditionName, ClientRolesConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setConditionClientRoles(provider, new ArrayList<>(Arrays.asList(SAMPLE_CLIENT_ROLE)));
        });
        registerCondition(roleConditionName, policyName);
        logger.info("... Registered Condition : " + roleConditionName);

        String updateConditionName = ClientUpdateContextCondition_NAME;
        createCondition(updateConditionName, ClientUpdateContextConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setConditionRegistrationMethods(provider, new ArrayList<>(Arrays.asList(ClientUpdateContextConditionFactory.BY_AUTHENTICATED_USER)));
        });
        registerCondition(updateConditionName, policyName);
        logger.info("... Registered Condition : " + updateConditionName);

        String clientId = generateSuffixedName(CLIENT_NAME);
        String clientSecret = "secret";
        String cid = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret(clientSecret);
        });
        adminClient.realm(REALM_NAME).clients().get(cid).roles().create(RoleBuilder.create().name(SAMPLE_CLIENT_ROLE).build());

        successfulLoginAndLogout(clientId, clientSecret);
 
        String executorName = PKCEEnforceExecutor_NAME;
        createExecutor(executorName, PKCEEnforceExecutorFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setExecutorAugmentDeactivate(provider);
        });
        registerExecutor(executorName, policyName);
        logger.info("... Registered Executor : " + executorName);

        failLoginByNotFollowingPKCE(clientId);

        updateExecutor(executorName, (ComponentRepresentation provider) -> {
           setExecutorAugmentActivate(provider);
        });

        updateClientByAdmin(cid, (ClientRepresentation clientRep) -> {
            clientRep.setServiceAccountsEnabled(Boolean.FALSE);
        });
        assertEquals(false, getClientByAdmin(cid).isServiceAccountsEnabled());
        assertEquals(OAuth2Constants.PKCE_METHOD_S256, OIDCAdvancedConfigWrapper.fromClientRepresentation(getClientByAdmin(cid)).getPkceCodeChallengeMethod());

        deleteExecutor(executorName, policyName);
        logger.info("... Deleted Executor : " + executorName);

        updateClientByAdmin(cid, (ClientRepresentation clientRep) -> {
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setPkceCodeChallengeMethod(null);
        });
        assertEquals(null, OIDCAdvancedConfigWrapper.fromClientRepresentation(getClientByAdmin(cid)).getPkceCodeChallengeMethod());

        successfulLoginAndLogout(clientId, clientSecret);
    }

    @Test
    public void testAuthzCodeFlowUnderMultiPhasePolicy() throws Exception {
        setupPolicyAuthzCodeFlowUnderMultiPhasePolicy(POLICY_NAME);

        String clientName = generateSuffixedName(CLIENT_NAME);
        String clientId = createClientDynamically(clientName, (OIDCClientRepresentation clientRep) -> {});
        events.expect(EventType.CLIENT_REGISTER).client(clientId).user(Matchers.isEmptyOrNullString()).assertEvent();
        OIDCClientRepresentation response = getClientDynamically(clientId);
        String clientSecret = response.getClientSecret();
        assertEquals(clientName, response.getClientName());
        assertEquals(OIDCLoginProtocol.CLIENT_SECRET_BASIC, response.getTokenEndpointAuthMethod());
        events.expect(EventType.CLIENT_INFO).client(clientId).user(Matchers.isEmptyOrNullString()).assertEvent();

        adminClient.realm(REALM_NAME).clients().get(clientId).roles().create(RoleBuilder.create().name(SAMPLE_CLIENT_ROLE).build());

        successfulLoginAndLogoutWithPKCE(response.getClientId(), clientSecret, TEST_USER_NAME, TEST_USER_PASSWORD);
    }

    @Test
    public void testMultiplePolicies() throws ClientRegistrationException, ClientPolicyException {
        String roleAlphaName = "sample-client-role-alpha";
        String roleBetaName = "sample-client-role-beta";
        String roleZetaName = "sample-client-role-zeta";
        String roleCommonName = "sample-client-role-common";

        String policyAlphaName = "MyPolicy-alpha";
        createPolicy(policyAlphaName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyAlphaName);

        String roleConditionAlphaName = generateSuffixedName(ClientRolesCondition_NAME);
        createCondition(roleConditionAlphaName, ClientRolesConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setConditionClientRoles(provider, new ArrayList<>(Arrays.asList(roleAlphaName, roleZetaName)));
        });
        registerCondition(roleConditionAlphaName, policyAlphaName);
        logger.info("... Registered Condition : " + roleConditionAlphaName);

        String updateConditionAlphaName = generateSuffixedName(ClientUpdateContextCondition_NAME);
        createCondition(updateConditionAlphaName, ClientUpdateContextConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setConditionRegistrationMethods(provider, new ArrayList<>(Arrays.asList(ClientUpdateContextConditionFactory.BY_AUTHENTICATED_USER)));
        });
        registerCondition(updateConditionAlphaName, policyAlphaName);
        logger.info("... Registered Condition : " + updateConditionAlphaName);

        String clientAuthExecutorAlphaName = generateSuffixedName(SecureClientAuthEnforceExecutor_NAME);
        createExecutor(clientAuthExecutorAlphaName, SecureClientAuthEnforceExecutorFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setExecutorAcceptedClientAuthMethods(provider, new ArrayList<>(Arrays.asList(ClientIdAndSecretAuthenticator.PROVIDER_ID)));
            setExecutorAugmentActivate(provider);
            setExecutorAugmentedClientAuthMethod(provider, ClientIdAndSecretAuthenticator.PROVIDER_ID);
        });
        registerExecutor(clientAuthExecutorAlphaName, policyAlphaName);
        logger.info("... Registered Executor : " + clientAuthExecutorAlphaName);

        String policyBetaName = "MyPolicy-beta";
        createPolicy(policyBetaName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyBetaName);

        String roleConditionBetaName = generateSuffixedName(ClientRolesCondition_NAME);
        createCondition(roleConditionBetaName, ClientRolesConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setConditionClientRoles(provider, new ArrayList<>(Arrays.asList(roleBetaName, roleZetaName)));
        });
        registerCondition(roleConditionBetaName, policyBetaName);
        logger.info("... Registered Condition : " + roleConditionBetaName);

        String pkceExecutorBetaName = generateSuffixedName(PKCEEnforceExecutor_NAME);
        createExecutor(pkceExecutorBetaName, PKCEEnforceExecutorFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setExecutorAugmentActivate(provider);
        });
        registerExecutor(pkceExecutorBetaName, policyBetaName);
        logger.info("... Registered Executor : " + pkceExecutorBetaName);

        String clientAlphaId = generateSuffixedName("Alpha-App");
        String clientAlphaSecret = "secretAlpha";
        String cAlphaId = createClientByAdmin(clientAlphaId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret(clientAlphaSecret);
            clientRep.setClientAuthenticatorType(JWTClientSecretAuthenticator.PROVIDER_ID);
        });
        RolesResource rolesResourceAlpha = adminClient.realm(REALM_NAME).clients().get(cAlphaId).roles();
        rolesResourceAlpha.create(RoleBuilder.create().name(roleAlphaName).build());
        rolesResourceAlpha.create(RoleBuilder.create().name(roleCommonName).build());

        String clientBetaId = generateSuffixedName("Beta-App");
        String cBetaId = createClientByAdmin(clientBetaId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret("secretBeta");
        });
        RolesResource rolesResourceBeta = adminClient.realm(REALM_NAME).clients().get(cBetaId).roles();
        rolesResourceBeta.create(RoleBuilder.create().name(roleBetaName).build());
        rolesResourceBeta.create(RoleBuilder.create().name(roleCommonName).build());

        assertEquals(ClientIdAndSecretAuthenticator.PROVIDER_ID, getClientByAdmin(cAlphaId).getClientAuthenticatorType());
        successfulLoginAndLogout(clientAlphaId, clientAlphaSecret);
        failLoginByNotFollowingPKCE(clientBetaId);
    }

    @Test
    public void testIntentionalExceptionOnCondition() throws ClientRegistrationException, ClientPolicyException {
        String policyName = POLICY_NAME;
        createPolicy(policyName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyName);

        String conditionName = TestRaiseExeptionCondition_NAME;
        createCondition(conditionName, TestRaiseExeptionConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {});
        registerCondition(conditionName, policyName);
        logger.info("... Registered Condition : " + conditionName);

        try {
            createClientByAdmin(generateSuffixedName(CLIENT_NAME), (ClientRepresentation clientRep) -> {});
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(Errors.INVALID_REGISTRATION, e.getMessage());
        }
    }

    @Test
    public void testAnyClientCondition() throws ClientRegistrationException, ClientPolicyException {
        String policyName = POLICY_NAME;
        createPolicy(policyName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyName);

        String conditionName = AnyClientCondition_NAME;
        createCondition(conditionName, AnyClientConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {});
        registerCondition(conditionName, policyName);
        logger.info("... Registered Condition : " + conditionName);

        String executorName = SecureSessionEnforceExecutor_NAME;
        createExecutor(executorName, SecureSessionEnforceExecutorFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {});
        registerExecutor(executorName, policyName);
        logger.info("... Registered Executor : " + executorName);

        String clientAlphaId = generateSuffixedName("Alpha-App");
        String clientAlphaSecret = "secretAlpha";
        createClientByAdmin(clientAlphaId, (ClientRepresentation clientRep) -> {
            clientRep.setDefaultRoles((String[]) Arrays.asList("sample-client-role-alpha").toArray(new String[1]));
            clientRep.setSecret(clientAlphaSecret);
        });

        String clientBetaId = generateSuffixedName("Beta-App");
        createClientByAdmin(clientBetaId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret("secretBeta");
        });

        try {
            failLoginWithoutSecureSessionParameter(clientBetaId, ERR_MSG_MISSING_NONCE);
            oauth.nonce("yesitisnonce");
            successfulLoginAndLogout(clientAlphaId, clientAlphaSecret);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testConditionWithoutNoConfiguration() throws ClientRegistrationException, ClientPolicyException {
        String policyName = "MyPolicy-ClientAccessTypeCondition";
        createPolicy(policyName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyName);
        String conditionName = ClientAccessTypeCondition_NAME;
        createCondition(conditionName, ClientAccessTypeConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {});
        registerCondition(conditionName, policyName);
        logger.info("... Registered Condition : " + conditionName);

        policyName = "MyPolicy-ClientUpdateSourceGroupsCondition";
        createPolicy(policyName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyName);
        conditionName = ClientUpdateSourceGroupsCondition_NAME;
        createCondition(conditionName, ClientUpdateSourceGroupsConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {});
        registerCondition(conditionName, policyName);
        logger.info("... Registered Condition : " + conditionName);

        policyName = "MyPolicy-ClientUpdateSourceRolesCondition";
        createPolicy(policyName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyName);
        conditionName = ClientUpdateSourceRolesCondition_NAME;
        createCondition(conditionName, ClientUpdateSourceRolesConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {});
        registerCondition(conditionName, policyName);
        logger.info("... Registered Condition : " + conditionName);

        policyName = "MyPolicy-ClientUpdateContextCondition";
        createPolicy(policyName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyName);
        conditionName = ClientUpdateContextCondition_NAME;
        createCondition(conditionName, ClientUpdateContextConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {});
        registerCondition(conditionName, policyName);
        logger.info("... Registered Condition : " + conditionName);

        policyName = "MyPolicy-SecureSessionEnforceExecutor";
        createPolicy(policyName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyName);
        String executorName = SecureSessionEnforceExecutor_NAME;
        createExecutor(executorName, SecureSessionEnforceExecutorFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {});
        registerExecutor(executorName, policyName);
        logger.info("... Registered Executor : " + executorName);

        String clientId = generateSuffixedName(CLIENT_NAME);
        String clientSecret = "secret";
        createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret(clientSecret);
            clientRep.setBearerOnly(Boolean.FALSE);
            clientRep.setPublicClient(Boolean.FALSE);
        });

        successfulLoginAndLogout(clientId, clientSecret);
    }

    @AuthServerContainerExclude(AuthServer.REMOTE)
    @Test
    public void testClientUpdateSourceHostsCondition() throws ClientRegistrationException, ClientPolicyException {
        String policyName = POLICY_NAME;
        createPolicy(policyName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyName);

        String conditionName = ClientUpdateSourceHostsCondition_NAME;
        createCondition(conditionName, ClientUpdateSourceHostsConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setConditionClientUpdateSourceHosts(provider, new ArrayList<>(Arrays.asList("localhost", "127.0.0.1")));
        });
        registerCondition(conditionName, policyName);
        logger.info("... Registered Condition : " + conditionName);

        String executorName = SecureClientAuthEnforceExecutor_NAME;
        createExecutor(executorName, SecureClientAuthEnforceExecutorFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setExecutorAcceptedClientAuthMethods(provider, new ArrayList<>(Arrays.asList(
                    JWTClientAuthenticator.PROVIDER_ID, JWTClientSecretAuthenticator.PROVIDER_ID, X509ClientAuthenticator.PROVIDER_ID)));
        });
        registerExecutor(executorName, policyName);
        logger.info("... Registered Executor : " + executorName);

        String clientId = generateSuffixedName(CLIENT_NAME);
        String clientSecret = "secret";
        try {
            createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
                clientRep.setSecret(clientSecret);
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(Errors.INVALID_REGISTRATION, e.getMessage());
        }

        updateCondition(conditionName, (ComponentRepresentation provider) -> {
            setConditionClientUpdateSourceHosts(provider, new ArrayList<>(Arrays.asList("example.com")));
        });
        try {
            createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
                clientRep.setSecret(clientSecret);
            });
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testClientUpdateSourceGroupsCondition() throws ClientRegistrationException, ClientPolicyException {
        String policyName = POLICY_NAME;
        createPolicy(policyName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyName);

        String conditionName = ClientUpdateSourceGroupsCondition_NAME;
        createCondition(conditionName, ClientUpdateSourceGroupsConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setConditionClientUpdateSourceGroups(provider, new ArrayList<>(Arrays.asList("topGroup")));
        });
        registerCondition(conditionName, policyName);
        logger.info("... Registered Condition : " + conditionName);

        String executorName = SecureClientAuthEnforceExecutor_NAME;
        createExecutor(executorName, SecureClientAuthEnforceExecutorFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setExecutorAcceptedClientAuthMethods(provider, new ArrayList<>(Arrays.asList(JWTClientAuthenticator.PROVIDER_ID)));
        });
        registerExecutor(executorName, policyName);
        logger.info("... Registered Executor : " + executorName);

        try {
            authCreateClients();
            createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {});
            fail();
        } catch (ClientRegistrationException e) {
            assertEquals(ERR_MSG_CLIENT_REG_FAIL, e.getMessage());
        }
        authManageClients();
        try {
            createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {});
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testClientUpdateSourceRolesCondition() throws ClientRegistrationException, ClientPolicyException {
        String policyName = POLICY_NAME;
        createPolicy(policyName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyName);

        String conditionName = ClientUpdateSourceRolesCondition_NAME;
        createCondition(conditionName, ClientUpdateSourceRolesConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setConditionUpdatingClientSourceRoles(provider, new ArrayList<>(Arrays.asList(AdminRoles.CREATE_CLIENT)));
        });
        registerCondition(conditionName, policyName);
        logger.info("... Registered Condition : " + conditionName);

        String executorName = SecureClientAuthEnforceExecutor_NAME;
        createExecutor(executorName, SecureClientAuthEnforceExecutorFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setExecutorAcceptedClientAuthMethods(provider, new ArrayList<>(Arrays.asList(JWTClientAuthenticator.PROVIDER_ID)));
        });
        registerExecutor(executorName, policyName);
        logger.info("... Registered Executor : " + executorName);

        try {
            authCreateClients();
            createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {});
            fail();
        } catch (ClientRegistrationException e) {
            assertEquals(ERR_MSG_CLIENT_REG_FAIL, e.getMessage());
        }
        authManageClients();
        try {
            createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {});
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testClientScopesCondition() throws ClientRegistrationException, ClientPolicyException {
        String policyName = POLICY_NAME;
        createPolicy(policyName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyName);

        String conditionName = ClientScopesCondition_NAME;
        createCondition(conditionName, ClientScopesConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setConditionClientScopes(provider, new ArrayList<>(Arrays.asList("offline_access", "microprofile-jwt")));
        });
        registerCondition(conditionName, policyName);
        logger.info("... Registered Condition : " + conditionName);

        String executorName = PKCEEnforceExecutor_NAME;
        createExecutor(executorName, PKCEEnforceExecutorFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setExecutorAugmentActivate(provider);
        });
        registerExecutor(executorName, policyName);
        logger.info("... Registered Executor : " + executorName);

        String clientId = generateSuffixedName(CLIENT_NAME);
        String clientSecret = "secret";
        createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret(clientSecret);
        });

        try {
            oauth.scope("address" + " " + "phone");
            successfulLoginAndLogout(clientId, clientSecret);

            oauth.scope("microprofile-jwt" + " " + "profile");
            failLoginByNotFollowingPKCE(clientId);

            oauth.scope("microprofile-jwt" + " " + "profile");
            failLoginByNotFollowingPKCE(clientId);

            successfulLoginAndLogoutWithPKCE(clientId, clientSecret, TEST_USER_NAME, TEST_USER_PASSWORD);
        } catch (Exception e) {
            fail();
        }
    }


    @Test
    public void testClientAccessTypeCondition() throws ClientRegistrationException, ClientPolicyException {
        String policyName = POLICY_NAME;
        createPolicy(policyName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyName);

        String conditionName = ClientAccessTypeCondition_NAME;
        createCondition(conditionName, ClientAccessTypeConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setConditionClientAccessType(provider, new ArrayList<>(Arrays.asList(ClientAccessTypeConditionFactory.TYPE_CONFIDENTIAL)));
        });
        registerCondition(conditionName, policyName);
        logger.info("... Registered Condition : " + conditionName);

        String executorName = SecureSessionEnforceExecutor_NAME;
        createExecutor(executorName, SecureSessionEnforceExecutorFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {});
        registerExecutor(executorName, policyName);
        logger.info("... Registered Executor : " + executorName);

        // confidential client
        String clientAlphaId = generateSuffixedName("Alpha-App");
        createClientByAdmin(clientAlphaId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret("secretAlpha");
            clientRep.setBearerOnly(Boolean.FALSE);
            clientRep.setPublicClient(Boolean.FALSE);
        });

        // public client
        String clientBetaId = generateSuffixedName("Beta-App");
        createClientByAdmin(clientBetaId, (ClientRepresentation clientRep) -> {
            clientRep.setBearerOnly(Boolean.FALSE);
            clientRep.setPublicClient(Boolean.TRUE);
        });

        successfulLoginAndLogout(clientBetaId, null);
        failLoginWithoutNonce(clientAlphaId);
    }

    @Test
    public void testSecureResponseTypeExecutor() throws ClientRegistrationException, ClientPolicyException {
        String policyName = POLICY_NAME;
        createPolicy(policyName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyName);

        String conditionName = ClientRolesCondition_NAME;
        createCondition(conditionName, ClientRolesConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setConditionClientRoles(provider, new ArrayList<>(Arrays.asList(SAMPLE_CLIENT_ROLE)));
        });
        registerCondition(conditionName, policyName);
        logger.info("... Registered Condition : " + conditionName);

        String executorName = SecureResponseTypeExecutor_NAME;
        createExecutor(executorName, SecureResponseTypeExecutorFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {});
        registerExecutor(executorName, policyName);
        logger.info("... Registered Executor : " + executorName);

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
        oauth.openLoginForm();
        assertEquals(OAuthErrorException.INVALID_REQUEST, oauth.getCurrentQuery().get(OAuth2Constants.ERROR));
        assertEquals("invalid response_type", oauth.getCurrentQuery().get(OAuth2Constants.ERROR_DESCRIPTION));

        oauth.responseType(OIDCResponseType.CODE + " " + OIDCResponseType.ID_TOKEN + " "  + OIDCResponseType.TOKEN);
        oauth.nonce("cie8cjcwiw");
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

        EventRepresentation loginEvent = events.expectLogin().client(clientId).assertEvent();
        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);
        String code = new OAuthClient.AuthorizationEndpointResponse(oauth).getCode();
        OAuthClient.AccessTokenResponse res = oauth.doAccessTokenRequest(code, clientSecret);
        assertEquals(200, res.getStatusCode());
        events.expectCodeToToken(codeId, sessionId).client(clientId).assertEvent();

        oauth.doLogout(res.getRefreshToken(), clientSecret);
        events.expectLogout(sessionId).client(clientId).clearDetails().assertEvent();

        oauth.responseType(OIDCResponseType.CODE + " " + OIDCResponseType.ID_TOKEN);
        oauth.nonce("vbwe566fsfffds");
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

        loginEvent = events.expectLogin().client(clientId).assertEvent();
        sessionId = loginEvent.getSessionId();
        codeId = loginEvent.getDetails().get(Details.CODE_ID);
        code = new OAuthClient.AuthorizationEndpointResponse(oauth).getCode();
        res = oauth.doAccessTokenRequest(code, clientSecret);
        assertEquals(200, res.getStatusCode());
        events.expectCodeToToken(codeId, sessionId).client(clientId).assertEvent();

        oauth.doLogout(res.getRefreshToken(), clientSecret);
        events.expectLogout(sessionId).client(clientId).clearDetails().assertEvent();
    }

    @Test
    public void testSecureRequestObjectExecutor() throws ClientRegistrationException, ClientPolicyException, URISyntaxException, IOException {
        String policyName = POLICY_NAME;
        createPolicy(policyName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyName);

        String conditionName = ClientRolesCondition_NAME;
        createCondition(conditionName, ClientRolesConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setConditionClientRoles(provider, new ArrayList<>(Arrays.asList(SAMPLE_CLIENT_ROLE)));
        });
        registerCondition(conditionName, policyName);
        logger.info("... Registered Condition : " + conditionName);

        String executor = SecureRequestObjectExecutor_NAME;
        createExecutor(executor, SecureRequestObjectExecutorFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {});
        registerExecutor(executor, policyName);
        logger.info("... Registered Executor : " + executor);

        String clientId = generateSuffixedName(CLIENT_NAME);
        String clientSecret = "secret";
        String cid = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret(clientSecret);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestUris(Arrays.asList(TestApplicationResourceUrls.clientRequestUri()));
        });
        adminClient.realm(REALM_NAME).clients().get(cid).roles().create(RoleBuilder.create().name(SAMPLE_CLIENT_ROLE).build());

        oauth.clientId(clientId);
        AuthorizationEndpointRequestObject requestObject;

        // check whether whether request object exists
        oauth.request(null);
        oauth.requestUri(null);
        oauth.openLoginForm();
        assertEquals(OAuthErrorException.INVALID_REQUEST, oauth.getCurrentQuery().get(OAuth2Constants.ERROR));
        assertEquals("Invalid parameter", oauth.getCurrentQuery().get(OAuth2Constants.ERROR_DESCRIPTION));

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
        oauth.openLoginForm();
        assertEquals(OAuthErrorException.INVALID_REQUEST, oauth.getCurrentQuery().get(OAuth2Constants.ERROR));
        assertEquals("Missing parameter : scope", oauth.getCurrentQuery().get(OAuth2Constants.ERROR_DESCRIPTION));

        // check whether "exp" claim exists
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.exp(null);
        registerRequestObject(requestObject, clientId, Algorithm.ES256, false);
        oauth.openLoginForm();
        assertEquals(SecureRequestObjectExecutor.INVALID_REQUEST_OBJECT, oauth.getCurrentQuery().get(OAuth2Constants.ERROR));
        assertEquals("Missing parameter : exp", oauth.getCurrentQuery().get(OAuth2Constants.ERROR_DESCRIPTION));

        // check whether request object not expired
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.exp(Long.valueOf(0));
        registerRequestObject(requestObject, clientId, Algorithm.ES256, true);
        oauth.openLoginForm();
        assertEquals(SecureRequestObjectExecutor.INVALID_REQUEST_OBJECT, oauth.getCurrentQuery().get(OAuth2Constants.ERROR));
        assertEquals("Request Expired", oauth.getCurrentQuery().get(OAuth2Constants.ERROR_DESCRIPTION));

        // check whether "aud" claim exists
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.audience((String)null);
        registerRequestObject(requestObject, clientId, Algorithm.ES256, false);
        oauth.openLoginForm();
        assertEquals(SecureRequestObjectExecutor.INVALID_REQUEST_OBJECT, oauth.getCurrentQuery().get(OAuth2Constants.ERROR));
        assertEquals("Missing parameter : aud", oauth.getCurrentQuery().get(OAuth2Constants.ERROR_DESCRIPTION));

        // check whether "aud" claim points to this keycloak as authz server
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.audience(suiteContext.getAuthServerInfo().getContextRoot().toString());
        registerRequestObject(requestObject, clientId, Algorithm.ES256, true);
        oauth.openLoginForm();
        assertEquals(SecureRequestObjectExecutor.INVALID_REQUEST_OBJECT, oauth.getCurrentQuery().get(OAuth2Constants.ERROR));
        assertEquals("Invalid parameter : aud", oauth.getCurrentQuery().get(OAuth2Constants.ERROR_DESCRIPTION));

        // confirm whether all parameters in query string are included in the request object, and have the same values
        // argument "request" are parameters overridden by parameters in request object
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.setState("notmatchstate");
        registerRequestObject(requestObject, clientId, Algorithm.ES256, false);
        oauth.openLoginForm();
        assertEquals(OAuthErrorException.INVALID_REQUEST, oauth.getCurrentQuery().get(OAuth2Constants.ERROR));
        assertEquals("Invalid parameter", oauth.getCurrentQuery().get(OAuth2Constants.ERROR_DESCRIPTION));

        // valid request object
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        registerRequestObject(requestObject, clientId, Algorithm.ES256, true);

        successfulLoginAndLogout(clientId, clientSecret);
    }

    @Test
    public void testSecureSessionEnforceExecutor() throws ClientRegistrationException, ClientPolicyException {
        String policyName = POLICY_NAME;
        createPolicy(policyName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyName);

        String roleAlphaName = "sample-client-role-alpha";
        String roleBetaName = "sample-client-role-beta";
        String conditionName = ClientRolesCondition_NAME;
        createCondition(conditionName, ClientRolesConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setConditionClientRoles(provider, new ArrayList<>(Arrays.asList(roleBetaName)));
        });
        registerCondition(conditionName, policyName);
        logger.info("... Registered Condition : " + conditionName);

        String executorName = SecureSessionEnforceExecutor_NAME;
        createExecutor(executorName, SecureSessionEnforceExecutorFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {});
        registerExecutor(executorName, policyName);
        logger.info("... Registered Executor : " + executorName);

        String clientAlphaId = generateSuffixedName("Alpha-App");
        String clientAlphaSecret = "secretAlpha";
        String cAlphaId = createClientByAdmin(clientAlphaId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret(clientAlphaSecret);
        });
        adminClient.realm(REALM_NAME).clients().get(cAlphaId).roles().create(RoleBuilder.create().name(roleAlphaName).build());

        String clientBetaId = generateSuffixedName("Beta-App");
        String clientBetaSecret = "secretBeta";
        String cBetaId = createClientByAdmin(clientBetaId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret(clientBetaSecret);
        });
        adminClient.realm(REALM_NAME).clients().get(cBetaId).roles().create(RoleBuilder.create().name(roleBetaName).build());

        successfulLoginAndLogout(clientAlphaId, clientAlphaSecret);

        oauth.openid(false);
        successfulLoginAndLogout(clientAlphaId, clientAlphaSecret);

        oauth.openid(true);
        failLoginWithoutSecureSessionParameter(clientBetaId, ERR_MSG_MISSING_NONCE);

        oauth.nonce("yesitisnonce");
        successfulLoginAndLogout(clientBetaId, clientBetaSecret);

        oauth.openid(false);
        oauth.stateParamHardcoded(null);
        failLoginWithoutSecureSessionParameter(clientBetaId, ERR_MSG_MISSING_STATE);

        oauth.stateParamRandom();
        successfulLoginAndLogout(clientBetaId, clientBetaSecret);
    }

    @Test
    public void testSecureSigningAlgorithmEnforceExecutor() throws ClientRegistrationException, ClientPolicyException {
        String policyName = POLICY_NAME;
        createPolicy(policyName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyName);

        String conditionName = ClientUpdateContextCondition_NAME;
        createCondition(conditionName, ClientUpdateContextConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setConditionRegistrationMethods(provider, new ArrayList<>(Arrays.asList(
                    ClientUpdateContextConditionFactory.BY_AUTHENTICATED_USER,
                    ClientUpdateContextConditionFactory.BY_INITIAL_ACCESS_TOKEN,
                    ClientUpdateContextConditionFactory.BY_REGISTRATION_ACCESS_TOKEN)));
        });
        registerCondition(conditionName, policyName);
        logger.info("... Registered Condition : " + conditionName);

        String execuorName = SecureSigningAlgorithmEnforceExecutor_NAME;
        createExecutor(execuorName, SecureSigningAlgorithmEnforceExecutorFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {});
        registerExecutor(execuorName, policyName);
        logger.info("... Registered Executor : " + execuorName);

        // create by Admin REST API - fail
        try {
            createClientByAdmin(generateSuffixedName("App-by-Admin"), (ClientRepresentation clientRep) -> {
                    clientRep.setSecret("secret");
                    clientRep.setAttributes(new HashMap<>());
                    clientRep.getAttributes().put(OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG, Algorithm.none.name());
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(Errors.INVALID_REGISTRATION, e.getMessage());
        }

        // create by Admin REST API - success
        String cAppAdminId = createClientByAdmin(generateSuffixedName("App-by-Admin"), (ClientRepresentation clientRep) -> {
                clientRep.setAttributes(new HashMap<>());
                clientRep.getAttributes().put(OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG, org.keycloak.crypto.Algorithm.PS256);
                clientRep.getAttributes().put(OIDCConfigAttributes.REQUEST_OBJECT_SIGNATURE_ALG, org.keycloak.crypto.Algorithm.ES256);
                clientRep.getAttributes().put(OIDCConfigAttributes.ID_TOKEN_SIGNED_RESPONSE_ALG, org.keycloak.crypto.Algorithm.ES256);
                clientRep.getAttributes().put(OIDCConfigAttributes.TOKEN_ENDPOINT_AUTH_SIGNING_ALG, org.keycloak.crypto.Algorithm.ES256);
                clientRep.getAttributes().put(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG, org.keycloak.crypto.Algorithm.ES256);
            });

        // update by Admin REST API - fail
        try {
            updateClientByAdmin(cAppAdminId, (ClientRepresentation clientRep) -> {
                clientRep.setAttributes(new HashMap<>());
                clientRep.getAttributes().put(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG, Algorithm.RS512.name());
            });
        } catch (BadRequestException bre) {
            assertEquals("HTTP 400 Bad Request", bre.getMessage());
        }
        ClientRepresentation cRep = getClientByAdmin(cAppAdminId);
        assertEquals(Algorithm.ES256.name(), cRep.getAttributes().get(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG));

        // update by Admin REST API - success
        updateClientByAdmin(cAppAdminId, (ClientRepresentation clientRep) -> {
                clientRep.setAttributes(new HashMap<>());
                clientRep.getAttributes().put(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG, Algorithm.PS384.name());
        });
        cRep = getClientByAdmin(cAppAdminId);
        assertEquals(Algorithm.PS384.name(), cRep.getAttributes().get(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG));

        // create dynamically - fail
        try {
            createClientByAdmin(generateSuffixedName("App-in-Dynamic"), (ClientRepresentation clientRep) -> {
                    clientRep.setSecret("secret");
                    clientRep.setAttributes(new HashMap<>());
                    clientRep.getAttributes().put(OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG, Algorithm.RS384.name());
                });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(Errors.INVALID_REGISTRATION, e.getMessage());
        }

        // create dynamically - success
        String cAppDynamicClientId = createClientDynamically(generateSuffixedName("App-in-Dynamic"), (OIDCClientRepresentation clientRep) -> {
                clientRep.setUserinfoSignedResponseAlg(org.keycloak.crypto.Algorithm.ES256);
                clientRep.setRequestObjectSigningAlg(org.keycloak.crypto.Algorithm.ES256);
                clientRep.setIdTokenSignedResponseAlg(org.keycloak.crypto.Algorithm.PS256);
                clientRep.setTokenEndpointAuthSigningAlg(org.keycloak.crypto.Algorithm.PS256);
            });
        events.expect(EventType.CLIENT_REGISTER).client(cAppDynamicClientId).user(Matchers.isEmptyOrNullString()).assertEvent();
        getClientDynamically(cAppDynamicClientId);

        // update dynamically - fail
        try {
            updateClientDynamically(cAppDynamicClientId, (OIDCClientRepresentation clientRep) -> {
                     clientRep.setIdTokenSignedResponseAlg(Algorithm.RS256.name());
                 });
            fail();
        } catch (ClientRegistrationException e) {
            assertEquals(ERR_MSG_CLIENT_REG_FAIL, e.getMessage());
        }
        assertEquals(org.keycloak.crypto.Algorithm.PS256, getClientDynamically(cAppDynamicClientId).getIdTokenSignedResponseAlg());

        // update dynamically - success
        updateClientDynamically(cAppDynamicClientId, (OIDCClientRepresentation clientRep) -> {
                clientRep.setIdTokenSignedResponseAlg(org.keycloak.crypto.Algorithm.ES384);
            });
        assertEquals(org.keycloak.crypto.Algorithm.ES384, getClientDynamically(cAppDynamicClientId).getIdTokenSignedResponseAlg());
    }

    @Test
    public void testSecureRedirectUriEnforceExecutor() throws ClientRegistrationException, ClientPolicyException {
        String policyName = POLICY_NAME;
        createPolicy(policyName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyName);

        String conditionName = ClientUpdateContextCondition_NAME;
        createCondition(conditionName, ClientUpdateContextConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setConditionRegistrationMethods(provider, new ArrayList<>(Arrays.asList(
                    ClientUpdateContextConditionFactory.BY_AUTHENTICATED_USER,
                    ClientUpdateContextConditionFactory.BY_INITIAL_ACCESS_TOKEN,
                    ClientUpdateContextConditionFactory.BY_REGISTRATION_ACCESS_TOKEN)));
        });
        registerCondition(conditionName, policyName);
        logger.info("... Registered Condition : " + conditionName);

        String executorName = SecureRedirectUriEnforceExecutor_NAME;
        createExecutor(executorName, SecureRedirectUriEnforceExecutorFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {});
        registerExecutor(executorName, policyName);
        logger.info("... Registered Executor : " + executorName);

        try {
            createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {
                clientRep.setRedirectUris(Collections.singletonList("http://newredirect"));
            });
            fail();
        } catch (ClientRegistrationException e) {
            assertEquals(ERR_MSG_CLIENT_REG_FAIL, e.getMessage());
        }
        updateCondition(conditionName, (ComponentRepresentation provider) -> {
            setConditionRegistrationMethods(provider, new ArrayList<>(Arrays.asList(
                    ClientUpdateContextConditionFactory.BY_AUTHENTICATED_USER,
                    ClientUpdateContextConditionFactory.BY_REGISTRATION_ACCESS_TOKEN)));
        });
        try {
            createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {});
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testSecureSigningAlgorithmForSignedJwtEnforceExecutor() throws Exception {
        String roleAlphaName = "sample-client-role-alpha";
        String roleZetaName = "sample-client-role-zeta";
        String roleCommonName = "sample-client-role-common";

        // policy including client role condition
        String policyName = POLICY_NAME;
        createPolicy(policyName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyName);

        String conditionName = ClientRolesCondition_NAME;
        createCondition(conditionName, ClientRolesConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setConditionClientRoles(provider, new ArrayList<>(Arrays.asList(roleAlphaName, roleZetaName)));
        });
        registerCondition(conditionName, policyName);
        logger.info("... Registered Condition : " + conditionName);

        String executorName = SecureSigningAlgorithmForSignedJwtEnforceExecutor_NAME;
        createExecutor(executorName, SecureSigningAlgorithmForSignedJwtEnforceExecutorFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {});
        registerExecutor(executorName, policyName);
        logger.info("... Registered Executor : " + executorName);

        // create a client with client role
        String clientId = generateSuffixedName(CLIENT_NAME);
        createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
                clientRep.setDefaultRoles(Arrays.asList(roleAlphaName, roleCommonName).toArray(new String[2]));
                clientRep.setSecret("secret");
                clientRep.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID);
                clientRep.setAttributes(new HashMap<>());
                clientRep.getAttributes().put(OIDCConfigAttributes.TOKEN_ENDPOINT_AUTH_SIGNING_ALG, org.keycloak.crypto.Algorithm.ES256);
            });

        ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm(REALM_NAME), clientId);
        ClientRepresentation clientRep = clientResource.toRepresentation();

        KeyPair keyPair = setupJwks(org.keycloak.crypto.Algorithm.ES256, clientRep, clientResource);
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        String signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, org.keycloak.crypto.Algorithm.ES256);

        oauth.clientId(clientId);
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
        EventRepresentation loginEvent = events.expectLogin()
                                                 .client(clientId)
                                                 .assertEvent();
        String sessionId = loginEvent.getSessionId();
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        // obtain access token
        OAuthClient.AccessTokenResponse response  = doAccessTokenRequestWithSignedJWT(code, signedJwt);

        assertEquals(200, response.getStatusCode());
        oauth.verifyToken(response.getAccessToken());
        RefreshToken refreshToken = oauth.parseRefreshToken(response.getRefreshToken());
        assertEquals(sessionId, refreshToken.getSessionState());
        assertEquals(sessionId, refreshToken.getSessionState());
        events.expectCodeToToken(loginEvent.getDetails().get(Details.CODE_ID), loginEvent.getSessionId())
                .client(clientId)
                .detail(Details.CLIENT_AUTH_METHOD, JWTClientAuthenticator.PROVIDER_ID)
                .assertEvent();

        // refresh token
        signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, org.keycloak.crypto.Algorithm.ES256);
        OAuthClient.AccessTokenResponse refreshedResponse = doRefreshTokenRequestWithSignedJWT(response.getRefreshToken(), signedJwt);
        assertEquals(200, refreshedResponse.getStatusCode());

        // introspect token
        signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, org.keycloak.crypto.Algorithm.ES256);
        HttpResponse tokenIntrospectionResponse = doTokenIntrospectionWithSignedJWT("access_token", refreshedResponse.getAccessToken(), signedJwt);
        assertEquals(200, tokenIntrospectionResponse.getStatusLine().getStatusCode());

        // revoke token
        signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, org.keycloak.crypto.Algorithm.ES256);
        HttpResponse revokeTokenResponse = doTokenRevokeWithSignedJWT("refresh_toke", refreshedResponse.getRefreshToken(), signedJwt);
        assertEquals(200, revokeTokenResponse.getStatusLine().getStatusCode());

        signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, org.keycloak.crypto.Algorithm.ES256);
        OAuthClient.AccessTokenResponse tokenRes = doRefreshTokenRequestWithSignedJWT(refreshedResponse.getRefreshToken(), signedJwt);
        assertEquals(400, tokenRes.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_GRANT, tokenRes.getError());

        // logout
        signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, org.keycloak.crypto.Algorithm.ES256);
        HttpResponse logoutResponse = doLogoutWithSignedJWT(refreshedResponse.getRefreshToken(), signedJwt);
        assertEquals(204, logoutResponse.getStatusLine().getStatusCode());
    }

    @Test
    public void testHolderOfKeyEnforceExecutor() throws Exception {
        String policyName = POLICY_NAME;
        createPolicy(policyName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyName);

        String conditionName = AnyClientCondition_NAME;
        createCondition(conditionName, AnyClientConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {});
        registerCondition(conditionName, policyName);
        logger.info("... Registered Condition : " + conditionName);

        String executorName = HolderOfKeyEnforceExecutor_NAME;
        createExecutor(executorName, HolderOfKeyEnforceExecutorFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setExecutorAugmentActivate(provider);
        });
        registerExecutor(executorName, policyName);
        logger.info("... Registered Executor : " + executorName);

        ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm(REALM_NAME), TEST_CLIENT);
        ClientRepresentation clientRep = clientResource.toRepresentation();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUseMtlsHoKToken(true);
        clientResource.update(clientRep);
        try {
            checkMtlsFlow();
        } finally {
            clientResource = ApiUtil.findClientByClientId(adminClient.realm(REALM_NAME), TEST_CLIENT);
            clientRep = clientResource.toRepresentation();
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUseMtlsHoKToken(false);
            clientResource.update(clientRep);
        }
    }

    private void checkMtlsFlow() throws IOException {
        // Check login.
        OAuthClient.AuthorizationEndpointResponse loginResponse = oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
        Assert.assertNull(loginResponse.getError());

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        // Check token obtaining.
        OAuthClient.AccessTokenResponse accessTokenResponse;
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore()) {
            accessTokenResponse = oauth.doAccessTokenRequest(code, TEST_CLIENT_SECRET, client);
        }  catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        assertEquals(200, accessTokenResponse.getStatusCode());

        // Check token refresh.
        OAuthClient.AccessTokenResponse accessTokenResponseRefreshed;
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore()) {
            accessTokenResponseRefreshed = oauth.doRefreshTokenRequest(accessTokenResponse.getRefreshToken(), TEST_CLIENT_SECRET, client);
        }  catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        assertEquals(200, accessTokenResponseRefreshed.getStatusCode());

        // Check token introspection.
        String tokenResponse;
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore()) {
            tokenResponse = oauth.introspectTokenWithClientCredential(TEST_CLIENT, TEST_CLIENT_SECRET, "access_token", accessTokenResponse.getAccessToken(), client);
        }  catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        Assert.assertNotNull(tokenResponse);
        TokenMetadataRepresentation tokenMetadataRepresentation = JsonSerialization.readValue(tokenResponse, TokenMetadataRepresentation.class);
        Assert.assertTrue(tokenMetadataRepresentation.isActive());

        // Check token revoke.
        CloseableHttpResponse tokenRevokeResponse;
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore()) {
            tokenRevokeResponse = oauth.doTokenRevoke(accessTokenResponse.getRefreshToken(), "refresh_token", TEST_CLIENT_SECRET, client);
        }  catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        assertEquals(200, tokenRevokeResponse.getStatusLine().getStatusCode());

        // Check logout.
        CloseableHttpResponse logoutResponse;
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore()) {
            logoutResponse = oauth.doLogout(accessTokenResponse.getRefreshToken(), TEST_CLIENT_SECRET, client);
        }  catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        assertEquals(204, logoutResponse.getStatusLine().getStatusCode());

        // Check login.
        loginResponse = oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
        Assert.assertNull(loginResponse.getError());

        code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        // Check token obtaining without certificate
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithoutKeyStoreAndTrustStore()) {
            accessTokenResponse = oauth.doAccessTokenRequest(code, TEST_CLIENT_SECRET, client);
        }  catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        assertEquals(400, accessTokenResponse.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_GRANT, accessTokenResponse.getError());

        // Check frontchannel logout and login.
        oauth.openLogout();
        loginResponse = oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
        Assert.assertNull(loginResponse.getError());

        code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        // Check token obtaining.
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore()) {
            accessTokenResponse = oauth.doAccessTokenRequest(code, TEST_CLIENT_SECRET, client);
        }  catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        assertEquals(200, accessTokenResponse.getStatusCode());

        // Check token refresh with other certificate
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithOtherKeyStoreAndTrustStore()) {
            accessTokenResponseRefreshed = oauth.doRefreshTokenRequest(accessTokenResponse.getRefreshToken(), TEST_CLIENT_SECRET, client);
        }  catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        assertEquals(401, accessTokenResponseRefreshed.getStatusCode());
        assertEquals(Errors.NOT_ALLOWED, accessTokenResponseRefreshed.getError());

        // Check token revoke with other certificate
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithOtherKeyStoreAndTrustStore()) {
            tokenRevokeResponse = oauth.doTokenRevoke(accessTokenResponse.getRefreshToken(), "refresh_token", TEST_CLIENT_SECRET, client);
        }  catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        assertEquals(401, tokenRevokeResponse.getStatusLine().getStatusCode());

        // Check logout without certificate
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithoutKeyStoreAndTrustStore()) {
            logoutResponse = oauth.doLogout(accessTokenResponse.getRefreshToken(), TEST_CLIENT_SECRET, client);
        }  catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        assertEquals(401, logoutResponse.getStatusLine().getStatusCode());

        // Check logout.
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore()) {
            logoutResponse = oauth.doLogout(accessTokenResponse.getRefreshToken(), TEST_CLIENT_SECRET, client);
        }  catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private void setupPolicyClientIdAndSecretNotAcceptableAuthType(String policyName) {
        createPolicy(policyName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyName);

        createCondition(ClientUpdateContextCondition_NAME, ClientUpdateContextConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setConditionRegistrationMethods(provider, new ArrayList<>(Arrays.asList(ClientUpdateContextConditionFactory.BY_AUTHENTICATED_USER)));
        });
        registerCondition(ClientUpdateContextCondition_NAME, policyName);
        logger.info("... Registered Condition : " + ClientUpdateContextCondition_NAME);

        createExecutor(SecureClientAuthEnforceExecutor_NAME, SecureClientAuthEnforceExecutorFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setExecutorAcceptedClientAuthMethods(provider, new ArrayList<>(Arrays.asList(
                    JWTClientAuthenticator.PROVIDER_ID, JWTClientSecretAuthenticator.PROVIDER_ID, X509ClientAuthenticator.PROVIDER_ID)));
        });
        registerExecutor(SecureClientAuthEnforceExecutor_NAME, policyName);
        logger.info("... Registered Executor : " + SecureClientAuthEnforceExecutor_NAME);

    }

    private void setupPolicyAuthzCodeFlowUnderMultiPhasePolicy(String policyName) {
        logger.info("Setup Policy");
        createPolicy(policyName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyName);

        createCondition(ClientUpdateContextCondition_NAME, ClientUpdateContextConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setConditionRegistrationMethods(provider, new ArrayList<>(Arrays.asList(ClientUpdateContextConditionFactory.BY_INITIAL_ACCESS_TOKEN)));
        });
        registerCondition(ClientUpdateContextCondition_NAME, policyName);
        logger.info("... Registered Condition : " + ClientUpdateContextCondition_NAME);

        createCondition(ClientRolesCondition_NAME, ClientRolesConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setConditionClientRoles(provider, new ArrayList<>(Arrays.asList(SAMPLE_CLIENT_ROLE)));
        });
        registerCondition(ClientRolesCondition_NAME, policyName);
        logger.info("... Registered Condition : " + ClientRolesCondition_NAME);

        createExecutor(SecureClientAuthEnforceExecutor_NAME, SecureClientAuthEnforceExecutorFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setExecutorAcceptedClientAuthMethods(provider, new ArrayList<>(Arrays.asList(ClientIdAndSecretAuthenticator.PROVIDER_ID, JWTClientAuthenticator.PROVIDER_ID)));
            setExecutorAugmentedClientAuthMethod(provider, ClientIdAndSecretAuthenticator.PROVIDER_ID);
            setExecutorAugmentActivate(provider);
        });
        registerExecutor(SecureClientAuthEnforceExecutor_NAME, policyName);
        logger.info("... Registered Executor : " + SecureClientAuthEnforceExecutor_NAME);

        createExecutor(PKCEEnforceExecutor_NAME, PKCEEnforceExecutorFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setExecutorAugmentActivate(provider);
        });
        registerExecutor(PKCEEnforceExecutor_NAME, policyName);
        logger.info("... Registered Executor : " + PKCEEnforceExecutor_NAME);
    }

    private void successfulLoginAndLogout(String clientId, String clientSecret) {
        oauth.clientId(clientId);
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

        EventRepresentation loginEvent = events.expectLogin().client(clientId).assertEvent();
        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse res = oauth.doAccessTokenRequest(code, clientSecret);
        assertEquals(200, res.getStatusCode());
        events.expectCodeToToken(codeId, sessionId).client(clientId).assertEvent();

        oauth.doLogout(res.getRefreshToken(), clientSecret);
        events.expectLogout(sessionId).client(clientId).clearDetails().assertEvent();
    }

    private void successfulLoginAndLogoutWithPKCE(String clientId, String clientSecret, String userName, String userPassword) throws Exception {
        oauth.clientId(clientId);
        String codeVerifier = "1a345A7890123456r8901c3456789012b45K7890l23"; // 43
        String codeChallenge = generateS256CodeChallenge(codeVerifier);
        oauth.codeChallenge(codeChallenge);
        oauth.codeChallengeMethod(OAuth2Constants.PKCE_METHOD_S256);
        oauth.nonce("bjapewiziIE083d");

        oauth.doLogin(userName, userPassword);

        EventRepresentation loginEvent = events.expectLogin().client(clientId).assertEvent();
        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        oauth.codeVerifier(codeVerifier);
        OAuthClient.AccessTokenResponse res = oauth.doAccessTokenRequest(code, clientSecret);
        assertEquals(200, res.getStatusCode());
        events.expectCodeToToken(codeId, sessionId).client(clientId).assertEvent();

        AccessToken token = oauth.verifyToken(res.getAccessToken());
        String userId = findUserByUsername(adminClient.realm(REALM_NAME), userName).getId();
        assertEquals(userId, token.getSubject());
        Assert.assertNotEquals(userName, token.getSubject());
        assertEquals(sessionId, token.getSessionState());
        assertEquals(clientId, token.getIssuedFor());

        String refreshTokenString = res.getRefreshToken();
        RefreshToken refreshToken = oauth.parseRefreshToken(refreshTokenString);
        assertEquals(sessionId, refreshToken.getSessionState());
        assertEquals(clientId, refreshToken.getIssuedFor());

        OAuthClient.AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(refreshTokenString, clientSecret);
        assertEquals(200, refreshResponse.getStatusCode());
        events.expectRefresh(refreshToken.getId(), sessionId).client(clientId).assertEvent();

        AccessToken refreshedToken = oauth.verifyToken(refreshResponse.getAccessToken());
        RefreshToken refreshedRefreshToken = oauth.parseRefreshToken(refreshResponse.getRefreshToken());
        assertEquals(sessionId, refreshedToken.getSessionState());
        assertEquals(sessionId, refreshedRefreshToken.getSessionState());
        assertEquals(findUserByUsername(adminClient.realm(REALM_NAME), userName).getId(), refreshedToken.getSubject());

        doIntrospectAccessToken(refreshResponse, userName, clientId, clientSecret);

        doTokenRevoke(refreshResponse.getRefreshToken(), clientId, clientSecret, userId, false);
    }

    private void failLoginByNotFollowingPKCE(String clientId) {
        oauth.clientId(clientId);
        oauth.openLoginForm();
        assertEquals(OAuthErrorException.INVALID_REQUEST, oauth.getCurrentQuery().get(OAuth2Constants.ERROR));
        assertEquals("Missing parameter: code_challenge_method", oauth.getCurrentQuery().get(OAuth2Constants.ERROR_DESCRIPTION));
    }

    private void failTokenRequestByNotFollowingPKCE(String clientId, String clientSecret) {
        oauth.clientId(clientId);
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

        EventRepresentation loginEvent = events.expectLogin().client(clientId).assertEvent();
        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse res = oauth.doAccessTokenRequest(code, clientSecret);

        assertEquals(OAuthErrorException.INVALID_GRANT, res.getError());
        assertEquals("PKCE code verifier not specified", res.getErrorDescription());
        events.expect(EventType.CODE_TO_TOKEN_ERROR).client(clientId).session(sessionId).clearDetails().error(Errors.CODE_VERIFIER_MISSING).assertEvent();

        oauth.openLogout();
        events.expectLogout(sessionId).clearDetails().assertEvent();
    }

    private void failLoginWithoutSecureSessionParameter(String clientId, String errorDescription) {
        oauth.clientId(clientId);
        oauth.openLoginForm();
        assertEquals(OAuthErrorException.INVALID_REQUEST, oauth.getCurrentQuery().get(OAuth2Constants.ERROR));
        assertEquals(errorDescription, oauth.getCurrentQuery().get(OAuth2Constants.ERROR_DESCRIPTION));
    }

    private void failLoginWithoutNonce(String clientId) {
        oauth.clientId(clientId);
        oauth.openLoginForm();
        assertEquals(OAuthErrorException.INVALID_REQUEST, oauth.getCurrentQuery().get(OAuth2Constants.ERROR));
        assertEquals(ERR_MSG_MISSING_NONCE, oauth.getCurrentQuery().get(OAuth2Constants.ERROR_DESCRIPTION));
    }

}
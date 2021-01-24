/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.hamcrest.Matchers;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RolesResource;
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
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.representations.oidc.TokenMetadataRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientAccessTypeConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientRolesConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientScopesConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdateContextConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdateSourceGroupsConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdateSourceHostsConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdateSourceRolesConditionFactory;
import org.keycloak.services.clientpolicy.executor.HolderOfKeyEnforceExecutorFactory;
import org.keycloak.services.clientpolicy.executor.PKCEEnforceExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureClientAuthEnforceExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureRedirectUriEnforceExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureRequestObjectExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureResponseTypeExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureSessionEnforceExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureSigningAlgorithmEnforceExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureSigningAlgorithmForSignedJwtEnforceExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureRequestObjectExecutor;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.client.resources.TestApplicationResourceUrls;
import org.keycloak.testsuite.rest.resource.TestingOIDCEndpointsApplicationResource.AuthorizationEndpointRequestObject;
import org.keycloak.testsuite.services.clientpolicy.condition.TestRaiseExeptionConditionFactory;
import org.keycloak.testsuite.services.clientpolicy.executor.TestRaiseExeptionExecutorFactory;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.util.MutualTLSUtils;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RoleBuilder;
import org.keycloak.testsuite.util.ServerURLs;
import org.keycloak.util.JsonSerialization;

@EnableFeature(value = Profile.Feature.CLIENT_POLICIES, skipRestart = true)
public class ClientPoliciesTest extends AbstractClientPoliciesTest {

    private static final Logger logger = Logger.getLogger(ClientPoliciesTest.class);

    private static final String CLIENT_NAME = "Zahlungs-App";
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
    public void testAdminClientRegisterUnacceptableAuthType() throws Exception {
        setupPolicyClientIdAndSecretNotAcceptableAuthType(POLICY_NAME);
        try {
            createClientByAdmin(generateSuffixedName(CLIENT_NAME), (ClientRepresentation clientRep) -> {
                clientRep.setClientAuthenticatorType(ClientIdAndSecretAuthenticator.PROVIDER_ID);
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getMessage());
        }
    }

    @Test
    public void testAdminClientRegisterAcceptableAuthType() throws Exception {
        setupPolicyClientIdAndSecretNotAcceptableAuthType(POLICY_NAME);
        String cId = createClientByAdmin(generateSuffixedName(CLIENT_NAME), (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(JWTClientSecretAuthenticator.PROVIDER_ID);
        });
        assertEquals(JWTClientSecretAuthenticator.PROVIDER_ID, getClientByAdmin(cId).getClientAuthenticatorType());
    }

    @Test
    public void testAdminClientRegisterDefaultAuthType() throws Exception {
        setupPolicyClientIdAndSecretNotAcceptableAuthType(POLICY_NAME);
        try {
            createClientByAdmin(generateSuffixedName(CLIENT_NAME), (ClientRepresentation clientRep) -> {});
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getMessage());
        }
    }

    @Test
    public void testAdminClientUpdateUnacceptableAuthType() throws Exception {
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
        } catch (ClientPolicyException cpe) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, cpe.getError());
        }
        assertEquals(JWTClientSecretAuthenticator.PROVIDER_ID, getClientByAdmin(cId).getClientAuthenticatorType());
    }

    @Test
    public void testAdminClientUpdateAcceptableAuthType() throws Exception {
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
    public void testAdminClientUpdateDefaultAuthType() throws Exception {
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
    public void testAdminClientAugmentedAuthType() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                   (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Pershyy Profil", Boolean.FALSE, null)
                       .addExecutor(SecureClientAuthEnforceExecutorFactory.PROVIDER_ID, 
                           createSecureClientAuthEnforceExecutorConfig(Boolean.TRUE, 
                               Arrays.asList(JWTClientAuthenticator.PROVIDER_ID, JWTClientSecretAuthenticator.PROVIDER_ID, X509ClientAuthenticator.PROVIDER_ID),
                               X509ClientAuthenticator.PROVIDER_ID))
                       .toRepresentation()
                ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                   (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Persha Polityka", Boolean.FALSE, Boolean.TRUE, null, null)
                       .addCondition(ClientUpdateContextConditionFactory.PROVIDER_ID, 
                           createClientUpdateContextConditionConfig(Arrays.asList(ClientUpdateContextConditionFactory.BY_AUTHENTICATED_USER)))
                       .addProfile(PROFILE_NAME)
                       .toRepresentation()
               ).toString();
        updatePolicies(json);

        String cId = createClientByAdmin(generateSuffixedName(CLIENT_NAME), (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(ClientIdAndSecretAuthenticator.PROVIDER_ID);
        });

        assertEquals(X509ClientAuthenticator.PROVIDER_ID, getClientByAdmin(cId).getClientAuthenticatorType());

        // update profiles
        json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Pershyy Profil", Boolean.FALSE, null)
                    .addExecutor(SecureClientAuthEnforceExecutorFactory.PROVIDER_ID, 
                        createSecureClientAuthEnforceExecutorConfig(Boolean.TRUE, 
                            Arrays.asList(JWTClientAuthenticator.PROVIDER_ID, JWTClientSecretAuthenticator.PROVIDER_ID, X509ClientAuthenticator.PROVIDER_ID),
                            JWTClientAuthenticator.PROVIDER_ID))
                    .toRepresentation()
             ).toString();
         updateProfiles(json);

         updateClientByAdmin(cId, (ClientRepresentation clientRep) -> {
             clientRep.setClientAuthenticatorType(JWTClientSecretAuthenticator.PROVIDER_ID);
         });
         assertEquals(JWTClientAuthenticator.PROVIDER_ID, getClientByAdmin(cId).getClientAuthenticatorType());
    }

    @Test
    public void testDynamicClientRegisterAndUpdate() throws Exception {
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
    public void testCreateDeletePolicyRuntime() throws Exception {
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
    public void testCreateUpdateDeleteConditionRuntime() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                   (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Den Eichte profil", Boolean.FALSE, null)
                       .addExecutor(PKCEEnforceExecutorFactory.PROVIDER_ID, 
                           createPKCEEnforceExecutorConfig(Boolean.TRUE))
                       .toRepresentation()
                ).toString();
        updateProfiles(json);

        String clientId = generateSuffixedName(CLIENT_NAME);
        String clientSecret = "secret";
        String cid = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret(clientSecret);
        });
        adminClient.realm(REALM_NAME).clients().get(cid).roles().create(RoleBuilder.create().name(SAMPLE_CLIENT_ROLE).build());

        successfulLoginAndLogout(clientId, clientSecret);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                   (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Dei Eischt Politik", Boolean.FALSE, Boolean.TRUE, null, null)
                       .addCondition(ClientRolesConditionFactory.PROVIDER_ID, 
                           createClientRolesConditionConfig(Arrays.asList(SAMPLE_CLIENT_ROLE)))
                       .addProfile(PROFILE_NAME)
                       .toRepresentation()
               ).toString();
        updatePolicies(json);

        failLoginByNotFollowingPKCE(clientId);

        // update policies
        updatePolicy((new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Dei Aktualiseiert Eischt Politik", Boolean.FALSE, Boolean.TRUE, null, null)
                .addCondition(ClientRolesConditionFactory.PROVIDER_ID, 
                        createClientRolesConditionConfig(Arrays.asList("anothor-client-role")))
                    .addProfile(PROFILE_NAME)
                    .toRepresentation());

        successfulLoginAndLogout(clientId, clientSecret);

        // update policies
        updatePolicy((new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Dei Aktualiseiert Eischt Politik", Boolean.FALSE, Boolean.TRUE, null, null)
                .addProfile(PROFILE_NAME)
                .toRepresentation());

        successfulLoginAndLogout(clientId, clientSecret);
    }

    @Test
    public void testCreateUpdateDeleteExecutorRuntime() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                   (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Purofairu Sono Ichi", Boolean.FALSE, null)
                       .addExecutor(PKCEEnforceExecutorFactory.PROVIDER_ID, 
                           createPKCEEnforceExecutorConfig(Boolean.FALSE))
                       .toRepresentation()
                ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                   (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Porishii Sono Ichi", Boolean.FALSE, Boolean.TRUE, null, null)
                       .addCondition(ClientRolesConditionFactory.PROVIDER_ID, 
                           createClientRolesConditionConfig(Arrays.asList(SAMPLE_CLIENT_ROLE)))
                       .addCondition(ClientUpdateContextConditionFactory.PROVIDER_ID, 
                               createClientUpdateContextConditionConfig(Arrays.asList(ClientUpdateContextConditionFactory.BY_AUTHENTICATED_USER)))
                       .toRepresentation()
               ).toString();
        updatePolicies(json);

        String clientId = generateSuffixedName(CLIENT_NAME);
        String clientSecret = "secret";
        String cid = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret(clientSecret);
        });
        adminClient.realm(REALM_NAME).clients().get(cid).roles().create(RoleBuilder.create().name(SAMPLE_CLIENT_ROLE).build());

        successfulLoginAndLogout(clientId, clientSecret);
 
        // update policies
        updatePolicy((new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Koushinsareta Porishii Sono Ichi", Boolean.FALSE, Boolean.TRUE, null, null)
                .addCondition(ClientRolesConditionFactory.PROVIDER_ID, 
                    createClientRolesConditionConfig(Arrays.asList(SAMPLE_CLIENT_ROLE)))
                .addCondition(ClientUpdateContextConditionFactory.PROVIDER_ID, 
                    createClientUpdateContextConditionConfig(Arrays.asList(ClientUpdateContextConditionFactory.BY_AUTHENTICATED_USER)))
                .addProfile(PROFILE_NAME)
                .toRepresentation());

        failLoginByNotFollowingPKCE(clientId);

        // update profiles
        updateProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Koushinsareta Purofairu Sono Ichi", Boolean.FALSE, null)
                .addExecutor(PKCEEnforceExecutorFactory.PROVIDER_ID, 
                    createPKCEEnforceExecutorConfig(Boolean.TRUE))
                .toRepresentation());

        updateClientByAdmin(cid, (ClientRepresentation clientRep) -> {
            clientRep.setServiceAccountsEnabled(Boolean.FALSE);
        });
        assertEquals(false, getClientByAdmin(cid).isServiceAccountsEnabled());
        assertEquals(OAuth2Constants.PKCE_METHOD_S256, OIDCAdvancedConfigWrapper.fromClientRepresentation(getClientByAdmin(cid)).getPkceCodeChallengeMethod());

        // update profiles
        updateProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Sarani Koushinsareta Purofairu Sono Ichi", Boolean.FALSE, null).toRepresentation());

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
    public void testMultiplePolicies() throws Exception {
        String roleAlphaName = "sample-client-role-alpha";
        String roleBetaName = "sample-client-role-beta";
        String roleZetaName = "sample-client-role-zeta";
        String roleCommonName = "sample-client-role-common";

        // register profiles
        String profileAlphaName = "MyProfile-alpha";
        String profileBetaName = "MyProfile-beta";
        String json = (new ClientProfilesBuilder()).addProfile(
                   (new ClientProfileBuilder()).createProfile(profileAlphaName, "Pierwszy Profil", Boolean.FALSE, null)
                       .addExecutor(SecureClientAuthEnforceExecutorFactory.PROVIDER_ID, 
                           createSecureClientAuthEnforceExecutorConfig(Boolean.TRUE, Arrays.asList(ClientIdAndSecretAuthenticator.PROVIDER_ID), ClientIdAndSecretAuthenticator.PROVIDER_ID))
                       .toRepresentation()).addProfile(
                   (new ClientProfileBuilder()).createProfile(profileBetaName, "Drugi Profil", Boolean.FALSE, null)
                       .addExecutor(PKCEEnforceExecutorFactory.PROVIDER_ID, 
                           createPKCEEnforceExecutorConfig(Boolean.TRUE))
                       .toRepresentation()
                ).toString();
        updateProfiles(json);

        // register policies
        String policyAlphaName = "MyPolicy-alpha";
        String policyBetaName = "MyPolicy-beta";
        json = (new ClientPoliciesBuilder()).addPolicy(
                   (new ClientPolicyBuilder()).createPolicy(policyAlphaName, "Pierwsza Zasada", Boolean.FALSE, Boolean.TRUE, null, null)
                       .addCondition(ClientRolesConditionFactory.PROVIDER_ID, 
                           createClientRolesConditionConfig(Arrays.asList(roleAlphaName, roleZetaName)))
                       .addCondition(ClientUpdateContextConditionFactory.PROVIDER_ID, 
                           createClientUpdateContextConditionConfig(Arrays.asList(ClientUpdateContextConditionFactory.BY_AUTHENTICATED_USER)))
                       .addProfile(profileAlphaName)
                       .toRepresentation()).addPolicy(
                   (new ClientPolicyBuilder()).createPolicy(policyBetaName, "Drugi Zasada", Boolean.FALSE, Boolean.TRUE, null, null)
                       .addCondition(ClientRolesConditionFactory.PROVIDER_ID, 
                           createClientRolesConditionConfig(Arrays.asList(roleBetaName, roleZetaName)))
                       .addProfile(profileBetaName)
                       .toRepresentation()
               ).toString();
        updatePolicies(json);

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
    public void testIntentionalExceptionOnCondition() throws Exception {
        // register policies
        String json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Fyrsta Stefnan", Boolean.FALSE, Boolean.TRUE, null, null)
                    .addCondition(TestRaiseExeptionConditionFactory.PROVIDER_ID, 
                        createTestRaiseExeptionConditionConfig())
                    .toRepresentation()
                ).toString();
        updatePolicies(json);

        try {
            createClientByAdmin(generateSuffixedName(CLIENT_NAME), (ClientRepresentation clientRep) -> {});
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.SERVER_ERROR, e.getMessage());
        }
    }

    @Test
    public void testAnyClientCondition() throws Exception {
        getProfiles();
        getPolicies();
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil", Boolean.FALSE, null)
                    .addExecutor(SecureSessionEnforceExecutorFactory.PROVIDER_ID, 
                        createSecureSessionEnforceExecutorConfig())
                    .toRepresentation()
                ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.FALSE, Boolean.TRUE, null, null)
                    .addCondition(AnyClientConditionFactory.PROVIDER_ID, 
                        createAnyClientConditionConfig())
                    .addProfile(PROFILE_NAME)
                    .toRepresentation()
                ).toString();
        updatePolicies(json);

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
    public void testConditionWithoutNoConfiguration() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Die Erste Politik", Boolean.FALSE, null)
                    .addExecutor(SecureClientAuthEnforceExecutorFactory.PROVIDER_ID, null)
                    .toRepresentation()
                ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                       (new ClientPolicyBuilder()).createPolicy("MyPolicy-ClientAccessTypeCondition", "Die Erste Politik", Boolean.FALSE, Boolean.TRUE, null, null)
                           .addCondition(ClientAccessTypeConditionFactory.PROVIDER_ID, null)
                           .addProfile(PROFILE_NAME)
                           .toRepresentation()
                   ).addPolicy(
                       (new ClientPolicyBuilder()).createPolicy("MyPolicy-ClientUpdateSourceGroupsCondition", "Die Zweite Politik", Boolean.FALSE, Boolean.TRUE, null, null)
                           .addCondition(ClientUpdateSourceGroupsConditionFactory.PROVIDER_ID, null)
                           .addProfile(PROFILE_NAME)
                           .toRepresentation()
                   ).addPolicy(
                       (new ClientPolicyBuilder()).createPolicy("MyPolicy-ClientUpdateSourceRolesCondition", "Die Dritte Politik", Boolean.FALSE, Boolean.TRUE, null, null)
                           .addCondition(ClientUpdateSourceRolesConditionFactory.PROVIDER_ID, null)
                           .addProfile(PROFILE_NAME)
                           .toRepresentation()
                   ).addPolicy(
                       (new ClientPolicyBuilder()).createPolicy("MyPolicy-ClientUpdateContextCondition", "Die Vierte Politik", Boolean.FALSE, Boolean.TRUE, null, null)
                           .addCondition(ClientUpdateContextConditionFactory.PROVIDER_ID, null)
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


    @AuthServerContainerExclude(AuthServer.REMOTE)
    @Test
    public void testClientUpdateSourceHostsCondition() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Prvni Profil", Boolean.FALSE, null)
                    .addExecutor(SecureClientAuthEnforceExecutorFactory.PROVIDER_ID, 
                        createSecureClientAuthEnforceExecutorConfig(
                            Boolean.FALSE, 
                            Arrays.asList(JWTClientAuthenticator.PROVIDER_ID, JWTClientSecretAuthenticator.PROVIDER_ID, X509ClientAuthenticator.PROVIDER_ID),
                            null)
                        )
                    .toRepresentation()
                ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                   (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Prvni Politika", Boolean.FALSE, Boolean.TRUE, null, null)
                       .addCondition(ClientUpdateSourceHostsConditionFactory.PROVIDER_ID, 
                            createClientUpdateSourceHostsConditionConfig(Arrays.asList("localhost", "127.0.0.1"), Arrays.asList(Boolean.TRUE, Boolean.TRUE)))
                       .addProfile(PROFILE_NAME)
                       .toRepresentation()
               ).toString();
        updatePolicies(json);

        String clientId = generateSuffixedName(CLIENT_NAME);
        String clientSecret = "secret";
        try {
            createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
                clientRep.setSecret(clientSecret);
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getMessage());
        }

        // update policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                   (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Aktualizovana Prvni Politika", Boolean.FALSE, Boolean.TRUE, null, null)
                       .addCondition(ClientUpdateSourceHostsConditionFactory.PROVIDER_ID, 
                           createClientUpdateSourceHostsConditionConfig(Arrays.asList("example.com"), Arrays.asList(Boolean.TRUE)))
                       .addProfile(PROFILE_NAME)
                       .toRepresentation()
                ).toString();
        updatePolicies(json);

        try {
            createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
                clientRep.setSecret(clientSecret);
            });
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testClientUpdateSourceGroupsCondition() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Den Forste Profil", Boolean.FALSE, null)
                    .addExecutor(SecureClientAuthEnforceExecutorFactory.PROVIDER_ID, 
                        createSecureClientAuthEnforceExecutorConfig(
                            Boolean.FALSE, 
                            Arrays.asList(JWTClientAuthenticator.PROVIDER_ID),
                            null)
                        )
                    .toRepresentation()
                ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                   (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Den Forste Politik", Boolean.FALSE, Boolean.TRUE, null, null)
                       .addCondition(ClientUpdateSourceGroupsConditionFactory.PROVIDER_ID, 
                            createClientUpdateSourceGroupsConditionConfig(Arrays.asList("topGroup")))
                       .addProfile(PROFILE_NAME)
                       .toRepresentation()
               ).toString();
        updatePolicies(json);

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
    public void testClientUpdateSourceRolesCondition() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Il Primo Profilo", Boolean.FALSE, null)
                    .addExecutor(SecureClientAuthEnforceExecutorFactory.PROVIDER_ID, 
                        createSecureClientAuthEnforceExecutorConfig(
                            Boolean.FALSE, 
                            Arrays.asList(JWTClientSecretAuthenticator.PROVIDER_ID),
                            null)
                        )
                    .toRepresentation()
                ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Prima Politica", Boolean.FALSE, Boolean.TRUE, null, null)
                    .addCondition(ClientUpdateSourceRolesConditionFactory.PROVIDER_ID, 
                        createClientUpdateSourceRolesConditionConfig(Arrays.asList(AdminRoles.CREATE_CLIENT)))
                    .addProfile(PROFILE_NAME)
                    .toRepresentation()
                ).toString();
        updatePolicies(json);

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
    public void testClientScopesCondition() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Het Eerste Profiel", Boolean.FALSE, null)
                    .addExecutor(PKCEEnforceExecutorFactory.PROVIDER_ID, 
                        createPKCEEnforceExecutorConfig(Boolean.TRUE))
                    .toRepresentation()
                ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Het Eerste Beleid", Boolean.FALSE, Boolean.TRUE, null, null)
                    .addCondition(ClientScopesConditionFactory.PROVIDER_ID, 
                        createClientScopesConditionConfig(ClientScopesConditionFactory.OPTIONAL, Arrays.asList("offline_access", "microprofile-jwt")))
                    .addProfile(PROFILE_NAME)
                    .toRepresentation()
                ).toString();
        updatePolicies(json);

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
    public void testClientAccessTypeCondition() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "El Primer Perfil", Boolean.FALSE, null)
                    .addExecutor(SecureSessionEnforceExecutorFactory.PROVIDER_ID, 
                        createSecureSessionEnforceExecutorConfig())
                    .toRepresentation()
                ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Primera Plitica", Boolean.FALSE, Boolean.TRUE, null, null)
                    .addCondition(ClientAccessTypeConditionFactory.PROVIDER_ID, 
                        createClientAccessTypeConditionConfig(Arrays.asList(ClientAccessTypeConditionFactory.TYPE_CONFIDENTIAL)))
                    .addProfile(PROFILE_NAME)
                    .toRepresentation()
                ).toString();
        updatePolicies(json);

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
    public void testSecureResponseTypeExecutor() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "O Primeiro Perfil", Boolean.FALSE, null)
                    .addExecutor(SecureResponseTypeExecutorFactory.PROVIDER_ID, 
                        createSecureResponseTypeExecutorConfig())
                    .toRepresentation()
                ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "A Primeira Politica", Boolean.FALSE, Boolean.TRUE, null, null)
                    .addCondition(ClientRolesConditionFactory.PROVIDER_ID, 
                        createClientRolesConditionConfig(Arrays.asList(SAMPLE_CLIENT_ROLE)))
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
    public void testSecureRequestObjectExecutor() throws Exception, URISyntaxException, IOException {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Prvy Profil", Boolean.FALSE, null)
                    .addExecutor(SecureRequestObjectExecutorFactory.PROVIDER_ID, 
                        createSecureRequestObjectExecutorConfig())
                    .toRepresentation()
                ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Prva Politika", Boolean.FALSE, Boolean.TRUE, null, null)
                    .addCondition(ClientRolesConditionFactory.PROVIDER_ID, 
                        createClientRolesConditionConfig(Arrays.asList(SAMPLE_CLIENT_ROLE)))
                    .addProfile(PROFILE_NAME)
                    .toRepresentation()
                ).toString();
        updatePolicies(json);

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
    public void testSecureSessionEnforceExecutor() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Den Forste Profilen", Boolean.FALSE, null)
                    .addExecutor(SecureSessionEnforceExecutorFactory.PROVIDER_ID, 
                        createSecureSessionEnforceExecutorConfig())
                    .toRepresentation()
                ).toString();
        updateProfiles(json);

        // register policies
        String roleAlphaName = "sample-client-role-alpha";
        String roleBetaName = "sample-client-role-beta";
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Den Forste Politikken", Boolean.FALSE, Boolean.TRUE, null, null)
                    .addCondition(ClientRolesConditionFactory.PROVIDER_ID, 
                        createClientRolesConditionConfig(Arrays.asList(roleBetaName)))
                    .addProfile(PROFILE_NAME)
                    .toRepresentation()
                ).toString();
        updatePolicies(json);

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
    public void testSecureSigningAlgorithmEnforceExecutor() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Den Forsta Profilen", Boolean.FALSE, null)
                    .addExecutor(SecureSigningAlgorithmEnforceExecutorFactory.PROVIDER_ID, 
                        createSecureSigningAlgorithmEnforceExecutorConfig())
                    .toRepresentation()
                ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Den Forsta Policyn", Boolean.FALSE, Boolean.TRUE, null, null)
                    .addCondition(ClientUpdateContextConditionFactory.PROVIDER_ID, 
                        createClientUpdateContextConditionConfig(Arrays.asList(
                                ClientUpdateContextConditionFactory.BY_AUTHENTICATED_USER,
                                ClientUpdateContextConditionFactory.BY_INITIAL_ACCESS_TOKEN,
                                ClientUpdateContextConditionFactory.BY_REGISTRATION_ACCESS_TOKEN)))
                    .addProfile(PROFILE_NAME)
                    .toRepresentation()
                ).toString();
        updatePolicies(json);


        // create by Admin REST API - fail
        try {
            createClientByAdmin(generateSuffixedName("App-by-Admin"), (ClientRepresentation clientRep) -> {
                    clientRep.setSecret("secret");
                    clientRep.setAttributes(new HashMap<>());
                    clientRep.getAttributes().put(OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG, Algorithm.none.name());
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_REQUEST, e.getMessage());
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
                clientRep.getAttributes().put(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG, org.keycloak.crypto.Algorithm.RS512);
            });
        } catch (ClientPolicyException cpe) {
            assertEquals(Errors.INVALID_REQUEST, cpe.getError());
        }
        ClientRepresentation cRep = getClientByAdmin(cAppAdminId);
        assertEquals(org.keycloak.crypto.Algorithm.ES256, cRep.getAttributes().get(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG));

        // update by Admin REST API - success
        updateClientByAdmin(cAppAdminId, (ClientRepresentation clientRep) -> {
                clientRep.setAttributes(new HashMap<>());
                clientRep.getAttributes().put(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG, org.keycloak.crypto.Algorithm.PS384);
        });
        cRep = getClientByAdmin(cAppAdminId);
        assertEquals(org.keycloak.crypto.Algorithm.PS384, cRep.getAttributes().get(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG));

        // create dynamically - fail
        try {
            createClientByAdmin(generateSuffixedName("App-in-Dynamic"), (ClientRepresentation clientRep) -> {
                    clientRep.setSecret("secret");
                    clientRep.setAttributes(new HashMap<>());
                    clientRep.getAttributes().put(OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG, org.keycloak.crypto.Algorithm.RS384);
                });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_REQUEST, e.getMessage());
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
                     clientRep.setIdTokenSignedResponseAlg(org.keycloak.crypto.Algorithm.RS256);
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
    public void testSecureRedirectUriEnforceExecutor() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Ensimmainen Profiili", Boolean.FALSE, null)
                    .addExecutor(SecureRedirectUriEnforceExecutorFactory.PROVIDER_ID, 
                        createSecureRedirectUriEnforceExecutorConfig())
                    .toRepresentation()
                ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Ensimmainen Politiikka", Boolean.FALSE, Boolean.TRUE, null, null)
                    .addCondition(ClientUpdateContextConditionFactory.PROVIDER_ID, 
                        createClientUpdateContextConditionConfig(Arrays.asList(
                                ClientUpdateContextConditionFactory.BY_AUTHENTICATED_USER,
                                ClientUpdateContextConditionFactory.BY_INITIAL_ACCESS_TOKEN,
                                ClientUpdateContextConditionFactory.BY_REGISTRATION_ACCESS_TOKEN)))
                    .addProfile(PROFILE_NAME)
                    .toRepresentation()
                ).toString();
        updatePolicies(json);

        try {
            createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {
                clientRep.setRedirectUris(Collections.singletonList("http://newredirect"));
            });
            fail();
        } catch (ClientRegistrationException e) {
            assertEquals(ERR_MSG_CLIENT_REG_FAIL, e.getMessage());
        }

        // update policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Paivitetyn Ensimmaisen Politiikka", Boolean.FALSE, Boolean.TRUE, null, null)
                    .addCondition(ClientUpdateContextConditionFactory.PROVIDER_ID, 
                        createClientUpdateContextConditionConfig(Arrays.asList(
                                ClientUpdateContextConditionFactory.BY_AUTHENTICATED_USER,
                                ClientUpdateContextConditionFactory.BY_REGISTRATION_ACCESS_TOKEN)))
                    .addProfile(PROFILE_NAME)
                    .toRepresentation()
                ).toString();
        updatePolicies(json);

        try {
            createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {});
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testSecureSigningAlgorithmForSignedJwtEnforceExecutor() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Ensimmainen Profiili", Boolean.FALSE, null)
                    .addExecutor(SecureSigningAlgorithmForSignedJwtEnforceExecutorFactory.PROVIDER_ID, 
                        createSecureSigningAlgorithmForSignedJwtEnforceExecutorConfig())
                    .toRepresentation()
                ).toString();
        updateProfiles(json);

        // register policies
        String roleAlphaName = "sample-client-role-alpha";
        String roleZetaName = "sample-client-role-zeta";
        String roleCommonName = "sample-client-role-common";
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Den Forste Politikken", Boolean.FALSE, Boolean.TRUE, null, null)
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
                clientRep.getAttributes().put(OIDCConfigAttributes.TOKEN_ENDPOINT_AUTH_SIGNING_ALG, org.keycloak.crypto.Algorithm.ES256);
            });
        adminClient.realm(REALM_NAME).clients().get(cid).roles().create(RoleBuilder.create().name(roleAlphaName).build());
        adminClient.realm(REALM_NAME).clients().get(cid).roles().create(RoleBuilder.create().name(roleCommonName).build());


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
        Assume.assumeTrue("This test must be executed with enabled TLS.", ServerURLs.AUTH_SERVER_SSL_REQUIRED);

        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Az Elso Profil", Boolean.FALSE, null)
                    .addExecutor(HolderOfKeyEnforceExecutorFactory.PROVIDER_ID, 
                        createHolderOfKeyEnforceExecutorConfig(Boolean.TRUE))
                    .toRepresentation()
                ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Az Elso Politika", Boolean.FALSE, Boolean.TRUE, null, null)
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
    public void testNegativeLogicCondition() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Den Forste Profilen", Boolean.FALSE, null)
                    .addExecutor(SecureSessionEnforceExecutorFactory.PROVIDER_ID, createSecureSessionEnforceExecutorConfig())
                    .toRepresentation()
                ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.FALSE, Boolean.TRUE, null, null)
                    .addCondition(AnyClientConditionFactory.PROVIDER_ID, createAnyClientConditionConfig())
                    .addProfile(PROFILE_NAME)
                    .toRepresentation()
                ).toString();
        updatePolicies(json);

        String clientId = generateSuffixedName(CLIENT_NAME);
        String clientSecret = "secretBeta";
        createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret(clientSecret);
        });

        try {
            failLoginWithoutSecureSessionParameter(clientId, ERR_MSG_MISSING_NONCE);

            // update policies
            updatePolicy((new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.FALSE, Boolean.TRUE, null, null)
                            .addCondition(AnyClientConditionFactory.PROVIDER_ID, createAnyClientConditionConfig(Boolean.TRUE))
                            .addProfile(PROFILE_NAME)
                            .toRepresentation());

            successfulLoginAndLogout(clientId, clientSecret);

            // update policies
            updatePolicy((new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.FALSE, Boolean.TRUE, null, null)
                            .addCondition(AnyClientConditionFactory.PROVIDER_ID, createAnyClientConditionConfig(Boolean.FALSE))
                            .addProfile(PROFILE_NAME)
                            .toRepresentation());

            failLoginWithoutSecureSessionParameter(clientId, ERR_MSG_MISSING_NONCE);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testExtendedClientPolicyIntefacesForClientRegistrationPolicyMigration() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Den Forste Profilen", Boolean.FALSE, null)
                    .addExecutor(TestRaiseExeptionExecutorFactory.PROVIDER_ID, null)
                    .toRepresentation()
                ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.FALSE, Boolean.TRUE, null, null)
                    .addCondition(AnyClientConditionFactory.PROVIDER_ID, createAnyClientConditionConfig())
                    .addProfile(PROFILE_NAME)
                    .toRepresentation()
                ).toString();
        updatePolicies(json);

        String clientName = "ByAdmin-App" + KeycloakModelUtils.generateId().substring(0, 7);
        String clientId = null;

        try {
            createClientByAdmin(clientName, (ClientRepresentation clientRep) -> {});
            fail();
        } catch (ClientPolicyException cpe) {
            assertEquals(ClientPolicyEvent.REGISTERED.toString(), cpe.getError());
        }

        clientId = getClientByAdminWithName(clientName).getId();
        assertEquals(true, getClientByAdmin(clientId).isEnabled());
        try {
            updateClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
                clientRep.setEnabled(false);
            });
            fail();
        } catch (ClientPolicyException cpe) {
            assertEquals(ClientPolicyEvent.UPDATED.toString(), cpe.getError());
        }
        assertEquals(false, getClientByAdmin(clientId).isEnabled());

        try {
            deleteClientByAdmin(clientId);
            fail();
        } catch (ClientPolicyException cpe) {
            assertEquals(ClientPolicyEvent.UNREGISTER.toString(), cpe.getError());
        }

        // TODO : For dynamic client registration, the existing test scheme can not distinguish when the exception happens on which event so that the migrated client policy executors test them afterwards.
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

    private void setupPolicyClientIdAndSecretNotAcceptableAuthType(String policyName) throws ClientPolicyException {
        // register profiles
        String profileName = "MyProfile";
        String json = (new ClientProfilesBuilder()).addProfile(
                   (new ClientProfileBuilder()).createProfile(profileName, "Primum Profile", Boolean.FALSE, null)
                       .addExecutor(SecureClientAuthEnforceExecutorFactory.PROVIDER_ID, 
                           createSecureClientAuthEnforceExecutorConfig(Boolean.FALSE, 
                               Arrays.asList(JWTClientAuthenticator.PROVIDER_ID, JWTClientSecretAuthenticator.PROVIDER_ID, X509ClientAuthenticator.PROVIDER_ID),
                               null))
                       .toRepresentation()
                ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                   (new ClientPolicyBuilder()).createPolicy(policyName, "Primum Consilium", Boolean.FALSE, Boolean.TRUE, null, null)
                       .addCondition(ClientUpdateContextConditionFactory.PROVIDER_ID, 
                           createClientUpdateContextConditionConfig(Arrays.asList(ClientUpdateContextConditionFactory.BY_AUTHENTICATED_USER)))
                       .addProfile(profileName)
                       .toRepresentation()
               ).toString();
        updatePolicies(json);
    }

    private void setupPolicyAuthzCodeFlowUnderMultiPhasePolicy(String policyName) throws ClientPolicyException {
        // register profiles
        String profileName = "MyProfile";
        String json = (new ClientProfilesBuilder()).addProfile(
                   (new ClientProfileBuilder()).createProfile(profileName, "Primul Profil", Boolean.FALSE, null)
                       .addExecutor(SecureClientAuthEnforceExecutorFactory.PROVIDER_ID, 
                           createSecureClientAuthEnforceExecutorConfig(Boolean.TRUE, 
                               Arrays.asList(ClientIdAndSecretAuthenticator.PROVIDER_ID, JWTClientAuthenticator.PROVIDER_ID),
                               ClientIdAndSecretAuthenticator.PROVIDER_ID))
                       .addExecutor(PKCEEnforceExecutorFactory.PROVIDER_ID, 
                           createPKCEEnforceExecutorConfig(Boolean.TRUE))
                       .toRepresentation()
                ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                   (new ClientPolicyBuilder()).createPolicy(policyName, "Prima Politica", Boolean.FALSE, Boolean.TRUE, null, null)
                       .addCondition(ClientRolesConditionFactory.PROVIDER_ID, 
                           createClientRolesConditionConfig(Arrays.asList(SAMPLE_CLIENT_ROLE)))
                       .addCondition(ClientUpdateContextConditionFactory.PROVIDER_ID, 
                           createClientUpdateContextConditionConfig(Arrays.asList(ClientUpdateContextConditionFactory.BY_INITIAL_ACCESS_TOKEN)))
                       .addProfile(profileName)
                       .toRepresentation()
               ).toString();
        updatePolicies(json);
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

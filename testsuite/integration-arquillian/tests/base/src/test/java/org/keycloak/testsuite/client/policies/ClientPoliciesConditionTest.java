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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import jakarta.ws.rs.BadRequestException;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientSecretAuthenticator;
import org.keycloak.authentication.authenticators.client.X509ClientAuthenticator;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.common.Profile;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.OAuth2DeviceConfig;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientAccessTypeConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientAttributesConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientScopesConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdaterSourceGroupsConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdaterSourceHostsConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdaterSourceRolesConditionFactory;
import org.keycloak.services.clientpolicy.executor.PKCEEnforcerExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureClientAuthenticatorExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureSessionEnforceExecutorFactory;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LogoutConfirmPage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.services.clientpolicy.executor.TestRaiseExceptionExecutorFactory;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientPoliciesBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientPolicyBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientProfileBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientProfilesBuilder;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;

import static org.keycloak.testsuite.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createAnyClientConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createClientAccessTypeConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createClientAttributesConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createClientScopesConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createClientUpdateSourceGroupsConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createClientUpdateSourceHostsConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createClientUpdateSourceRolesConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createPKCEEnforceExecutorConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createSecureClientAuthenticatorExecutorConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createTestRaiseExeptionExecutorConfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * This test class is for testing a condition of client policies.
 *
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
@EnableFeature(value = Profile.Feature.CLIENT_SECRET_ROTATION)
public class ClientPoliciesConditionTest extends AbstractClientPoliciesTest {

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

    @Test
    public void testAnyClientCondition() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(SecureSessionEnforceExecutorFactory.PROVIDER_ID, null)
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

        String clientAlphaId = generateSuffixedName("Alpha-App");
        String clientAlphaSecret = "secretAlpha";
        createClientByAdmin(clientAlphaId, (ClientRepresentation clientRep) -> {
            clientRep.setDefaultRoles(List.of("sample-client-role-alpha").toArray(new String[1]));
            clientRep.setSecret(clientAlphaSecret);
        });

        String clientBetaId = generateSuffixedName("Beta-App");
        createClientByAdmin(clientBetaId, (ClientRepresentation clientRep) -> clientRep.setSecret("secretBeta"));

        try {
            failLoginWithoutSecureSessionParameter(clientBetaId, ERR_MSG_MISSING_NONCE);
            successfulLoginAndLogout(clientAlphaId, clientAlphaSecret, "yesitisnonce", "somestate");
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testClientUpdateSourceHostsCondition() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Prvni Profil")
                        .addExecutor(SecureClientAuthenticatorExecutorFactory.PROVIDER_ID,
                                createSecureClientAuthenticatorExecutorConfig(
                                        Arrays.asList(JWTClientAuthenticator.PROVIDER_ID, JWTClientSecretAuthenticator.PROVIDER_ID, X509ClientAuthenticator.PROVIDER_ID),
                                        null)
                        )
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Prvni Politika", Boolean.TRUE)
                        .addCondition(ClientUpdaterSourceHostsConditionFactory.PROVIDER_ID,
                                createClientUpdateSourceHostsConditionConfig(Arrays.asList("localhost", "127.0.0.1")))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        String clientId = generateSuffixedName(CLIENT_NAME);
        String clientSecret = "secret";
        try {
            createClientByAdmin(clientId, (ClientRepresentation clientRep) -> clientRep.setSecret(clientSecret));
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getMessage());
        }

        // update policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Aktualizovana Prvni Politika", Boolean.TRUE)
                        .addCondition(ClientUpdaterSourceHostsConditionFactory.PROVIDER_ID,
                                createClientUpdateSourceHostsConditionConfig(List.of("example.com")))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        try {
            createClientByAdmin(clientId, (ClientRepresentation clientRep) -> clientRep.setSecret(clientSecret));
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testClientUpdateSourceGroupsCondition() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Den Forste Profil")
                        .addExecutor(SecureClientAuthenticatorExecutorFactory.PROVIDER_ID,
                                createSecureClientAuthenticatorExecutorConfig(
                                        List.of(JWTClientAuthenticator.PROVIDER_ID),
                                        null)
                        )
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Den Forste Politik", Boolean.TRUE)
                        .addCondition(ClientUpdaterSourceGroupsConditionFactory.PROVIDER_ID,
                                createClientUpdateSourceGroupsConditionConfig(List.of("topGroup")))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        try {
            authCreateClients();
            createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {
            });
            fail();
        } catch (ClientRegistrationException e) {
            assertEquals(ERR_MSG_CLIENT_REG_FAIL, e.getMessage());
        }
        authManageClients();
        try {
            createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {
            });
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testClientUpdateSourceRolesCondition() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Il Primo Profilo")
                        .addExecutor(SecureClientAuthenticatorExecutorFactory.PROVIDER_ID,
                                createSecureClientAuthenticatorExecutorConfig(
                                        List.of(JWTClientSecretAuthenticator.PROVIDER_ID),
                                        null)
                        )
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Prima Politica", Boolean.TRUE)
                        .addCondition(ClientUpdaterSourceRolesConditionFactory.PROVIDER_ID,
                                createClientUpdateSourceRolesConditionConfig(List.of(Constants.REALM_MANAGEMENT_CLIENT_ID + "." + AdminRoles.CREATE_CLIENT)))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        try {
            authCreateClients();
            createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {
            });
            fail();
        } catch (ClientRegistrationException e) {
            assertEquals(ERR_MSG_CLIENT_REG_FAIL, e.getMessage());
        }
        authManageClients();
        try {
            createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {
            });
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testClientScopesOptionalCondition() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Het Eerste Profiel")
                        .addExecutor(PKCEEnforcerExecutorFactory.PROVIDER_ID,
                                createPKCEEnforceExecutorConfig(Boolean.TRUE))
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

        String clientId = generateSuffixedName(CLIENT_NAME);
        String clientSecret = "secret";
        createClientByAdmin(clientId, (ClientRepresentation clientRep) -> clientRep.setSecret(clientSecret));

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
    public void testClientScopesAnyCondition() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Het Eerste Profiel")
                        .addExecutor(PKCEEnforcerExecutorFactory.PROVIDER_ID,
                                createPKCEEnforceExecutorConfig(Boolean.TRUE))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Het Eerste Beleid", Boolean.TRUE)
                        .addCondition(ClientScopesConditionFactory.PROVIDER_ID,
                                createClientScopesConditionConfig(ClientScopesConditionFactory.ANY, List.of("email", "microprofile-jwt")))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        String clientId = generateSuffixedName(CLIENT_NAME);
        String clientSecret = "secret";
        String id = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> clientRep.setSecret(clientSecret));


        String emailClientScopeId = adminClient.realm(REALM_NAME)
                .getDefaultDefaultClientScopes().stream()
                .filter(scope -> "email".equals(scope.getName()))
                .map(ClientScopeRepresentation::getId)
                .findAny()
                .orElse(null);

        //remove email default client scope
        adminClient.realm(REALM_NAME).clients().get(id).removeDefaultClientScope(emailClientScopeId);

        try {
            //condition evaluates false
            oauth.scope("address" + " " + "phone");
            successfulLoginAndLogout(clientId, clientSecret);

            //condition evaluates true because of microprofile-jwt optional client scope
            oauth.scope("microprofile-jwt" + " " + "profile");
            failLoginByNotFollowingPKCE(clientId);

            adminClient.realm(REALM_NAME).clients().get(id).addDefaultClientScope(emailClientScopeId);
            oauth.scope(null);
            //condition evaluates true because of email default client scope
            failLoginByNotFollowingPKCE(clientId);

            successfulLoginAndLogoutWithPKCE(clientId, clientSecret, TEST_USER_NAME, TEST_USER_PASSWORD);
        } catch (Exception e) {
            fail();
        }

    }

    @Test
    public void testClientScopesConditionValidation() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Het Eerste Profiel")
                        .addExecutor(PKCEEnforcerExecutorFactory.PROVIDER_ID,
                                createPKCEEnforceExecutorConfig(Boolean.TRUE))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies with empty scopes
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "test", Boolean.TRUE)
                        .addCondition(ClientScopesConditionFactory.PROVIDER_ID,
                                createClientScopesConditionConfig(ClientScopesConditionFactory.ANY, null))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "test", Boolean.TRUE)
                        .addCondition(ClientScopesConditionFactory.PROVIDER_ID,
                                createClientScopesConditionConfig(ClientScopesConditionFactory.ANY, List.of("fake-client-scope")))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();

        ClientPoliciesRepresentation clientPolicies = json==null ? null : JsonSerialization.readValue(json, ClientPoliciesRepresentation.class);
        BadRequestException e = Assert.assertThrows(BadRequestException.class,
                () -> adminClient.realm(REALM_NAME).clientPoliciesPoliciesResource().updatePolicies(clientPolicies));

        ErrorRepresentation error = e.getResponse().readEntity(ErrorRepresentation.class);
        Assert.assertEquals("Invalid client-scopes configuration - Client scopes not allowed: [fake-client-scope]", error.getErrorMessage());
    }

    @Test
    public void testClientAttributesCondition() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Het Eerste Profiel")
                        .addExecutor(PKCEEnforcerExecutorFactory.PROVIDER_ID,
                                createPKCEEnforceExecutorConfig(Boolean.TRUE))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Het Eerste Beleid", Boolean.TRUE)
                        .addCondition(ClientAttributesConditionFactory.PROVIDER_ID,
                                createClientAttributesConditionConfig(new MultivaluedHashMap<>() {
                                    {
                                        putSingle("attr1", "Apple");
                                        putSingle("attr2", "Orange");
                                    }
                                }))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        String clientAlphaId = generateSuffixedName("Alpha-App");
        String clientSecret = "secret";
        createClientByAdmin(clientAlphaId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret(clientSecret);
            clientRep.setAttributes(new HashMap<>() {
                {
                    put("attr1", "Apple");
                    put("attr2", "Orange");
                    put("attr3", "Banana");
                }
            });
        });

        String clientBetaId = generateSuffixedName("Beta-App");
        createClientByAdmin(clientBetaId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret(clientSecret);
            clientRep.setAttributes(new HashMap<>() {
                {
                    put("attr1", "Apple");
                    put("attr2", "Peach"); // attr2 is not "Orange"
                    put("attr3", "Banana");
                }
            });
        });

        try {
            successfulLoginAndLogout(clientBetaId, clientSecret);
        } catch (Exception e) {
            fail();
        }

        try {
            failLoginByNotFollowingPKCE(clientAlphaId);
            successfulLoginAndLogoutWithPKCE(clientAlphaId, clientSecret, TEST_USER_NAME, TEST_USER_PASSWORD);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testClientAccessTypeCondition() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "El Primer Perfil")
                        .addExecutor(SecureSessionEnforceExecutorFactory.PROVIDER_ID, null)
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Primera Plitica", Boolean.TRUE)
                        .addCondition(ClientAccessTypeConditionFactory.PROVIDER_ID,
                                createClientAccessTypeConditionConfig(List.of(ClientAccessTypeConditionFactory.TYPE_CONFIDENTIAL)))
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

        // update profiles
        json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "El Primer Perfil")
                        .addExecutor(PKCEEnforcerExecutorFactory.PROVIDER_ID,
                                createPKCEEnforceExecutorConfig(Boolean.FALSE)) // check only
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // Attempt to create a confidential client without PKCE setting. Should fail
        try {
            createClientByAdmin(generateSuffixedName("Gamma-App"), (ClientRepresentation clientRep) -> {
                clientRep.setSecret("secretGamma");
                clientRep.setBearerOnly(Boolean.FALSE);
                clientRep.setPublicClient(Boolean.FALSE);
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getMessage());
            assertEquals("Invalid client metadata: code_challenge_method", e.getErrorDetail());
        }

        json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "El Primer Perfil")
                        .addExecutor(PKCEEnforcerExecutorFactory.PROVIDER_ID,
                                createPKCEEnforceExecutorConfig(Boolean.TRUE)) // enforce
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        authCreateClients();
        String clientGammaId = createClientDynamically(generateSuffixedName("Gamma-App"), (OIDCClientRepresentation clientRep) -> clientRep.setClientSecret("secretGamma"));

        ClientRepresentation clientRep = getClientByAdmin(clientGammaId);
        assertEquals(OAuth2Constants.PKCE_METHOD_S256, OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).getPkceCodeChallengeMethod());

        json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "El Primer Perfil")
                        .addExecutor(PKCEEnforcerExecutorFactory.PROVIDER_ID,
                                createPKCEEnforceExecutorConfig(Boolean.FALSE)) // check only
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // Attempt to update the confidential client with not allowed PKCE setting. Should fail
        try {
            updateClientByAdmin(clientGammaId, (ClientRepresentation updatingClientRep) -> {
                updatingClientRep.setAttributes(new HashMap<>());
                updatingClientRep.getAttributes().put(OIDCConfigAttributes.PKCE_CODE_CHALLENGE_METHOD, OAuth2Constants.PKCE_METHOD_PLAIN);
            });
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getMessage());
            assertEquals("Invalid client metadata: code_challenge_method", e.getErrorDetail());
        }
        ClientRepresentation cRep = getClientByAdmin(clientGammaId);
        assertEquals(OAuth2Constants.PKCE_METHOD_S256, cRep.getAttributes().get(OIDCConfigAttributes.PKCE_CODE_CHALLENGE_METHOD));

    }

    @Test
    public void testClientPolicyTriggeredForServiceAccountRequest() throws Exception {
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
                                createTestRaiseExeptionExecutorConfig(List.of(ClientPolicyEvent.SERVICE_ACCOUNT_TOKEN_REQUEST)))
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

        String origClientId = oauth.getClientId();
        oauth.client("service-account-app", "app-secret");
        try {
            AccessTokenResponse response = oauth.doClientCredentialsGrantAccessTokenRequest();
            assertEquals(400, response.getStatusCode());
            assertEquals(ClientPolicyEvent.SERVICE_ACCOUNT_TOKEN_REQUEST.toString(), response.getError());
            assertEquals("Exception thrown intentionally", response.getErrorDescription());
        } finally {
            oauth.client(origClientId);
        }
    }
}

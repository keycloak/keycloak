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
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;

import org.hamcrest.Matchers;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientSecretAuthenticator;
import org.keycloak.authentication.authenticators.client.X509ClientAuthenticator;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistration;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.Time;
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
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.representations.oidc.TokenMetadataRepresentation;
import org.keycloak.services.Urls;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ClientPolicyProvider;
import org.keycloak.services.clientpolicy.DefaultClientPolicyProviderFactory;
import org.keycloak.services.clientpolicy.condition.ClientAccessTypeConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientPolicyConditionProvider;
import org.keycloak.services.clientpolicy.condition.ClientUpdateContextConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdateSourceGroupsConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdateSourceHostsConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdateSourceRolesConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientRolesConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientScopesConditionFactory;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProvider;
import org.keycloak.services.clientpolicy.executor.SecureClientAuthEnforceExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureRedirectUriEnforceExecutorFactory;
import org.keycloak.services.clientpolicy.executor.PKCEEnforceExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureRequestObjectExecutor;
import org.keycloak.services.clientpolicy.executor.SecureRequestObjectExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureResponseTypeExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureSessionEnforceExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureSigningAlgorithmEnforceExecutorFactory;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.client.resources.TestApplicationResourceUrls;
import org.keycloak.testsuite.client.resources.TestOIDCEndpointsApplicationResource;
import org.keycloak.testsuite.rest.resource.TestingOIDCEndpointsApplicationResource.AuthorizationEndpointRequestObject;
import org.keycloak.testsuite.services.clientpolicy.condition.TestRaiseExeptionConditionFactory;
import org.keycloak.testsuite.util.OAuthClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.keycloak.testsuite.util.ServerURLs;
import org.keycloak.util.JsonSerialization;

@EnableFeature(value = Profile.Feature.CLIENT_POLICIES, skipRestart = true)
public class ClientPolicyBasicsTest extends AbstractKeycloakTest {

    private static final Logger logger = Logger.getLogger(ClientPolicyBasicsTest.class);

    static final String REALM_NAME = "test";
    static final String TEST_CLIENT = "test-app";

    static final String CLIENTUPDATECONTEXT_CONDITION_NAME = "ClientUpdateContextCondition";
    static final String CLIENTUPDATECONTEXT_CONDITION_ALPHA_NAME = "ClientUpdateContextCondition-alpha";

    static final String CLIENTROLES_CONDITION_NAME = "ClientRolesCondition";
    static final String CLIENTROLES_CONDITION_ALPHA_NAME = "ClientRolesCondition-alpha";
    static final String CLIENTROLES_CONDITION_BETA_NAME = "ClientRolesCondition-beta";

    static final String SECURERESPONSETYPE_EXECUTOR_NAME = "SecureResponseTypeExecutor";

    static final String SECUREREQUESTOBJECT_EXECUTOR_NAME = "SecureRequestObjectExecutor";

    ClientRegistration reg;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Before
    public void before() throws Exception {
        // get initial access token for Dynamic Client Registration with authentication
        reg = ClientRegistration.create().url(suiteContext.getAuthServerInfo().getContextRoot() + "/auth", REALM_NAME).build();
        ClientInitialAccessPresentation token = adminClient.realm(REALM_NAME).clientInitialAccess().create(new ClientInitialAccessCreatePresentation(0, 10));
        reg.auth(Auth.token(token));

    }

    @After
    public void after() throws Exception {
        reg.close();

    }

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
        setupPolicyAcceptableAuthType("MyPolicy");

        try {
            createClientByAdmin("Zahlungs-App", (ClientRepresentation clientRep) -> {
                clientRep.setClientAuthenticatorType(ClientIdAndSecretAuthenticator.PROVIDER_ID);
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(Errors.INVALID_REGISTRATION, e.getMessage());
        }
    }

    @Test
    public void testAdminClientRegisterAcceptableAuthType() throws ClientPolicyException {
        setupPolicyAcceptableAuthType("MyPolicy");

        String clientId = createClientByAdmin("Zahlungs-App", (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(JWTClientSecretAuthenticator.PROVIDER_ID);
        });
        try {
            assertEquals(JWTClientSecretAuthenticator.PROVIDER_ID, getClientByAdmin(clientId).getClientAuthenticatorType());
        } finally {
            deleteClientByAdmin(clientId);
        }
    }

    @Test
    public void testAdminClientUpdateUnacceptableAuthType() throws ClientPolicyException {
        setupPolicyAcceptableAuthType("MyPolicy");

        String clientId = createClientByAdmin("Zahlungs-App", (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(JWTClientSecretAuthenticator.PROVIDER_ID);
        });

        try {
            assertEquals(JWTClientSecretAuthenticator.PROVIDER_ID, getClientByAdmin(clientId).getClientAuthenticatorType());
 
            try {
                updateClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
                    clientRep.setClientAuthenticatorType(ClientIdAndSecretAuthenticator.PROVIDER_ID);
                });
                fail();
            } catch (BadRequestException bre) {}
            assertEquals(JWTClientSecretAuthenticator.PROVIDER_ID, getClientByAdmin(clientId).getClientAuthenticatorType());

        } finally {
            deleteClientByAdmin(clientId);
        }
    }

    @Test
    public void testAdminClientUpdateAcceptableAuthType() throws ClientPolicyException {
        setupPolicyAcceptableAuthType("MyPolicy");

        String clientId = createClientByAdmin("Zahlungs-App", (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(JWTClientSecretAuthenticator.PROVIDER_ID);
        });

        try {
            assertEquals(JWTClientSecretAuthenticator.PROVIDER_ID, getClientByAdmin(clientId).getClientAuthenticatorType());

            updateClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
                clientRep.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID);
            });
            assertEquals(JWTClientAuthenticator.PROVIDER_ID, getClientByAdmin(clientId).getClientAuthenticatorType());

        } finally {
            deleteClientByAdmin(clientId);
        }
    }

    @Test
    public void testAdminClientRegisterDefaultAuthType() {
        setupPolicyAcceptableAuthType("MyPolicy");

        try {
            createClientByAdmin("Zahlungs-App", (ClientRepresentation clientRep) -> {});
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(Errors.INVALID_REGISTRATION, e.getMessage());
        }
    }

    @Test
    public void testAdminClientUpdateDefaultAuthType() throws ClientPolicyException {
        setupPolicyAcceptableAuthType("MyPolicy");

        String clientId = createClientByAdmin("Zahlungs-App", (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(JWTClientSecretAuthenticator.PROVIDER_ID);
        });

        try {
            assertEquals(JWTClientSecretAuthenticator.PROVIDER_ID, getClientByAdmin(clientId).getClientAuthenticatorType());

            updateClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
                clientRep.setServiceAccountsEnabled(Boolean.FALSE);
            });
            assertEquals(JWTClientSecretAuthenticator.PROVIDER_ID, getClientByAdmin(clientId).getClientAuthenticatorType());
            assertEquals(Boolean.FALSE, getClientByAdmin(clientId).isServiceAccountsEnabled());
        } finally {
            deleteClientByAdmin(clientId);
        }
    }

    @Test
    public void testAdminClientAugmentedAuthType() throws ClientPolicyException {
        setupPolicyAcceptableAuthType("MyPolicy");

        updateExecutor("SecureClientAuthEnforceExecutor", (ComponentRepresentation provider) -> {
            setExecutorAugmentActivate(provider);
            setExecutorAugmentedClientAuthMethod(provider, X509ClientAuthenticator.PROVIDER_ID);
        });

        String clientId = createClientByAdmin("Zahlungs-App", (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(ClientIdAndSecretAuthenticator.PROVIDER_ID);
        });

        try {
            assertEquals(X509ClientAuthenticator.PROVIDER_ID, getClientByAdmin(clientId).getClientAuthenticatorType());

            updateExecutor("SecureClientAuthEnforceExecutor", (ComponentRepresentation provider) -> {
                setExecutorAugmentedClientAuthMethod(provider, JWTClientAuthenticator.PROVIDER_ID);
            });

            updateClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
                clientRep.setClientAuthenticatorType(JWTClientSecretAuthenticator.PROVIDER_ID);
            });
            assertEquals(JWTClientAuthenticator.PROVIDER_ID, getClientByAdmin(clientId).getClientAuthenticatorType());

        } finally {
            deleteClientByAdmin(clientId);
        }
    }

    @Test
    public void testDynamicClientRegisterAndUpdate() throws ClientRegistrationException {
        setupPolicyAcceptableAuthType("MyPolicy");

        String clientId = createClientDynamically("Gourmet-App", (OIDCClientRepresentation clientRep) -> {});
        try {
            assertEquals(OIDCLoginProtocol.CLIENT_SECRET_BASIC, getClientDynamically(clientId).getTokenEndpointAuthMethod());
            assertEquals(Boolean.FALSE, getClientDynamically(clientId).getTlsClientCertificateBoundAccessTokens());

            updateClientDynamically(clientId, (OIDCClientRepresentation clientRep) -> {
                clientRep.setTokenEndpointAuthMethod(OIDCLoginProtocol.CLIENT_SECRET_BASIC);
                clientRep.setTlsClientCertificateBoundAccessTokens(Boolean.TRUE);
            });
            assertEquals(OIDCLoginProtocol.CLIENT_SECRET_BASIC, getClientDynamically(clientId).getTokenEndpointAuthMethod());
            assertEquals(Boolean.TRUE, getClientDynamically(clientId).getTlsClientCertificateBoundAccessTokens());

        } finally {
            deleteClientDynamically(clientId);
        }
    }

    @Test
    public void testAuthzCodeFlowUnderMultiPhasePolicy() throws Exception {
        setupPolicyAuthzCodeFlowUnderMultiPhasePolicy("MultiPhasePolicy");

        String userName = "test-user@localhost";
        String userPassword = "password";
        String clientName = "Flughafen-App";
        String clientId = createClientDynamically(clientName, (OIDCClientRepresentation clientRep) -> {});
        events.expect(EventType.CLIENT_REGISTER).client(clientId).user(Matchers.isEmptyOrNullString()).assertEvent();
        OIDCClientRepresentation response = getClientDynamically(clientId);
        String clientSecret = response.getClientSecret();
        assertEquals(clientName, response.getClientName());
        assertEquals(OIDCLoginProtocol.CLIENT_SECRET_BASIC, response.getTokenEndpointAuthMethod());
        events.expect(EventType.CLIENT_INFO).client(clientId).user(Matchers.isEmptyOrNullString()).assertEvent();

        updateClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setDefaultRoles(Arrays.asList("sample-client-role").toArray(new String[1]));
        });

        successfulLoginAndLogoutWithPKCE(response.getClientId(), clientSecret, userName, userPassword);
    }

    @Test
    public void testCreateDeletePolicyRuntime() throws ClientRegistrationException {
        String clientId = createClientDynamically("Gourmet-App", (OIDCClientRepresentation clientRep) -> {});
        try {
            OIDCClientRepresentation clientRep = getClientDynamically(clientId);
            assertEquals(OIDCLoginProtocol.CLIENT_SECRET_BASIC, clientRep.getTokenEndpointAuthMethod());
            events.expect(EventType.CLIENT_REGISTER).client(clientId).user(Matchers.isEmptyOrNullString()).assertEvent();
            events.expect(EventType.CLIENT_INFO).client(clientId).user(Matchers.isEmptyOrNullString()).assertEvent();
            updateClientByAdmin(clientId, (ClientRepresentation cr) -> {
                cr.setDefaultRoles((String[]) Arrays.asList("sample-client-role").toArray(new String[1]));
            });

            successfulLoginAndLogout(clientId, clientRep.getClientSecret());

            setupPolicyAuthzCodeFlowUnderMultiPhasePolicy("MyPolicy");

            failLoginByNotFollowingPKCE(clientId);

            deletePolicy("MyPolicy");
            logger.info("... Deleted Policy : MyPolicy");

            successfulLoginAndLogout(clientId, clientRep.getClientSecret());

        } finally {
            deleteClientDynamically(clientId);
        }
    }

    @Test
    public void testCreateUpdateDeleteConditionRuntime() throws ClientRegistrationException, ClientPolicyException {
        String policyName = "MyPolicy";
        createPolicy(policyName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyName);

        createExecutor("PKCEEnforceExecutor", PKCEEnforceExecutorFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setExecutorAugmentActivate(provider);
        });
        registerExecutor("PKCEEnforceExecutor", policyName);
        logger.info("... Registered Executor : PKCEEnforceExecutor");

        String clientId = "Zahlungs-App";
        String clientSecret = "secret";
        String cid = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setDefaultRoles((String[]) Arrays.asList("sample-client-role").toArray(new String[1]));
            clientRep.setSecret(clientSecret);
        });

        try {
            successfulLoginAndLogout(clientId, clientSecret);
 
            createCondition(CLIENTROLES_CONDITION_NAME, ClientRolesConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
                setConditionClientRoles(provider, new ArrayList<>(Arrays.asList("sample-client-role")));
            });
            registerCondition(CLIENTROLES_CONDITION_NAME, policyName);
            logger.info("... Registered Condition : " + CLIENTROLES_CONDITION_NAME);

            failLoginByNotFollowingPKCE(clientId);

            updateCondition(CLIENTROLES_CONDITION_NAME, (ComponentRepresentation provider) -> {
                setConditionClientRoles(provider, new ArrayList<>(Arrays.asList("anothor-client-role")));
            });

            successfulLoginAndLogout(clientId, clientSecret);

            deleteCondition(CLIENTROLES_CONDITION_NAME, policyName);

            successfulLoginAndLogout(clientId, clientSecret);

        } finally {
            deleteClientByAdmin(cid);
        }
    }

    @Test
    public void testCreateUpdateDeleteExecutorRuntime() throws ClientRegistrationException, ClientPolicyException {
        String policyName = "MyPolicy";
        createPolicy(policyName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyName);

        createCondition(CLIENTROLES_CONDITION_NAME, ClientRolesConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setConditionClientRoles(provider, new ArrayList<>(Arrays.asList("sample-client-role")));
        });
        registerCondition(CLIENTROLES_CONDITION_NAME, policyName);
        logger.info("... Registered Condition : " + CLIENTROLES_CONDITION_NAME);

        createCondition(CLIENTUPDATECONTEXT_CONDITION_NAME, ClientUpdateContextConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setConditionRegistrationMethods(provider, new ArrayList<>(Arrays.asList(ClientUpdateContextConditionFactory.BY_AUTHENTICATED_USER)));
        });
        registerCondition(CLIENTUPDATECONTEXT_CONDITION_NAME, policyName);
        logger.info("... Registered Condition : " + CLIENTUPDATECONTEXT_CONDITION_NAME);

        String clientId = "Zahlungs-App";
        String clientSecret = "secret";
        String cid = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            String[] defaultRoles = {"sample-client-role"};
            clientRep.setDefaultRoles(defaultRoles);
            clientRep.setSecret(clientSecret);
        });

        try {
            successfulLoginAndLogout(clientId, clientSecret);
 
            createExecutor("PKCEEnforceExecutor", PKCEEnforceExecutorFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
                setExecutorAugmentDeactivate(provider);
            });
            registerExecutor("PKCEEnforceExecutor", policyName);
            logger.info("... Registered Executor : PKCEEnforceExecutor");

            failLoginByNotFollowingPKCE(clientId);

            updateExecutor("PKCEEnforceExecutor", (ComponentRepresentation provider) -> {
               setExecutorAugmentActivate(provider);
            });

            updateClientByAdmin(cid, (ClientRepresentation clientRep) -> {
                clientRep.setServiceAccountsEnabled(Boolean.FALSE);
            });
            assertEquals(false, getClientByAdmin(cid).isServiceAccountsEnabled());
            assertEquals(OAuth2Constants.PKCE_METHOD_S256, OIDCAdvancedConfigWrapper.fromClientRepresentation(getClientByAdmin(cid)).getPkceCodeChallengeMethod());

            deleteExecutor("PKCEEnforceExecutor", policyName);
            logger.info("... Deleted Executor : PKCEEnforceExecutor");

            updateClientByAdmin(cid, (ClientRepresentation clientRep) -> {
                OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setPkceCodeChallengeMethod(null);
            });
            assertEquals(null, OIDCAdvancedConfigWrapper.fromClientRepresentation(getClientByAdmin(cid)).getPkceCodeChallengeMethod());

            successfulLoginAndLogout(clientId, clientSecret);

        } finally {
            deleteClientByAdmin(cid);
        }

    }

    @Test
    public void testMultiplePolicies() throws ClientRegistrationException, ClientPolicyException {
        String policyAlphaName = "MyPolicy-alpha";
        createPolicy(policyAlphaName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyAlphaName);

        createCondition(CLIENTROLES_CONDITION_ALPHA_NAME, ClientRolesConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setConditionClientRoles(provider, new ArrayList<>(Arrays.asList("sample-client-role-alpha", "sample-client-role-zeta")));
        });
        registerCondition(CLIENTROLES_CONDITION_ALPHA_NAME, policyAlphaName);
        logger.info("... Registered Condition : " + CLIENTROLES_CONDITION_ALPHA_NAME);

        createCondition(CLIENTUPDATECONTEXT_CONDITION_ALPHA_NAME, ClientUpdateContextConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setConditionRegistrationMethods(provider, new ArrayList<>(Arrays.asList(ClientUpdateContextConditionFactory.BY_AUTHENTICATED_USER)));
        });
        registerCondition(CLIENTUPDATECONTEXT_CONDITION_ALPHA_NAME, policyAlphaName);
        logger.info("... Registered Condition : " + CLIENTUPDATECONTEXT_CONDITION_ALPHA_NAME);

        createExecutor("SecureClientAuthEnforceExecutor-alpha", SecureClientAuthEnforceExecutorFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setExecutorAcceptedClientAuthMethods(provider, new ArrayList<>(Arrays.asList(ClientIdAndSecretAuthenticator.PROVIDER_ID)));
            setExecutorAugmentActivate(provider);
            setExecutorAugmentedClientAuthMethod(provider, ClientIdAndSecretAuthenticator.PROVIDER_ID);
        });
        registerExecutor("SecureClientAuthEnforceExecutor-alpha", policyAlphaName);
        logger.info("... Registered Executor : SecureClientAuthEnforceExecutor-alpha");

        String policyBetaName = "MyPolicy-beta";
        createPolicy(policyBetaName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyBetaName);

        createCondition(CLIENTROLES_CONDITION_BETA_NAME, ClientRolesConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setConditionClientRoles(provider, new ArrayList<>(Arrays.asList("sample-client-role-beta", "sample-client-role-zeta")));
        });
        registerCondition(CLIENTROLES_CONDITION_BETA_NAME, policyBetaName);
        logger.info("... Registered Condition : " + CLIENTROLES_CONDITION_BETA_NAME);

        createExecutor("PKCEEnforceExecutor-beta", PKCEEnforceExecutorFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setExecutorAugmentActivate(provider);
        });
        registerExecutor("PKCEEnforceExecutor-beta", policyBetaName);
        logger.info("... Registered Executor : PKCEEnforceExecutor-beta");

        String clientAlphaId = "Alpha-App";
        String clientAlphaSecret = "secretAlpha";
        String cAlphaId = createClientByAdmin(clientAlphaId, (ClientRepresentation clientRep) -> {
            clientRep.setDefaultRoles((String[]) Arrays.asList("sample-client-role-alpha", "sample-client-role-common").toArray(new String[2]));
            clientRep.setSecret(clientAlphaSecret);
            clientRep.setClientAuthenticatorType(JWTClientSecretAuthenticator.PROVIDER_ID);
        });

        String clientBetaId = "Beta-App";
        String clientBetaSecret = "secretBeta";
        String cBetaId = createClientByAdmin(clientBetaId, (ClientRepresentation clientRep) -> {
            clientRep.setDefaultRoles((String[]) Arrays.asList("sample-client-role-beta", "sample-client-role-common").toArray(new String[2]));
            clientRep.setSecret(clientBetaSecret);
        });

        try {
            assertEquals(ClientIdAndSecretAuthenticator.PROVIDER_ID, getClientByAdmin(cAlphaId).getClientAuthenticatorType());

            successfulLoginAndLogout(clientAlphaId, clientAlphaSecret);

            failLoginByNotFollowingPKCE(clientBetaId);

        } finally {
            deleteClientByAdmin(cAlphaId);
            deleteClientByAdmin(cBetaId);
        }
    }

    @Test
    public void testIntentionalExceptionOnCondition() throws ClientRegistrationException, ClientPolicyException {
        String policyName = "MyPolicy";
        createPolicy(policyName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyName);

        createCondition("TestRaiseExeptionCondition", TestRaiseExeptionConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
        });
        registerCondition("TestRaiseExeptionCondition", policyName);
        logger.info("... Registered Condition : TestRaiseExeptionCondition-beta");

        try {
            createClientByAdmin("Zahlungs-App", (ClientRepresentation clientRep) -> {
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(Errors.INVALID_REGISTRATION, e.getMessage());
        }
    }

    @Test
    public void testSecureResponseTypeExecutor() throws ClientRegistrationException, ClientPolicyException {
        String policyName = "MyPolicy";
        createPolicy(policyName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyName);

        createCondition(CLIENTROLES_CONDITION_NAME, ClientRolesConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setConditionClientRoles(provider, new ArrayList<>(Arrays.asList("sample-client-role")));
        });
        registerCondition(CLIENTROLES_CONDITION_NAME, policyName);
        logger.info("... Registered Condition : " + CLIENTROLES_CONDITION_NAME);

        createExecutor(SECURERESPONSETYPE_EXECUTOR_NAME, SecureResponseTypeExecutorFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
        });
        registerExecutor(SECURERESPONSETYPE_EXECUTOR_NAME, policyName);
        logger.info("... Registered Executor : " + SECURERESPONSETYPE_EXECUTOR_NAME);

        String clientId = "Zahlungs-App";
        String clientSecret = "secret";
        String cid = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            String[] defaultRoles = {"sample-client-role"};
            clientRep.setDefaultRoles(defaultRoles);
            clientRep.setSecret(clientSecret);
            clientRep.setStandardFlowEnabled(Boolean.TRUE);
            clientRep.setImplicitFlowEnabled(Boolean.TRUE);
            clientRep.setPublicClient(Boolean.FALSE);
        });

        try {
            oauth.clientId(clientId);
            oauth.openLoginForm();
            assertEquals(OAuthErrorException.INVALID_REQUEST, oauth.getCurrentQuery().get(OAuth2Constants.ERROR));
            assertEquals("invalid response_type", oauth.getCurrentQuery().get(OAuth2Constants.ERROR_DESCRIPTION));

            oauth.responseType(OIDCResponseType.CODE + " " + OIDCResponseType.ID_TOKEN + " "  + OIDCResponseType.TOKEN);
            oauth.nonce("cie8cjcwiw");
            oauth.doLogin("test-user@localhost", "password");

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
            oauth.doLogin("test-user@localhost", "password");

            loginEvent = events.expectLogin().client(clientId).assertEvent();
            sessionId = loginEvent.getSessionId();
            codeId = loginEvent.getDetails().get(Details.CODE_ID);
            code = new OAuthClient.AuthorizationEndpointResponse(oauth).getCode();
            res = oauth.doAccessTokenRequest(code, clientSecret);
            assertEquals(200, res.getStatusCode());
            events.expectCodeToToken(codeId, sessionId).client(clientId).assertEvent();

            oauth.doLogout(res.getRefreshToken(), clientSecret);
            events.expectLogout(sessionId).client(clientId).clearDetails().assertEvent();
        } finally {
            deleteClientByAdmin(cid);
        }
    }

    @Test
    public void testSecureRequestObjectExecutor() throws ClientRegistrationException, ClientPolicyException, URISyntaxException, IOException {
        String policyName = "MyPolicy";
        createPolicy(policyName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyName);

        createCondition(CLIENTROLES_CONDITION_NAME, ClientRolesConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setConditionClientRoles(provider, new ArrayList<>(Arrays.asList("sample-client-role")));
        });
        registerCondition(CLIENTROLES_CONDITION_NAME, policyName);
        logger.info("... Registered Condition : " + CLIENTROLES_CONDITION_NAME);

        createExecutor(SECUREREQUESTOBJECT_EXECUTOR_NAME, SecureRequestObjectExecutorFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
        });
        registerExecutor(SECUREREQUESTOBJECT_EXECUTOR_NAME, policyName);
        logger.info("... Registered Executor : " + SECUREREQUESTOBJECT_EXECUTOR_NAME);

        String clientId = "Zahlungs-App";
        String clientSecret = "secret";
        String cid = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            String[] defaultRoles = {"sample-client-role"};
            clientRep.setDefaultRoles(defaultRoles);
            clientRep.setSecret(clientSecret);
        });

        try {
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
        } finally {
            deleteClientByAdmin(cid);
        }

    }

    @Test
    public void testSecureSessionEnforceExecutor() throws ClientRegistrationException, ClientPolicyException {
        String policyBetaName = "MyPolicy-beta";
        createPolicy(policyBetaName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyBetaName);

        createCondition("ClientRolesCondition-beta", ClientRolesConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setConditionClientRoles(provider, new ArrayList<>(Arrays.asList("sample-client-role-beta")));
        });
        registerCondition("ClientRolesCondition-beta", policyBetaName);
        logger.info("... Registered Condition : ClientRolesCondition-beta");

        createExecutor("SecureSessionEnforceExecutor-beta", SecureSessionEnforceExecutorFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
        });
        registerExecutor("SecureSessionEnforceExecutor-beta", policyBetaName);
        logger.info("... Registered Executor : SecureSessionEnforceExecutor-beta");

        String clientAlphaId = "Alpha-App";
        String clientAlphaSecret = "secretAlpha";
        String cAlphaId = createClientByAdmin(clientAlphaId, (ClientRepresentation clientRep) -> {
            clientRep.setDefaultRoles((String[]) Arrays.asList("sample-client-role-alpha").toArray(new String[1]));
            clientRep.setSecret(clientAlphaSecret);
        });

        String clientBetaId = "Beta-App";
        String clientBetaSecret = "secretBeta";
        String cBetaId = createClientByAdmin(clientBetaId, (ClientRepresentation clientRep) -> {
            clientRep.setDefaultRoles((String[]) Arrays.asList("sample-client-role-beta").toArray(new String[1]));
            clientRep.setSecret(clientBetaSecret);
        });

        try {
            successfulLoginAndLogout(clientAlphaId, clientAlphaSecret);

            oauth.openid(false);
            successfulLoginAndLogout(clientAlphaId, clientAlphaSecret);

            oauth.openid(true);
            failLoginWithoutSecureSessionParameter(clientBetaId, "Missing parameter: nonce");

            oauth.nonce("yesitisnonce");
            successfulLoginAndLogout(clientBetaId, clientBetaSecret);

            oauth.openid(false);
            oauth.stateParamHardcoded(null);
            failLoginWithoutSecureSessionParameter(clientBetaId, "Missing parameter: state");

            oauth.stateParamRandom();
            successfulLoginAndLogout(clientBetaId, clientBetaSecret);
        } finally {
            deleteClientByAdmin(cAlphaId);
            deleteClientByAdmin(cBetaId);
        }
    }

    @Test
    public void testClientScopesCondition() throws ClientRegistrationException, ClientPolicyException {
        String policyName = "MyPolicy";
        createPolicy(policyName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyName);

        createCondition("ClientScopesCondition", ClientScopesConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setConditionClientScopes(provider, new ArrayList<>(Arrays.asList("offline_access", "microprofile-jwt")));
        });
        registerCondition("ClientScopesCondition", policyName);
        logger.info("... Registered Condition : ClientScopesCondition");

        createExecutor("PKCEEnforceExecutor", PKCEEnforceExecutorFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setExecutorAugmentActivate(provider);
        });
        registerExecutor("PKCEEnforceExecutor", policyName);
        logger.info("... Registered Executor : PKCEEnforceExecutor");

        String clientAlphaId = "Alpha-App";
        String clientAlphaSecret = "secretAlpha";
        String cAlphaId = createClientByAdmin(clientAlphaId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret(clientAlphaSecret);
        });

        try {
            oauth.scope("address" + " " + "phone");
            successfulLoginAndLogout(clientAlphaId, clientAlphaSecret);

            oauth.scope("microprofile-jwt" + " " + "profile");
            failLoginByNotFollowingPKCE(clientAlphaId);

            successfulLoginAndLogoutWithPKCE(clientAlphaId, clientAlphaSecret, "test-user@localhost", "password");
        } catch (Exception e) {
            fail();
        } finally {
            deleteClientByAdmin(cAlphaId);
        }
    }

    @Test
    public void testSecureSigningAlgorithmEnforceExecutor() throws ClientRegistrationException, ClientPolicyException {
        String policyName = "MyPolicy";
        createPolicy(policyName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyName);

        createCondition("ClientUpdateContextCondition", ClientUpdateContextConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setConditionRegistrationMethods(provider, new ArrayList<>(Arrays.asList(
                    ClientUpdateContextConditionFactory.BY_AUTHENTICATED_USER,
                    ClientUpdateContextConditionFactory.BY_INITIAL_ACCESS_TOKEN,
                    ClientUpdateContextConditionFactory.BY_REGISTRATION_ACCESS_TOKEN)));
        });
        registerCondition("ClientUpdateContextCondition", policyName);
        logger.info("... Registered Condition : ClientUpdateContextConditionFactory");

        createExecutor("SecureSigningAlgorithmEnforceExecutor", SecureSigningAlgorithmEnforceExecutorFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
        });
        registerExecutor("SecureSigningAlgorithmEnforceExecutor", policyName);
        logger.info("... Registered Executor : SecureSigningAlgorithmEnforceExecutor");

        String cAppAdminId = null;
        String cAppDynamicId = null;
        try {

            // create by Admin REST API - fail
            try {
                createClientByAdmin("App-by-Admin", (ClientRepresentation clientRep) -> {
                    clientRep.setDefaultRoles((String[]) Arrays.asList("sample-client-role-beta").toArray(new String[1]));
                    clientRep.setSecret("secretBeta");
                    clientRep.setAttributes(new HashMap<>());
                    clientRep.getAttributes().put(OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG, Algorithm.none.name());
                });
                fail();
            } catch (ClientPolicyException e) {
                assertEquals(Errors.INVALID_REGISTRATION, e.getMessage());
            }

            // create by Admin REST API - success
            cAppAdminId = createClientByAdmin("App-by-Admin", (ClientRepresentation clientRep) -> {
                clientRep.setAttributes(new HashMap<>());
                clientRep.getAttributes().put(OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG, Algorithm.PS256.name());
                clientRep.getAttributes().put(OIDCConfigAttributes.REQUEST_OBJECT_SIGNATURE_ALG, Algorithm.ES256.name());
                clientRep.getAttributes().put(OIDCConfigAttributes.ID_TOKEN_SIGNED_RESPONSE_ALG, Algorithm.ES256.name());
                clientRep.getAttributes().put(OIDCConfigAttributes.TOKEN_ENDPOINT_AUTH_SIGNING_ALG, Algorithm.ES256.name());
                clientRep.getAttributes().put(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG, Algorithm.ES256.name());
            });

            // update by Admin REST API - fail
            try {
            updateClientByAdmin(cAppAdminId, (ClientRepresentation clientRep) -> {
                clientRep.setAttributes(new HashMap<>());
                clientRep.getAttributes().put(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG, Algorithm.RS512.name());
            });
            } catch (Exception e) {
                assertEquals("HTTP 400 Bad Request", e.getMessage());
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
                createClientByAdmin("App-in-Dynamic", (ClientRepresentation clientRep) -> {
                    clientRep.setDefaultRoles((String[]) Arrays.asList("sample-client-role-beta").toArray(new String[1]));
                    clientRep.setSecret("secretBeta");
                    clientRep.setAttributes(new HashMap<>());
                    clientRep.getAttributes().put(OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG, Algorithm.RS384.name());
                });
                fail();
            } catch (ClientPolicyException e) {
                assertEquals(Errors.INVALID_REGISTRATION, e.getMessage());
            }

            // create dynamically - success
            cAppDynamicId = createClientDynamically("App-in-Dynamic", (OIDCClientRepresentation clientRep) -> {
                clientRep.setUserinfoSignedResponseAlg(Algorithm.ES256.name());
                clientRep.setRequestObjectSigningAlg(Algorithm.ES256.name());
                clientRep.setIdTokenSignedResponseAlg(Algorithm.PS256.name());
                clientRep.setTokenEndpointAuthSigningAlg(Algorithm.PS256.name());
            });
            events.expect(EventType.CLIENT_REGISTER).client(cAppDynamicId).user(Matchers.isEmptyOrNullString()).assertEvent();
            getClientDynamically(cAppDynamicId);

            // update dynamically - fail
            try {
                 updateClientDynamically(cAppDynamicId, (OIDCClientRepresentation clientRep) -> {
                     clientRep.setIdTokenSignedResponseAlg(Algorithm.RS256.name());
                 });
                fail();
            } catch (ClientRegistrationException e) {
                assertEquals("Failed to send request", e.getMessage());
            }
            OIDCClientRepresentation oidcCRep = getClientDynamically(cAppDynamicId);
            assertEquals(Algorithm.PS256.name(), oidcCRep.getIdTokenSignedResponseAlg());

            // update dynamically - success
            updateClientDynamically(cAppDynamicId, (OIDCClientRepresentation clientRep) -> {
                clientRep.setIdTokenSignedResponseAlg(Algorithm.ES384.name());
            });
            oidcCRep = getClientDynamically(cAppDynamicId);
            assertEquals(Algorithm.ES384.name(), oidcCRep.getIdTokenSignedResponseAlg());

        } finally {
            deleteClientByAdmin(cAppAdminId);
            deleteClientDynamically(cAppDynamicId);
        }
    }

    @Test
    public void testClientAccessTypeCondition() throws ClientRegistrationException, ClientPolicyException {
        String policyName = "MyPolicy";
        createPolicy(policyName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyName);

        createCondition("ClientAccessTypeCondition", ClientAccessTypeConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setConditionClientAccessType(provider, new ArrayList<>(Arrays.asList(ClientAccessTypeConditionFactory.TYPE_CONFIDENTIAL)));
        });
        registerCondition("ClientAccessTypeCondition", policyName);
        logger.info("... Registered Condition : ClientAccessTypeCondition");

        createExecutor("SecureSessionEnforceExecutor", SecureSessionEnforceExecutorFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
        });
        registerExecutor("SecureSessionEnforceExecutor", policyName);
        logger.info("... Registered Executor : SecureSessionEnforceExecutor");

        // confidential client
        String clientAlphaId = "Alpha-App";
        String clientAlphaSecret = "secretAlpha";
        String cAlphaId = createClientByAdmin(clientAlphaId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret(clientAlphaSecret);
            clientRep.setBearerOnly(Boolean.FALSE);
            clientRep.setPublicClient(Boolean.FALSE);
        });

        // public client
        String clientBetaId = "Beta-App";
        String cBetaId = createClientByAdmin(clientBetaId, (ClientRepresentation clientRep) -> {
            clientRep.setBearerOnly(Boolean.FALSE);
            clientRep.setPublicClient(Boolean.TRUE);
        });

        try {
            successfulLoginAndLogout(clientBetaId, null);
            failLoginWithoutNonce(clientAlphaId);
        } finally {
            deleteClientByAdmin(cAlphaId);
            deleteClientByAdmin(cBetaId);
        }
    }

    @Test
    public void testConditionWithoutNoConfiguration() throws ClientRegistrationException, ClientPolicyException {
        String policyName = "MyPolicy-ClientAccessTypeCondition";
        createPolicy(policyName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyName);
        createCondition("ClientAccessTypeCondition", ClientAccessTypeConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
        });
        registerCondition("ClientAccessTypeCondition", policyName);
        logger.info("... Registered Condition : ClientAccessTypeCondition");

        policyName = "MyPolicy-ClientUpdateSourceGroupsCondition";
        createPolicy(policyName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyName);
        createCondition("ClientUpdateSourceGroupsCondition", ClientUpdateSourceGroupsConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
        });
        registerCondition("ClientUpdateSourceGroupsCondition", policyName);
        logger.info("... Registered Condition : ClientUpdateSourceGroupsCondition");

        policyName = "MyPolicy-ClientUpdateSourceRolesCondition";
        createPolicy(policyName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyName);
        createCondition("ClientUpdateSourceRolesCondition", ClientUpdateSourceRolesConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
        });
        registerCondition("ClientUpdateSourceRolesCondition", policyName);
        logger.info("... Registered Condition : ClientUpdateSourceRolesCondition");

        policyName = "MyPolicy-ClientUpdateContextCondition";
        createPolicy(policyName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyName);
        createCondition("ClientUpdateContextCondition", ClientUpdateContextConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
        });
        registerCondition("ClientUpdateContextCondition", policyName);
        logger.info("... Registered Condition : ClientUpdateContextCondition");

        policyName = "MyPolicy-SecureSessionEnforceExecutor";
        createPolicy(policyName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyName);
        createExecutor("SecureSessionEnforceExecutor", SecureSessionEnforceExecutorFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
        });
        registerExecutor("SecureSessionEnforceExecutor", policyName);
        logger.info("... Registered Executor : SecureSessionEnforceExecutor");

        String clientAlphaId = "Alpha-App";
        String clientAlphaSecret = "secretAlpha";
        String cAlphaId = createClientByAdmin(clientAlphaId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret(clientAlphaSecret);
            clientRep.setBearerOnly(Boolean.FALSE);
            clientRep.setPublicClient(Boolean.FALSE);
        });

        try {
            successfulLoginAndLogout(clientAlphaId, clientAlphaSecret);
        } finally {
            deleteClientByAdmin(cAlphaId);
        }
    }

    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void testClientUpdateSourceHostsCondition() throws ClientRegistrationException, ClientPolicyException {
        String policyName = "MyPolicy";
        createPolicy(policyName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyName);

        createCondition("ClientUpdateSourceHostsCondition", ClientUpdateSourceHostsConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setConditionClientUpdateSourceHosts(provider, new ArrayList<>(Arrays.asList("localhost", "127.0.0.1")));
        });
        registerCondition("ClientUpdateSourceHostsCondition", policyName);
        logger.info("... Registered Condition : ClientUpdateSourceHostsCondition");

        createExecutor("SecureClientAuthEnforceExecutor", SecureClientAuthEnforceExecutorFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setExecutorAcceptedClientAuthMethods(provider, new ArrayList<>(Arrays.asList(
                    JWTClientAuthenticator.PROVIDER_ID, JWTClientSecretAuthenticator.PROVIDER_ID, X509ClientAuthenticator.PROVIDER_ID)));
        });
        registerExecutor("SecureClientAuthEnforceExecutor", policyName);
        logger.info("... Registered Executor : SecureClientAuthEnforceExecutor");

        String clientAlphaId = "Alpha-App";
        String clientAlphaSecret = "secretAlpha";
        try {
            createClientByAdmin(clientAlphaId, (ClientRepresentation clientRep) -> {
                clientRep.setSecret(clientAlphaSecret);
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(Errors.INVALID_REGISTRATION, e.getMessage());
        }

        String cAlphaId = null;
        try {
            updateCondition("ClientUpdateSourceHostsCondition", (ComponentRepresentation provider) -> {
                setConditionClientUpdateSourceHosts(provider, new ArrayList<>(Arrays.asList("example.com")));
            });
            cAlphaId = createClientByAdmin(clientAlphaId, (ClientRepresentation clientRep) -> {
                clientRep.setSecret(clientAlphaSecret);
            });
        } finally {
            deleteClientByAdmin(cAlphaId);
        }
    }

    @Test
    public void testClientUpdateSourceGroupsCondition() throws ClientRegistrationException, ClientPolicyException {
        String policyName = "MyPolicy";
        createPolicy(policyName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyName);

        createCondition("ClientUpdateSourceGroupsCondition", ClientUpdateSourceGroupsConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setConditionClientUpdateSourceGroups(provider, new ArrayList<>(Arrays.asList("topGroup")));
        });
        registerCondition("ClientUpdateSourceGroupsCondition", policyName);
        logger.info("... Registered Condition : ClientUpdateSourceGroupsCondition");

        createExecutor("SecureClientAuthEnforceExecutor", SecureClientAuthEnforceExecutorFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setExecutorAcceptedClientAuthMethods(provider, new ArrayList<>(Arrays.asList(JWTClientAuthenticator.PROVIDER_ID)));
        });
        registerExecutor("SecureClientAuthEnforceExecutor", policyName);
        logger.info("... Registered Executor : SecureClientAuthEnforceExecutor");

        String cid = null;
        try {
            try {
                authCreateClients();
                createClientDynamically("Gourmet-App", (OIDCClientRepresentation clientRep) -> {});
                fail();
            } catch (ClientRegistrationException e) {
                assertEquals("Failed to send request", e.getMessage());
            }
            authManageClients();
            cid = createClientDynamically("Gourmet-App", (OIDCClientRepresentation clientRep) -> {});
        } finally {
            deleteClientByAdmin(cid);

        }
    }

    @Test
    public void testUpdatingClientSourceRolesCondition() throws ClientRegistrationException, ClientPolicyException {
        String policyName = "MyPolicy";
        createPolicy(policyName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyName);

        createCondition("ClientUpdateSourceRolesCondition", ClientUpdateSourceRolesConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setConditionUpdatingClientSourceRoles(provider, new ArrayList<>(Arrays.asList(AdminRoles.CREATE_CLIENT)));
        });
        registerCondition("ClientUpdateSourceRolesCondition", policyName);
        logger.info("... Registered Condition : ClientUpdateSourceRolesCondition");

        createExecutor("SecureClientAuthEnforceExecutor", SecureClientAuthEnforceExecutorFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setExecutorAcceptedClientAuthMethods(provider, new ArrayList<>(Arrays.asList(JWTClientAuthenticator.PROVIDER_ID)));
        });
        registerExecutor("SecureClientAuthEnforceExecutor", policyName);
        logger.info("... Registered Executor : SecureClientAuthEnforceExecutor");

        String cid = null;
        try {
            try {
                authCreateClients();
                createClientDynamically("Gourmet-App", (OIDCClientRepresentation clientRep) -> {});
                fail();
            } catch (ClientRegistrationException e) {
                assertEquals("Failed to send request", e.getMessage());
            }
            authManageClients();
            cid = createClientDynamically("Gourmet-App", (OIDCClientRepresentation clientRep) -> {
            });
        } finally {
            deleteClientByAdmin(cid);

        }
    }

    @Test
    public void testSecureRedirectUriEnforceExecutor() throws ClientRegistrationException, ClientPolicyException {
        String policyName = "MyPolicy";
        createPolicy(policyName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyName);

        createCondition("ClientUpdateContextCondition", ClientUpdateContextConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setConditionRegistrationMethods(provider, new ArrayList<>(Arrays.asList(
                    ClientUpdateContextConditionFactory.BY_AUTHENTICATED_USER,
                    ClientUpdateContextConditionFactory.BY_INITIAL_ACCESS_TOKEN,
                    ClientUpdateContextConditionFactory.BY_REGISTRATION_ACCESS_TOKEN)));
        });
        registerCondition("ClientUpdateContextCondition", policyName);
        logger.info("... Registered Condition : UpdatingClientSourceCondition");

        createExecutor("SecureRedirectUriEnforceExecutor", SecureRedirectUriEnforceExecutorFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
        });
        registerExecutor("SecureRedirectUriEnforceExecutor", policyName);
        logger.info("... Registered Executor : SecureRedirectUriEnforceExecutor");

        String cid = null;
        try {
            try {
                createClientDynamically("Gourmet-App", (OIDCClientRepresentation clientRep) -> {
                    clientRep.setRedirectUris(Collections.singletonList("http://newredirect"));
                });
                fail();
            } catch (ClientRegistrationException e) {
                assertEquals("Failed to send request", e.getMessage());
            }
            updateCondition("ClientUpdateContextCondition", (ComponentRepresentation provider) -> {
                setConditionRegistrationMethods(provider, new ArrayList<>(Arrays.asList(
                        ClientUpdateContextConditionFactory.BY_AUTHENTICATED_USER,
                        ClientUpdateContextConditionFactory.BY_REGISTRATION_ACCESS_TOKEN)));
            });
            cid = createClientDynamically("Gourmet-App", (OIDCClientRepresentation clientRep) -> {
            });
        } finally {
            deleteClientByAdmin(cid);
        }
    }

    private AuthorizationEndpointRequestObject createValidRequestObjectForSecureRequestObjectExecutor(String clientId) throws URISyntaxException {
        AuthorizationEndpointRequestObject requestObject = new AuthorizationEndpointRequestObject();
        requestObject.id(KeycloakModelUtils.generateId());
        requestObject.iat(Long.valueOf(Time.currentTime()));
        requestObject.exp(requestObject.getIat() + Long.valueOf(300));
        requestObject.nbf(Long.valueOf(0));
        requestObject.setClientId(clientId);
        requestObject.setResponseType("code");
        requestObject.setRedirectUriParam(oauth.getRedirectUri());
        requestObject.setScope("openid");
        String scope = KeycloakModelUtils.generateId();
        oauth.stateParamHardcoded(scope);
        requestObject.setState(scope);
        requestObject.setMax_age(Integer.valueOf(600));
        requestObject.setOtherClaims("custom_claim_ein", "rot");
        requestObject.audience(Urls.realmIssuer(new URI(suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth"), REALM_NAME), "https://example.com");
        return requestObject;
    }

    private void registerRequestObject(AuthorizationEndpointRequestObject requestObject, String clientId, Algorithm sigAlg, boolean isUseRequestUri) throws URISyntaxException, IOException {
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();

        // Set required signature for request_uri
        // use and set jwks_url
        ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm(REALM_NAME), clientId);
        ClientRepresentation clientRep = clientResource.toRepresentation();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestObjectSignatureAlg(sigAlg);
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUseJwksUrl(true);
        String jwksUrl = TestApplicationResourceUrls.clientJwksUri();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setJwksUrl(jwksUrl);
        clientResource.update(clientRep);

        oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();

        // generate and register client keypair
        oidcClientEndpointsResource.generateKeys(sigAlg.name());

        // register request object
        byte[] contentBytes = JsonSerialization.writeValueAsBytes(requestObject);
        String encodedRequestObject = Base64Url.encode(contentBytes);
        oidcClientEndpointsResource.registerOIDCRequest(encodedRequestObject, sigAlg.name());

        if (isUseRequestUri) {
            oauth.request(null);
            oauth.requestUri(TestApplicationResourceUrls.clientRequestUri());
        } else {
            oauth.requestUri(null);
            oauth.request(oidcClientEndpointsResource.getOIDCRequest());
        }
    }

    private void setupPolicyAcceptableAuthType(String policyName) {

        createPolicy(policyName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyName);

        createCondition(CLIENTUPDATECONTEXT_CONDITION_NAME, ClientUpdateContextConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setConditionRegistrationMethods(provider, new ArrayList<>(Arrays.asList(ClientUpdateContextConditionFactory.BY_AUTHENTICATED_USER)));
        });
        registerCondition(CLIENTUPDATECONTEXT_CONDITION_NAME, policyName);
        logger.info("... Registered Condition : " + CLIENTUPDATECONTEXT_CONDITION_NAME);

        createExecutor("SecureClientAuthEnforceExecutor", SecureClientAuthEnforceExecutorFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setExecutorAcceptedClientAuthMethods(provider, new ArrayList<>(Arrays.asList(
                    JWTClientAuthenticator.PROVIDER_ID, JWTClientSecretAuthenticator.PROVIDER_ID, X509ClientAuthenticator.PROVIDER_ID)));
        });
        registerExecutor("SecureClientAuthEnforceExecutor", policyName);
        logger.info("... Registered Executor : SecureClientAuthEnforceExecutor");

    }

    private void setupPolicyAuthzCodeFlowUnderMultiPhasePolicy(String policyName) {

        logger.info("Setup Policy");

        createPolicy(policyName, DefaultClientPolicyProviderFactory.PROVIDER_ID, null, null, null);
        logger.info("... Created Policy : " + policyName);

        createCondition(CLIENTUPDATECONTEXT_CONDITION_NAME, ClientUpdateContextConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setConditionRegistrationMethods(provider, new ArrayList<>(Arrays.asList(ClientUpdateContextConditionFactory.BY_INITIAL_ACCESS_TOKEN)));
        });
        registerCondition(CLIENTUPDATECONTEXT_CONDITION_NAME, policyName);
        logger.info("... Registered Condition : " + CLIENTUPDATECONTEXT_CONDITION_NAME);

        createCondition(CLIENTROLES_CONDITION_NAME, ClientRolesConditionFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setConditionClientRoles(provider, new ArrayList<>(Arrays.asList("sample-client-role")));
        });
        registerCondition(CLIENTROLES_CONDITION_NAME, policyName);
        logger.info("... Registered Condition : " + CLIENTROLES_CONDITION_NAME);

        createExecutor("SecureClientAuthEnforceExecutor", SecureClientAuthEnforceExecutorFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setExecutorAcceptedClientAuthMethods(provider, new ArrayList<>(Arrays.asList(ClientIdAndSecretAuthenticator.PROVIDER_ID, JWTClientAuthenticator.PROVIDER_ID)));
            setExecutorAugmentedClientAuthMethod(provider, ClientIdAndSecretAuthenticator.PROVIDER_ID);
            setExecutorAugmentActivate(provider);
        });
        registerExecutor("SecureClientAuthEnforceExecutor", policyName);
        logger.info("... Registered Executor : SecureClientAuthEnforceExecutor");

        createExecutor("PKCEEnforceExecutor", PKCEEnforceExecutorFactory.PROVIDER_ID, null, (ComponentRepresentation provider) -> {
            setExecutorAugmentActivate(provider);
        });
        registerExecutor("PKCEEnforceExecutor", policyName);
        logger.info("... Registered Executor : PKCEEnforceExecutor");

    }

    private void successfulLoginAndLogout(String clientId, String clientSecret) {
        oauth.clientId(clientId);
        oauth.doLogin("test-user@localhost", "password");

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

        AccessToken refreshedToken = oauth.verifyToken(refreshResponse.getAccessToken());
        RefreshToken refreshedRefreshToken = oauth.parseRefreshToken(refreshResponse.getRefreshToken());
        assertEquals(sessionId, refreshedToken.getSessionState());
        assertEquals(sessionId, refreshedRefreshToken.getSessionState());

        assertEquals(findUserByUsername(adminClient.realm(REALM_NAME), userName).getId(), refreshedToken.getSubject());

        events.expectRefresh(refreshToken.getId(), sessionId).client(clientId).assertEvent();

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
        oauth.doLogin("test-user@localhost", "password");

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
        assertEquals("Missing parameter: nonce", oauth.getCurrentQuery().get(OAuth2Constants.ERROR_DESCRIPTION));
    }

    private String generateS256CodeChallenge(String codeVerifier) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(codeVerifier.getBytes("ISO_8859_1"));
        byte[] digestBytes = md.digest();
        String codeChallenge = Base64Url.encode(digestBytes);
        return codeChallenge;
    }

    private void doIntrospectAccessToken(OAuthClient.AccessTokenResponse tokenRes, String username, String clientId, String clientSecret) throws IOException {
        String tokenResponse = oauth.introspectAccessTokenWithClientCredential(clientId, clientSecret, tokenRes.getAccessToken());
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(tokenResponse);
        assertEquals(true, jsonNode.get("active").asBoolean());
        assertEquals(username, jsonNode.get("username").asText());
        assertEquals(clientId, jsonNode.get("client_id").asText());
        TokenMetadataRepresentation rep = objectMapper.readValue(tokenResponse, TokenMetadataRepresentation.class);
        assertEquals(true, rep.isActive());
        assertEquals(clientId, rep.getClientId());
        assertEquals(clientId, rep.getIssuedFor());
        events.expect(EventType.INTROSPECT_TOKEN).client(clientId).user((String)null).clearDetails().assertEvent();
    }

    private void doTokenRevoke(String refreshToken, String clientId, String clientSecret, String userId, boolean isOfflineAccess) throws IOException {
        oauth.clientId(clientId);
        oauth.doTokenRevoke(refreshToken, "refresh_token", clientSecret);

        // confirm revocation
        OAuthClient.AccessTokenResponse tokenRes = oauth.doRefreshTokenRequest(refreshToken, clientSecret);
        assertEquals(400, tokenRes.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_GRANT, tokenRes.getError());
        if (isOfflineAccess) assertEquals("Offline user session not found", tokenRes.getErrorDescription());
        else assertEquals("Session not active", tokenRes.getErrorDescription());

        events.expect(EventType.REVOKE_GRANT).clearDetails().client(clientId).user(userId).assertEvent();
    }

    private void authCreateClients() {
        reg.auth(Auth.token(getToken("create-clients", "password")));
    }

    private void authManageClients() {
        reg.auth(Auth.token(getToken("manage-clients", "password")));
    }

    private void authNoAccess() {
        reg.auth(Auth.token(getToken("no-access", "password")));
    }

    private String getToken(String username, String password) {
        try {
            return oauth.doGrantAccessTokenRequest(REALM_NAME, username, password, null, Constants.ADMIN_CLI_CLIENT_ID, null).getAccessToken();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ComponentRepresentation createComponentInstance(String name, String providerId, String providerType, String subType) {
        ComponentRepresentation rep = new ComponentRepresentation();
        rep.setId(org.keycloak.models.utils.KeycloakModelUtils.generateId());
        rep.setName(name);
        rep.setParentId(REALM_NAME);
        rep.setProviderId(providerId);
        rep.setProviderType(providerType);
        rep.setSubType(subType);
        rep.setConfig(new MultivaluedHashMap<>());
        return rep;
    }

    private String createComponent(ComponentRepresentation cr) {
        Response resp = adminClient.realm(REALM_NAME).components().add(cr);
        String id = ApiUtil.getCreatedId(resp);
        resp.close();
        // registered components will be removed automatically
        testContext.getOrCreateCleanup(REALM_NAME).addComponentId(id);
        return id;
    }

    private ComponentRepresentation getComponent(String name, String providerType) {
        return adminClient.realm(REALM_NAME).components().query(null, providerType, name).get(0);
    }

    private void updateComponent(ComponentRepresentation cr) {
        adminClient.realm(REALM_NAME).components().component(cr.getId()).update(cr);
    }

    private void deleteComponent(String id) {
        adminClient.realm(REALM_NAME).components().component(id).remove();
    }

    private String createCondition(String name, String providerId, String subType, Consumer<ComponentRepresentation> op) {
        ComponentRepresentation component = createComponentInstance(name, providerId, ClientPolicyConditionProvider.class.getName(), subType);
        op.accept(component);
        return createComponent(component);
    }

    private void registerCondition(String conditionName, String policyName) {
        ComponentRepresentation policy = getPolicy(policyName);
        List<String> conditionIds = policy.getConfig().get(DefaultClientPolicyProviderFactory.CONDITION_IDS);
        if (conditionIds == null) conditionIds = new ArrayList<String>();
        ComponentRepresentation condition = getCondition(conditionName);
        conditionIds.add(condition.getId());
        policy.getConfig().put(DefaultClientPolicyProviderFactory.CONDITION_IDS, conditionIds);
        updatePolicy(policy);
    }

    private ComponentRepresentation getCondition(String name) {
        return getComponent(name, ClientPolicyConditionProvider.class.getName());
    }

    private void updateCondition(String name, Consumer<ComponentRepresentation> op) {
        ComponentRepresentation condition = getCondition(name);
        op.accept(condition);
        updateComponent(condition);
    }

    private void deleteCondition(String conditionName, String policyName) {
        ComponentRepresentation policy = getPolicy(policyName);
        List<String> conditionIds = policy.getConfig().get(DefaultClientPolicyProviderFactory.CONDITION_IDS);
        ComponentRepresentation condition = getCondition(conditionName);
        String conditionId = condition.getId();
        adminClient.realm(REALM_NAME).components().component(conditionId).remove();
        conditionIds.remove(conditionId);
        policy.getConfig().put(DefaultClientPolicyProviderFactory.CONDITION_IDS, conditionIds);
        updatePolicy(policy);
    }

    private String createExecutor(String name, String providerId, String subType, Consumer<ComponentRepresentation> op) {
        ComponentRepresentation component = createComponentInstance(name, providerId, ClientPolicyExecutorProvider.class.getName(), subType);
        op.accept(component);
        return createComponent(component);
    }

    private void registerExecutor(String executorName, String policyName) {
        ComponentRepresentation policy = getPolicy(policyName);
        List<String> executorIds = policy.getConfig().get(DefaultClientPolicyProviderFactory.EXECUTOR_IDS);
        if (executorIds == null) executorIds = new ArrayList<String>();
        ComponentRepresentation executor = getExecutor(executorName);
        executorIds.add(executor.getId());
        policy.getConfig().put(DefaultClientPolicyProviderFactory.EXECUTOR_IDS, executorIds);
        updatePolicy(policy);
    }

    private ComponentRepresentation getExecutor(String name) {
        return getComponent(name, ClientPolicyExecutorProvider.class.getName());
    }

    private void updateExecutor(String name, Consumer<ComponentRepresentation> op) {
        ComponentRepresentation executor = getExecutor(name);
        op.accept(executor);
        updateComponent(executor);
    }

    private void deleteExecutor(String executorName, String policyName) {
        ComponentRepresentation policy = getPolicy(policyName);
        List<String> executorIds = policy.getConfig().get(DefaultClientPolicyProviderFactory.EXECUTOR_IDS);
        ComponentRepresentation executor = getExecutor(executorName);
        String executorId = executor.getId();
        adminClient.realm(REALM_NAME).components().component(executorId).remove();
        executorIds.remove(executorId);
        policy.getConfig().put(DefaultClientPolicyProviderFactory.EXECUTOR_IDS, executorIds);
        updatePolicy(policy);
    }

    private String createPolicy(String name, String providerId, String subType, List<String> conditions, List<String> executors) {
        ComponentRepresentation component = createComponentInstance(name, providerId, ClientPolicyProvider.class.getName(), subType);
        component.getConfig().put(DefaultClientPolicyProviderFactory.CONDITION_IDS, conditions);
        component.getConfig().put(DefaultClientPolicyProviderFactory.EXECUTOR_IDS, executors);
        return createComponent(component);
    }

    private ComponentRepresentation getPolicy(String name) {
        return getComponent(name, ClientPolicyProvider.class.getName());
    }

    private void updatePolicy(ComponentRepresentation policy) {
        updateComponent(policy);
    }

    private void deletePolicy(String policyName) {
        ComponentRepresentation policy = getPolicy(policyName);
        List<String> conditionIds = policy.getConfig().get(DefaultClientPolicyProviderFactory.CONDITION_IDS);
        List<String> executorIds = policy.getConfig().get(DefaultClientPolicyProviderFactory.EXECUTOR_IDS);
        conditionIds.stream().forEach(i->adminClient.realm(REALM_NAME).components().component(i).remove());
        executorIds.stream().forEach(i->adminClient.realm(REALM_NAME).components().component(i).remove());
        adminClient.realm(REALM_NAME).components().component(policy.getId()).remove();
    }

    private String createClientByAdmin(String clientName, Consumer<ClientRepresentation> op) throws ClientPolicyException {
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId(clientName);
        clientRep.setName(clientName);
        clientRep.setProtocol("openid-connect");
        clientRep.setBearerOnly(Boolean.FALSE);
        clientRep.setPublicClient(Boolean.FALSE);
        clientRep.setServiceAccountsEnabled(Boolean.TRUE);
        clientRep.setRedirectUris(Collections.singletonList(ServerURLs.getAuthServerContextRoot() + "/auth/realms/master/app/auth"));
        op.accept(clientRep);
        Response resp = adminClient.realm(REALM_NAME).clients().create(clientRep);
        if (resp.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()) {
            throw new ClientPolicyException(Errors.INVALID_REGISTRATION, "registration error by admin");
        }
        resp.close();
        assertEquals(Response.Status.CREATED.getStatusCode(), resp.getStatus());
        return ApiUtil.getCreatedId(resp);
    }

    private ClientRepresentation getClientByAdmin(String clientId) {
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(clientId);
        return clientResource.toRepresentation();
    }

    private void updateClientByAdmin(String clientId, Consumer<ClientRepresentation> op) {
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(clientId);
        ClientRepresentation clientRep = clientResource.toRepresentation();
        op.accept(clientRep);
        clientResource.update(clientRep);
    }

    private void deleteClientByAdmin(String clientId) {
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(clientId);
        clientResource.remove();
    }

    private String createClientDynamically(String clientName, Consumer<OIDCClientRepresentation> op) throws ClientRegistrationException {
        OIDCClientRepresentation clientRep = new OIDCClientRepresentation();
        clientRep.setClientName(clientName);
        clientRep.setClientUri(ServerURLs.getAuthServerContextRoot());
        clientRep.setRedirectUris(Collections.singletonList(ServerURLs.getAuthServerContextRoot() + "/auth/realms/master/app/auth"));
        op.accept(clientRep);
        OIDCClientRepresentation response = reg.oidc().create(clientRep);
        reg.auth(Auth.token(response));
        return response.getClientId();
    }

    private OIDCClientRepresentation getClientDynamically(String clientId) throws ClientRegistrationException {
        return reg.oidc().get(clientId);
    }

    private void updateClientDynamically(String clientId, Consumer<OIDCClientRepresentation> op) throws ClientRegistrationException {
        OIDCClientRepresentation clientRep = reg.oidc().get(clientId);
        op.accept(clientRep);
        OIDCClientRepresentation response = reg.oidc().update(clientRep);
        reg.auth(Auth.token(response));
    }

    private void deleteClientDynamically(String clientId) throws ClientRegistrationException {
        reg.oidc().delete(clientId);
    }

    private void setConditionRegistrationMethods(ComponentRepresentation provider, List<String> registrationMethods) {
        provider.getConfig().put(ClientUpdateContextConditionFactory.UPDATE_CLIENT_SOURCE, registrationMethods);
    }

    private void setConditionClientRoles(ComponentRepresentation provider, List<String> clientRoles) {
        provider.getConfig().put(ClientRolesConditionFactory.ROLES, clientRoles);
    }

    private void setConditionClientScopes(ComponentRepresentation provider, List<String> clientScopes) {
        provider.getConfig().put(ClientScopesConditionFactory.SCOPES, clientScopes);
    }

    private void setConditionClientAccessType(ComponentRepresentation provider, List<String> clientAccessTypes) {
        provider.getConfig().put(ClientAccessTypeConditionFactory.TYPE, clientAccessTypes);
    }

    private void setConditionClientUpdateSourceHosts(ComponentRepresentation provider, List<String> hosts) {
        provider.getConfig().putSingle(ClientUpdateSourceHostsConditionFactory.HOST_SENDING_REQUEST_MUST_MATCH, "true");
        provider.getConfig().put(ClientUpdateSourceHostsConditionFactory.TRUSTED_HOSTS, hosts);
    }

    private void setConditionClientUpdateSourceGroups(ComponentRepresentation provider, List<String> groups) {
        provider.getConfig().put(ClientUpdateSourceGroupsConditionFactory.GROUPS, groups);
    }

    private void setConditionUpdatingClientSourceRoles(ComponentRepresentation provider, List<String> groups) {
        provider.getConfig().put(ClientUpdateSourceRolesConditionFactory.ROLES, groups);
    }

    private void setExecutorAugmentActivate(ComponentRepresentation provider) {
        provider.getConfig().putSingle("is-augment", Boolean.TRUE.toString());
    }

    private void setExecutorAugmentDeactivate(ComponentRepresentation provider) {
        provider.getConfig().putSingle("is-augment", Boolean.FALSE.toString());
    }

    private void setExecutorAcceptedClientAuthMethods(ComponentRepresentation provider, List<String> acceptedClientAuthMethods) {
        provider.getConfig().put(SecureClientAuthEnforceExecutorFactory.CLIENT_AUTHNS, acceptedClientAuthMethods);
    }

    private void setExecutorAugmentedClientAuthMethod(ComponentRepresentation provider, String augmentedClientAuthMethod) {
        provider.getConfig().putSingle(SecureClientAuthEnforceExecutorFactory.CLIENT_AUTHNS_AUGMENT, augmentedClientAuthMethod);
    }
}

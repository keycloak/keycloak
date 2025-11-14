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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.keycloak.OAuthErrorException;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.events.Errors;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.executor.SecureRedirectUrisEnforcerExecutorFactory;
import org.keycloak.testsuite.util.ClientPoliciesUtil;
import org.keycloak.testsuite.util.ServerURLs;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

import org.junit.Assert;
import org.junit.Test;

import static org.keycloak.testsuite.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createAnyClientConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createSecureRedirectUrisEnforcerExecutorConfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SecureRedirectUrisEnforcerExecutorTest extends AbstractClientPoliciesTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        testRealms.add(realm);
    }

    @Test
    public void testNotRedirectBasedFlowClient_normalUri() throws Exception {
        // register profiles
        String json = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                (new ClientPoliciesUtil.ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(SecureRedirectUrisEnforcerExecutorFactory.PROVIDER_ID,
                                createSecureRedirectUrisEnforcerExecutorConfig(it->{}))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesUtil.ClientPoliciesBuilder()).addPolicy(
                (new ClientPoliciesUtil.ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID,
                                createAnyClientConditionConfig())
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // The executor's check logic is not executed to an auth code flow or implicit flow disabled client.

        // Registration
        // Success - even if not setting a valid redirect uri
        String clientId;
        String cId = null;
        try {
            clientId = generateSuffixedName(CLIENT_NAME);
            cId = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
                clientRep.setSecret("secret");
                clientRep.setRedirectUris(List.of("http://oauth.redirect/some")); // normally, a redirect url with http scheme is not allowed.
                clientRep.setStandardFlowEnabled(false);
                clientRep.setImplicitFlowEnabled(false);
                clientRep.setServiceAccountsEnabled(true);
            });
            ClientRepresentation cRep = getClientByAdmin(cId);
            assertEquals(new HashSet<>(List.of("http://oauth.redirect/some")), new HashSet<>(cRep.getRedirectUris()));
        } catch (ClientPolicyException cpe) {
            fail();
        }

        // Update
        // Success - even if not setting a valid redirect uri
        try {
            updateClientByAdmin(cId, (ClientRepresentation clientRep) -> {
                clientRep.setAttributes(new HashMap<>());
                clientRep.setRedirectUris(List.of("")); // nomally, vacant redirect uri is not allowed.
            });
            ClientRepresentation cRep = getClientByAdmin(cId);
            assertEquals(new HashSet<>(List.of("")), new HashSet<>(cRep.getRedirectUris()));
        } catch (ClientPolicyException cpe) {
            fail();
        }
    }

    @Test
    public void testSecureRedirectUrisEnforcerExecutor_normalUri() throws Exception {
        // register profiles
        String json = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                (new ClientPoliciesUtil.ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(SecureRedirectUrisEnforcerExecutorFactory.PROVIDER_ID,
                                createSecureRedirectUrisEnforcerExecutorConfig(it->{}))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesUtil.ClientPoliciesBuilder()).addPolicy(
                (new ClientPoliciesUtil.ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID,
                                createAnyClientConditionConfig())
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // register - fail
        // no redirect uri is not allowed
        testSecureRedirectUrisEnforcerExecutor_failRegisterByAdmin(List.of(""));

        // register - fail
        // HTTP scheme not allowed
        testSecureRedirectUrisEnforcerExecutor_failRegisterDynamically(List.of("http://app.example.com:51004/oauth2redirect/example-provider"));

        // register - success
        List<String> registerResultList = testSecureRedirectUrisEnforcerExecutor_successRegisterByAdmin(
                Arrays.asList("https://app.example.com:51004/oauth2redirect/example-provider", "https://dev.example.com/redirect"));
        String alphaClientId = registerResultList.get(0);
        String alphaCid = registerResultList.get(1);

        // update - fail
        // IPv4 loopback address not allowed
        testSecureRedirectUrisEnforcerExecutor_failUpdateByAdmin(alphaCid,
                Arrays.asList("https://127.0.0.1:8443", "https://app.example.com:51004/oauth2redirect/example-provider"));

        // update - fail
        // wildcard context path not allowed
        testSecureRedirectUrisEnforcerExecutor_failUpdateDynamically(alphaClientId,
                List.of("https://dev.example.com:8443/*"));

        // update - success
        testSecureRedirectUrisEnforcerExecutor_successUpdateByAdmin(alphaCid,
                Arrays.asList("https://app.example.com:51004/oauth2redirect/example-provider/update", "https://dev.example.com/redirect/update"));

        // authorization request - fail
        // redirect_uri not matched with registered redirect uris
        testSecureRedirectUrisEnforcerExecutor_failAuthorizationRequest(alphaClientId, "https://app.example.com:51004/oauth2redirect/example-provider");
    }

    @Test
    public void testSecureRedirectUrisEnforcerExecutor_IPv4LoopbackAddress() throws Exception {
        // register profiles
        String json = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                (new ClientPoliciesUtil.ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(SecureRedirectUrisEnforcerExecutorFactory.PROVIDER_ID,
                                createSecureRedirectUrisEnforcerExecutorConfig(it->
                                    it.setAllowIPv4LoopbackAddress(true)))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesUtil.ClientPoliciesBuilder()).addPolicy(
                (new ClientPoliciesUtil.ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID,
                                createAnyClientConditionConfig())
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // register - fail
        // IPv6 loopback address not allowed
        testSecureRedirectUrisEnforcerExecutor_failRegisterDynamically(Arrays.asList("https://127.0.0.1/oauth2redirect/example-provider",
                "http://[::1]/oauth2redirect/example-provider"));

        // register - success
        List<String> registerResultList = testSecureRedirectUrisEnforcerExecutor_successRegisterByAdmin(
                Arrays.asList("https://app.example.com:51004/oauth2redirect/example-provider",
                        "https://127.0.0.1/oauth2redirect/example-provider"));
        String alphaClientId = registerResultList.get(0);
        String alphaCid = registerResultList.get(1);

        // update - fail
        // private use uri scheme not allowed
        testSecureRedirectUrisEnforcerExecutor_failUpdateByAdmin(alphaCid, List.of("com.example.app:/oauth2redirect/example-provider"));

        // update - success
        testSecureRedirectUrisEnforcerExecutor_successUpdateByAdmin(alphaCid,
                List.of("/auth/realms/master/app/auth", "https://dev.example.com/redirect/update"));

        // authorization request - fail
        // invalid uri form
        testSecureRedirectUrisEnforcerExecutor_failAuthorizationRequest(alphaClientId, "https://keycloak.org\n");

        // authorization request - success
        testSecureRedirectUrisEnforcerExecutor_successAuthorizationRequest(alphaClientId, ServerURLs.getAuthServerContextRoot() + "/auth/realms/master/app/auth");
    }

    @Test
    public void testSecureRedirectUrisEnforcerExecutor_IPv6LoopbackAddress() throws Exception {
        // register profiles
        String json = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                (new ClientPoliciesUtil.ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(SecureRedirectUrisEnforcerExecutorFactory.PROVIDER_ID,
                                createSecureRedirectUrisEnforcerExecutorConfig(it-> {
                                    it.setOAuth2_1Compliant(true);
                                    it.setAllowIPv6LoopbackAddress(true);
                                })
                        )
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesUtil.ClientPoliciesBuilder()).addPolicy(
                (new ClientPoliciesUtil.ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID,
                                createAnyClientConditionConfig())
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // register - fail
        // IPv4 loopback address not allowed
        testSecureRedirectUrisEnforcerExecutor_failRegisterDynamically(Arrays.asList("https://[::1]/", "https://127.0.0.1/auth/admin"));

        // register - fail
        // IPv4 loopback address not allowed (even when "localhost" is used)
        testSecureRedirectUrisEnforcerExecutor_failRegisterDynamically(Arrays.asList("https://[::1]/", "https://localhost/auth/admin"));

        // register - success
        List<String> registerResultList = testSecureRedirectUrisEnforcerExecutor_successRegisterByAdmin(
                Arrays.asList("https://[::1]/oauth2redirect/example-provider", "https://[::1]/"));
        String alphaClientId = registerResultList.get(0);
        String alphaCid = registerResultList.get(1);

        // update - fail
        // representation of IPv6 loopback address [0:0:0:0:0:0:0:1] not allowed with OAuth 2.1 mode enabled
        testSecureRedirectUrisEnforcerExecutor_failUpdateByAdmin(alphaCid, List.of("https://[0:0:0:0:0:0:0:1]/oauth2redirect/example-provider"));

        // update - success
        testSecureRedirectUrisEnforcerExecutor_successUpdateByAdmin(alphaCid,
                List.of("https://[::1]/oauth2redirect/example-provider/update", "https://dev.example.com/redirect/update"));

        // authorization request - fail
        // redirect_uri parameter not match with registered redirect uris
        testSecureRedirectUrisEnforcerExecutor_failAuthorizationRequest(alphaClientId, "http://[::1]:65522/oauth2redirect/example-provider");
    }

    @Test
    public void testSecureRedirectUrisEnforcerExecutor_PrivateUseUriScheme() throws Exception {
        // register profiles
        String json = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                (new ClientPoliciesUtil.ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(SecureRedirectUrisEnforcerExecutorFactory.PROVIDER_ID,
                                createSecureRedirectUrisEnforcerExecutorConfig(it->
                                    it.setAllowPrivateUseUriScheme(true)))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesUtil.ClientPoliciesBuilder()).addPolicy(
                (new ClientPoliciesUtil.ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID,
                                createAnyClientConditionConfig())
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // register - fail
        // invalid uri form
        testSecureRedirectUrisEnforcerExecutor_failRegisterByAdmin(List.of("com.example:"));

        // register - success
        List<String> registerResultList = testSecureRedirectUrisEnforcerExecutor_successRegisterByAdmin(
                List.of("com.example.app:/oauth2redirect/example-provider",
                        "dev.com.example.app:/oauth2redirect/example-provider/dev"));
        String alphaClientId = registerResultList.get(0);
        String alphaCid = registerResultList.get(1);

        // update - fail
        // HTTP scheme not allowed
        testSecureRedirectUrisEnforcerExecutor_failUpdateDynamically(alphaClientId,
                Arrays.asList("com.example.app:/oauth2redirect/example-provider", "http://dev.example.com/redirect"));

        // update - success
        testSecureRedirectUrisEnforcerExecutor_successUpdateByAdmin(alphaCid,
                Arrays.asList("com.example.app:/oauth2redirect/example-provider/update", "https://dev.example.com/redirect/update"));

        // authorization request - fail
        // redirect_uri not match with registered redirect uris
        testSecureRedirectUrisEnforcerExecutor_failAuthorizationRequest(alphaClientId, "com.example.app:/oauth2redirect/example-provider");
    }

    @Test
    public void testSecureRedirectUrisEnforcerExecutor_AllowHttpScheme() throws Exception {
        // register profiles
        String json = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                (new ClientPoliciesUtil.ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(SecureRedirectUrisEnforcerExecutorFactory.PROVIDER_ID,
                                createSecureRedirectUrisEnforcerExecutorConfig(it->{
                                    it.setAllowIPv4LoopbackAddress(true);
                                    it.setAllowIPv6LoopbackAddress(true);
                                    it.setAllowHttpScheme(true);}))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesUtil.ClientPoliciesBuilder()).addPolicy(
                (new ClientPoliciesUtil.ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID,
                                createAnyClientConditionConfig())
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // register - success
        List<String> registerResultList = testSecureRedirectUrisEnforcerExecutor_successRegisterByAdmin(
                Arrays.asList("http://localhost:8080/redirect", "http://dev.example.com/redirect/update"));
        String alphaClientId = registerResultList.get(0);
        String alphaCid = registerResultList.get(1);

        // update - success
        testSecureRedirectUrisEnforcerExecutor_successUpdateByAdmin(alphaCid,
                Arrays.asList("http://[::1]:8080/redirect", ServerURLs.getAuthServerContextRoot() + "/auth/realms/master/app/auth"));

        // authorization request - fail
        // redirect_uri not match with registered redirect uris
        testSecureRedirectUrisEnforcerExecutor_failAuthorizationRequest(alphaClientId, "http://[::1]:8080/");

        // authorization request - success
        testSecureRedirectUrisEnforcerExecutor_successAuthorizationRequest(alphaClientId, ServerURLs.getAuthServerContextRoot() + "/auth/realms/master/app/auth");
    }

    @Test
    public void testSecureRedirectUrisEnforcerExecutor_AllowWildcardContextPath() throws Exception {
        // register profiles
        String json = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                (new ClientPoliciesUtil.ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(SecureRedirectUrisEnforcerExecutorFactory.PROVIDER_ID,
                                createSecureRedirectUrisEnforcerExecutorConfig(it->{
                                    it.setAllowPrivateUseUriScheme(true);
                                    it.setAllowIPv4LoopbackAddress(true);
                                    it.setAllowIPv6LoopbackAddress(true);
                                    it.setAllowHttpScheme(true);
                                    it.setAllowWildcardContextPath(true);}))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesUtil.ClientPoliciesBuilder()).addPolicy(
                (new ClientPoliciesUtil.ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID,
                                createAnyClientConditionConfig())
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // register - success
        List<String> registerResultList = testSecureRedirectUrisEnforcerExecutor_successRegisterByAdmin(
                Arrays.asList("http://localhost:8080/*", "http://dev.example.com/redirect/update"));
        String alphaClientId = registerResultList.get(0);
        String alphaCid = registerResultList.get(1);

        // update - success
        testSecureRedirectUrisEnforcerExecutor_successUpdateByAdmin(alphaCid,
                Arrays.asList("http://[::1]:8080/*", ServerURLs.getAuthServerContextRoot() + "/*"));

        // authorization request - fail
        // redirect_uri not match with registered redirect uris
        testSecureRedirectUrisEnforcerExecutor_failAuthorizationRequest(alphaClientId, "com.example.app:/oauth2redirect/example-provider");

        // authorization request - success
        testSecureRedirectUrisEnforcerExecutor_successAuthorizationRequest(alphaClientId, ServerURLs.getAuthServerContextRoot() + "/auth/realms/master/app/auth");
    }

    @Test
    public void testSecureRedirectUrisEnforcerExecutor_AllowPermittedDomains() throws Exception {
        // register profiles
        String json = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                (new ClientPoliciesUtil.ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(SecureRedirectUrisEnforcerExecutorFactory.PROVIDER_ID,
                                createSecureRedirectUrisEnforcerExecutorConfig(it->{
                                    it.setAllowIPv4LoopbackAddress(true);
                                    it.setAllowPermittedDomains(Arrays.asList(
                                            "oauth.redirect", "((dev|test)-)*example.org", "localhost"));
                                    it.setAllowHttpScheme(true);
                                    it.setAllowWildcardContextPath(true);}))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesUtil.ClientPoliciesBuilder()).addPolicy(
                (new ClientPoliciesUtil.ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID,
                                createAnyClientConditionConfig())
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // register - fail
        // not match permitted domains
        testSecureRedirectUrisEnforcerExecutor_failRegisterByAdmin(Arrays.asList("http://oauth.redirect/*", "http://dev.example.org/redirect"));

        // register - success
        List<String> registerResultList = testSecureRedirectUrisEnforcerExecutor_successRegisterByAdmin(
                Arrays.asList("http://oauth.redirect/*", "http://dev-example.org/redirect"));
        String alphaClientId = registerResultList.get(0);
        String alphaCid = registerResultList.get(1);

        // update - fail
        // not match permitted domains
        testSecureRedirectUrisEnforcerExecutor_failUpdateDynamically(alphaClientId,
                Arrays.asList("http://dev.oauth.redirect/*", "http://test-example.com/redirect"));

        // update using rootUrl - fail
        try {
            updateClientByAdmin(alphaCid, (ClientRepresentation clientRep) -> {
                clientRep.setAttributes(new HashMap<>());
                clientRep.setRootUrl("http://incorrect.org");
                clientRep.setRedirectUris(List.of("/redirect"));
            });
            fail("Expected to fail when updating clientId " + alphaCid + " with redirectUris: '/redirect'");
        } catch (ClientPolicyException cpe) {
            assertEquals(Errors.INVALID_REQUEST, cpe.getError());
        }

        // update using rootUrl - success
        updateClientByAdmin(alphaCid, (ClientRepresentation clientRep) -> {
            clientRep.setAttributes(new HashMap<>());
            clientRep.setRootUrl("http://dev-example.org");
            clientRep.setRedirectUris(List.of("/redirect"));
        });
        ClientRepresentation cRep = getClientByAdmin(alphaCid);
        assertEquals(List.of("/redirect"), cRep.getRedirectUris());

        // update - success
        testSecureRedirectUrisEnforcerExecutor_successUpdateByAdmin(alphaCid,
                Arrays.asList("http://oauth.redirect/*", "http://dev-example.org/redirect", ServerURLs.getAuthServerContextRoot() + "/*"));

        // authorization request - fail
        // redirect_uri not match with registered redirect uris
        testSecureRedirectUrisEnforcerExecutor_failAuthorizationRequest(alphaClientId, "http://dev-example.org/v2/redirect");

        // authorization request - success
        testSecureRedirectUrisEnforcerExecutor_successAuthorizationRequest(alphaClientId, ServerURLs.getAuthServerContextRoot() + "/auth/realms/master/app/auth");
    }

    @Test
    public void testSecureRedirectUrisEnforcerExecutor_OAuth2_1Complient() throws Exception {
        // register profiles
        String json = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                (new ClientPoliciesUtil.ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(SecureRedirectUrisEnforcerExecutorFactory.PROVIDER_ID,
                                createSecureRedirectUrisEnforcerExecutorConfig(it->{
                                    it.setAllowPrivateUseUriScheme(true);
                                    it.setAllowIPv4LoopbackAddress(true);
                                    it.setAllowIPv6LoopbackAddress(true);
                                    it.setAllowHttpScheme(true);
                                    it.setOAuth2_1Compliant(true);}))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesUtil.ClientPoliciesBuilder()).addPolicy(
                (new ClientPoliciesUtil.ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID,
                                createAnyClientConditionConfig())
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // register - fail
        // IPv4 loopback address with port number not allowed
        testSecureRedirectUrisEnforcerExecutor_failRegisterByAdmin(Arrays.asList("http://127.0.0.1/auth/admin", "http://127.0.0.1:8080/auth/admin"));

        // register - success
        List<String> registerResultList = testSecureRedirectUrisEnforcerExecutor_successRegisterByAdmin(List.of("https://127.0.0.1/auth/admin"));
        String alphaClientId = registerResultList.get(0);
        String alphaCid = registerResultList.get(1);

        // update - fail
        // HTTP scheme not allowed
        testSecureRedirectUrisEnforcerExecutor_failUpdateDynamically(alphaClientId,
                List.of("https://127.0.0.1/auth/admin", "http://test-example.com/redirect"));

        // update - success
        testSecureRedirectUrisEnforcerExecutor_successUpdateByAdmin(alphaCid,
                List.of("https://[::1]/auth/admin", "com.example.app:/oauth2redirect/example-provider"));

        // authorization request - fail
        // redirect_uri not match with registered redirect uris
        testSecureRedirectUrisEnforcerExecutor_failAuthorizationRequest(alphaClientId, "com.example.app:/oauth3redirect/example-provider");
    }

    @Test
    public void testSecureRedirectUrisEnforcerExecutor_AllowOpenRedirect() throws Exception {
        // Allow open redirect
        String json = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                (new ClientPoliciesUtil.ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(SecureRedirectUrisEnforcerExecutorFactory.PROVIDER_ID,
                                createSecureRedirectUrisEnforcerExecutorConfig(it->{
                                    it.setAllowOpenRedirect(true);
                                    it.setOAuth2_1Compliant(true);}))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesUtil.ClientPoliciesBuilder()).addPolicy(
                (new ClientPoliciesUtil.ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID,
                                createAnyClientConditionConfig())
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // register - success
        // open redirect is allowed in any running mode
        testSecureRedirectUrisEnforcerExecutor_successRegisterByAdmin(List.of(""));
    }

    @Test
    public void testSecureRedirectUrisEnforcerExecutor_postLogoutRedirectUris() throws Exception {
        // register profiles
        String json = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                (new ClientPoliciesUtil.ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(SecureRedirectUrisEnforcerExecutorFactory.PROVIDER_ID,
                                createSecureRedirectUrisEnforcerExecutorConfig(it-> it.setAllowPermittedDomains(List.of("oauth.redirect")))
                        )
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesUtil.ClientPoliciesBuilder()).addPolicy(
                (new ClientPoliciesUtil.ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID,
                                createAnyClientConditionConfig())
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // Success - register without post-logout redirect uris
        String clientId = testSecureRedirectUrisEnforcerExecutor_successRegisterDynamically(List.of("https://oauth.redirect/something"));

        // Success - update with post-logout redirect uris as "+"
        updateClientDynamically(clientId, (OIDCClientRepresentation clientRep) -> {
                clientRep.setRedirectUris(List.of("https://oauth.redirect/some"));
                clientRep.setPostLogoutRedirectUris(List.of("+"));
        });
        OIDCClientRepresentation clientRepp = reg.oidc().get(clientId);
        Assert.assertEquals(List.of("https://oauth.redirect/some"), clientRepp.getRedirectUris());
        Assert.assertEquals(List.of("https://oauth.redirect/some"), clientRepp.getPostLogoutRedirectUris());

        // Fail - incorrect domain for post-logout redirect uri
        try {
            updateClientDynamically(clientId, (OIDCClientRepresentation clientRep) -> {
                clientRep.setRedirectUris(List.of("https://oauth.redirect/some"));
                clientRep.setPostLogoutRedirectUris(List.of("https://incorrect.domain/some"));
            });
            fail();
        } catch (ClientRegistrationException e) {
            assertEquals(ERR_MSG_CLIENT_REG_FAIL, e.getMessage());
        }

        // Success - update with post-logout redirect uris as "+"
        updateClientDynamically(clientId, (OIDCClientRepresentation clientRep) -> {
            clientRep.setRedirectUris(List.of("https://oauth.redirect/some"));
            clientRep.setPostLogoutRedirectUris(List.of("https://oauth.redirect/some-post-logout"));
        });
        clientRepp = reg.oidc().get(clientId);
        Assert.assertEquals(List.of("https://oauth.redirect/some"), clientRepp.getRedirectUris());
        Assert.assertEquals(List.of("https://oauth.redirect/some-post-logout"), clientRepp.getPostLogoutRedirectUris());
    }


    private void testSecureRedirectUrisEnforcerExecutor_failRegisterByAdmin(List<String> redirectUrisList) {
        try {
            createClientByAdmin(generateSuffixedName(CLIENT_NAME), (ClientRepresentation clientRep) -> {
                clientRep.setSecret("secret");
                clientRep.setRedirectUris(redirectUrisList);
            });
            fail();
        } catch (ClientPolicyException cpe) {
            assertEquals(OAuthErrorException.INVALID_REQUEST, cpe.getError());
        }
    }

    private void testSecureRedirectUrisEnforcerExecutor_failRegisterDynamically(List<String> redirectUrisList) {
        try {
            createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) ->
                clientRep.setRedirectUris(redirectUrisList));
            fail("Expected to fail with redirectUris: " +redirectUrisList);
        } catch (ClientRegistrationException cre) {
            assertEquals(ERR_MSG_CLIENT_REG_FAIL, cre.getMessage());
        }
    }

    // First item in the list is clientId. Second item is clientUUID (DB entity ID)
    private List<String> testSecureRedirectUrisEnforcerExecutor_successRegisterByAdmin(List<String> redirectUrisList) {
        String alphaClientId = null;
        String alphaCid = null;
        try {
            alphaClientId = generateSuffixedName(CLIENT_NAME);
            alphaCid = createClientByAdmin(alphaClientId, (ClientRepresentation clientRep) -> {
                clientRep.setSecret("secret");
                clientRep.setRedirectUris(redirectUrisList);
            });
            ClientRepresentation cRep = getClientByAdmin(alphaCid);
            assertEquals(new HashSet<>(redirectUrisList), new HashSet<>(cRep.getRedirectUris()));
        } catch (ClientPolicyException cpe) {
            fail();
        }
        return Arrays.asList(alphaClientId, alphaCid);
    }

    // Return clientId (not DB UUID)
    private String testSecureRedirectUrisEnforcerExecutor_successRegisterDynamically(List<String> redirectUrisList) {
        try {
            return createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) ->
                    clientRep.setRedirectUris(redirectUrisList));
        } catch (ClientRegistrationException cre) {
            fail("Did not expected to fail when dynamically registering client with redirectUris: " + redirectUrisList);
            // Should not be here
            return null;
        }
    }

    private void testSecureRedirectUrisEnforcerExecutor_failUpdateByAdmin(String cId, List<String> redirectUrisList) {
        try {
            updateClientByAdmin(cId, (ClientRepresentation clientRep) -> {
                clientRep.setAttributes(new HashMap<>());
                clientRep.setRedirectUris(redirectUrisList);
            });
            fail("Expected to failwhen updating clientId " + cId + " with redirectUris: " +redirectUrisList);
        } catch (ClientPolicyException cpe) {
            assertEquals(Errors.INVALID_REQUEST, cpe.getError());
        }
    }

    private void testSecureRedirectUrisEnforcerExecutor_failUpdateDynamically(String clientId, List<String> redirectUrisList) {
        try {
            updateClientDynamically(clientId, (OIDCClientRepresentation clientRep) ->
                clientRep.setRedirectUris(redirectUrisList));
            fail();
        } catch (ClientRegistrationException e) {
            assertEquals(ERR_MSG_CLIENT_REG_FAIL, e.getMessage());
        }
    }

    private void testSecureRedirectUrisEnforcerExecutor_successUpdateByAdmin(String cId, List<String> redirectUrisList) {
        try {
            updateClientByAdmin(cId, (ClientRepresentation clientRep) -> {
                clientRep.setAttributes(new HashMap<>());
                clientRep.setRedirectUris(redirectUrisList);
            });
            ClientRepresentation cRep = getClientByAdmin(cId);
            assertEquals(new HashSet<>(redirectUrisList), new HashSet<>(cRep.getRedirectUris()));
        } catch (ClientPolicyException cpe) {
            fail();
        }
    }

    private void testSecureRedirectUrisEnforcerExecutor_failAuthorizationRequest(String clientId, String redirectUri) {
        oauth.client(clientId);
        oauth.redirectUri(redirectUri);
        oauth.openLoginForm();
        assertTrue(errorPage.isCurrent());
    }

    private void testSecureRedirectUrisEnforcerExecutor_successAuthorizationRequest(String clientId, String redirectUri) {
        oauth.client(clientId, "secret");
        oauth.redirectUri(redirectUri);
        AuthorizationEndpointResponse response = oauth.doLogin("test-user@localhost", "password");
        Assert.assertNotNull(response.getCode());
        AccessTokenResponse res = oauth.doAccessTokenRequest(response.getCode());
        assertEquals(200, res.getStatusCode());
        oauth.doLogout(res.getRefreshToken());
    }
}

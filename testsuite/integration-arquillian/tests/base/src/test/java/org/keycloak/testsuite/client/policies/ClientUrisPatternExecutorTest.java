/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

import org.keycloak.OAuthErrorException;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.models.CibaConfig;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.executor.SecureClientUrisPatternExecutor;
import org.keycloak.services.clientpolicy.executor.SecureClientUrisPatternExecutorFactory;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy;
import org.keycloak.services.clientregistration.policy.impl.TrustedHostClientRegistrationPolicyFactory;
import org.keycloak.testsuite.client.resources.TestApplicationResourceUrls;
import org.keycloak.testsuite.client.resources.TestOIDCEndpointsApplicationResource;
import org.keycloak.testsuite.util.ClientPoliciesUtil;

import org.junit.Test;

import static org.keycloak.testsuite.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.AbstractTestRealmKeycloakTest.TEST_REALM_NAME;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createAnyClientConditionConfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ClientUrisPatternExecutorTest extends AbstractClientPoliciesTest {

    private static final String SAFE_PATTERN = "^https://trusted\\.com.*";

    private static final String VALID_URL = "https://trusted.com/callback";
    private static final String INVALID_URL = "http://untrusted.com/callback";

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        testRealms.add(realm);
    }

    @Test
    public void testFieldsByAdmin() throws Exception {
        testFieldByAdmin("rootUrl", ClientRepresentation::setRootUrl);
        testFieldByAdmin("adminUrl", ClientRepresentation::setAdminUrl);
        testFieldByAdmin("baseUrl", ClientRepresentation::setBaseUrl);
        testFieldByAdmin("redirectUris", (c, val) -> c.setRedirectUris(Collections.singletonList(val)));
        testFieldByAdmin("webOrigins", (c, val) -> c.setWebOrigins(Collections.singletonList(val)));

        testFieldByAdmin("jwksUri", (c, val) -> c.getAttributes().put(OIDCConfigAttributes.JWKS_URL, val));
        testFieldByAdmin("requestUris", (c, val) -> c.getAttributes().put(OIDCConfigAttributes.REQUEST_URIS, val));
        testFieldByAdmin("backchannelLogoutUrl", (c, val) -> c.getAttributes().put(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL, val));
        testFieldByAdmin("postLogoutRedirectUris", (c, val) -> c.getAttributes().put(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, val));
        testFieldByAdmin("cibaClientNotificationEndpoint", (c, val) -> c.getAttributes().put(CibaConfig.CIBA_BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT, val));
        testFieldByAdmin(OIDCConfigAttributes.LOGO_URI, (c, val) -> c.getAttributes().put(OIDCConfigAttributes.LOGO_URI, val));
        testFieldByAdmin(OIDCConfigAttributes.POLICY_URI, (c, val) -> c.getAttributes().put(OIDCConfigAttributes.POLICY_URI, val));
        testFieldByAdmin(OIDCConfigAttributes.TOS_URI, (c, val) -> c.getAttributes().put(OIDCConfigAttributes.TOS_URI, val));
    }

    @Test
    public void testFieldsDynamically() throws Exception {
        //remove trust registration policy
        List<ComponentRepresentation> components = realmsResouce().realm(TEST_REALM_NAME).components().query(null, ClientRegistrationPolicy.class.getCanonicalName()).stream().filter(c -> c.getProviderId().equals(TrustedHostClientRegistrationPolicyFactory.PROVIDER_ID)).toList();
        for (ComponentRepresentation component : components) {
            realmsResouce().realm(TEST_REALM_NAME).components().removeComponent(component.getId());
        }

        testFieldDynamically("baseUrl", OIDCClientRepresentation::setClientUri);
        testFieldDynamically("redirectUris", (c, val) -> c.setRedirectUris(Collections.singletonList(val)));

        testFieldDynamically("jwksUri", OIDCClientRepresentation::setJwksUri);
        testFieldDynamically("logoUri", OIDCClientRepresentation::setLogoUri);
        testFieldDynamically("policyUri", OIDCClientRepresentation::setPolicyUri);
        testFieldDynamically("backchannelLogoutUrl", OIDCClientRepresentation::setBackchannelLogoutUri);

        //test sectorIdentifierUri
        String redirectUri = oauth.getRedirectUri();
        List<String> sectorRedirects = List.of(redirectUri);
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
        oidcClientEndpointsResource.setSectorIdentifierRedirectUris(sectorRedirects);

        String sectorIdentifierUriPattern = "^https://localhost.*/get-sector-identifier-redirect-uris$";

        testFieldDynamically("sectorIdentifierUri", ((c, s) -> {
            c.setRedirectUris(Collections.singletonList(redirectUri));
            c.setSubjectType("pairwise");
            c.setSectorIdentifierUri(s);
        }), sectorIdentifierUriPattern, TestApplicationResourceUrls.pairwiseSectorIdentifierUri(), INVALID_URL);
    }

    public void testFieldByAdmin(String fieldName, BiConsumer<ClientRepresentation, String> setter) throws Exception {
        setupPolicy(List.of(SAFE_PATTERN), List.of(fieldName));

        //create with valid field
        String validClientId = generateSuffixedName("valid-" + fieldName);
        try {
            createClientByAdmin(validClientId, (ClientRepresentation c) -> setter.accept(c, VALID_URL));
        } catch (Exception e) {
            fail("Create failed for valid URI on field " + fieldName + ": " + e.getMessage());
        }

        //create invalid field
        try {
            createClientByAdmin(generateSuffixedName("invalid-" + fieldName), (ClientRepresentation c) ->  setter.accept(c, INVALID_URL));
            fail("Create should have failed for invalid URI on field: " + fieldName);
        } catch (ClientPolicyException e) {
            assertEquals("Invalid " + fieldName, e.getErrorDetail());
        }

        //try to update with invalid field
        try {
            ClientRepresentation cRep = getAdminClient().realm(REALM_NAME).clients().findByClientId(validClientId).get(0);
            updateClientByAdmin(cRep.getId(), (ClientRepresentation c) -> setter.accept(c, INVALID_URL));
            fail("Update should have failed for invalid URI on field: " + fieldName);
        } catch (ClientPolicyException e) {
            assertEquals("Invalid " + fieldName, e.getErrorDetail());
        }
    }

    public void testFieldDynamically(String fieldName, BiConsumer<OIDCClientRepresentation, String> setter) throws Exception {
        testFieldDynamically(fieldName, setter, SAFE_PATTERN, VALID_URL, INVALID_URL);
    }

    public void testFieldDynamically(String fieldName, BiConsumer<OIDCClientRepresentation, String> setter, String pattern, String validUrl, String invalidUrl) throws Exception {
        setupPolicy(List.of(pattern), List.of(fieldName));

        //create with valid field
        String validClientId = null;
        try {
            validClientId = createClientDynamically(generateSuffixedName("valid-" + fieldName), (OIDCClientRepresentation c) -> setter.accept(c, validUrl));
        } catch (Exception e) {
            fail("Create failed for valid URI on field " + fieldName + ": " + e.getMessage());
        }

        //create invalid field
        try {
            createClientDynamically(generateSuffixedName("invalid-" + fieldName), (OIDCClientRepresentation c) -> setter.accept(c, invalidUrl));
            fail("Create should have failed for invalid URI on field: " + fieldName);
        } catch (ClientRegistrationException e) {
            assertEquals("Failed to send request", e.getMessage());
        }

        //try to update with invalid field
        try {
            updateClientDynamically(validClientId, (OIDCClientRepresentation c) -> setter.accept(c, invalidUrl));
            fail("Update should have failed for invalid URI on field: " + fieldName);
        } catch (ClientRegistrationException e) {
            assertEquals("Failed to send request", e.getMessage());
        }
    }

    @Test
    public void testInvalidPatternConfiguration() throws Exception {
        setupPolicy(List.of("("), null);

        String allFieldsClient = generateSuffixedName("invalid-config");

        try {
            createClientByAdmin(allFieldsClient, (ClientRepresentation c) -> {
                c.setRootUrl("invalid-url");
            });
            fail("Should fail because regex is invalid");
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getError());
        }
    }

    @Test
    public void testEmptyPatternConfiguration() throws Exception {
        setupPolicy(Collections.emptyList(), null);

        String allFieldsClient = generateSuffixedName("invalid-config");

        try {
            createClientByAdmin(allFieldsClient, (ClientRepresentation c) -> {
                c.setRootUrl("invalid-url");
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getError());
        }
    }

    private void setupPolicy(List<String> allowedPatterns, List<String> fieldsToValidate) throws Exception {
        SecureClientUrisPatternExecutor.Configuration config = new SecureClientUrisPatternExecutor.Configuration();
        config.setAllowedPatterns(allowedPatterns);
        config.setClientUriFields(fieldsToValidate);

        String jsonProfile = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                (new ClientPoliciesUtil.ClientProfileBuilder()).createProfile(PROFILE_NAME, "Test Profile")
                        .addExecutor(SecureClientUrisPatternExecutorFactory.PROVIDER_ID, config)
                        .toRepresentation()
        ).toString();
        updateProfiles(jsonProfile);

        String jsonPolicy = (new ClientPoliciesUtil.ClientPoliciesBuilder()).addPolicy(
                (new ClientPoliciesUtil.ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Test Policy", Boolean.TRUE)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID, createAnyClientConditionConfig())
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(jsonPolicy);
    }
}

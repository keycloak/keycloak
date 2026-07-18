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
package org.keycloak.social.microsoft;

import java.io.IOException;

import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.oidc.mappers.AbstractClaimMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link MicrosoftIdentityProvider}.
 */
public class MicrosoftIdentityProviderTest {

    private static final String TENANT_ID = "contoso.onmicrosoft.com";
    private static final String GRAPH_USER_ID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final String ID_TOKEN_SUBJECT = "AAAAAAAAAAAAAAAAAAAAAIkzqFVrSaSaFHy782bbtaQ";
    private static final String GRAPH_PROFILE_URL = "https://graph.microsoft.com/v1.0/me/";
    private static final String OID_CLAIM = "oid";

    @Test
    public void testCommonTenantEndpoints() {
        MicrosoftIdentityProviderConfig config = new MicrosoftIdentityProviderConfig(new IdentityProviderModel());
        config.setEnabled(true);
        MicrosoftIdentityProvider idp = new MicrosoftIdentityProvider(null, config);

        validateEndpoints(idp, "common");
        Assert.assertTrue(config.getDefaultScope().contains(OIDCIdentityProvider.SCOPE_OPENID));
        Assert.assertTrue(config.getDefaultScope().contains("profile"));
        Assert.assertTrue(config.getDefaultScope().contains("email"));
        Assert.assertTrue(config.getDefaultScope().contains("User.read"));
        Assert.assertTrue(config.isUseJwksUrl());
        Assert.assertTrue(config.isValidateSignature());
        Assert.assertTrue(config.isAllowClientIdAsAudience());
    }

    @Test
    public void testSingleTenantEndpoints() {
        IdentityProviderModel model = new IdentityProviderModel();
        MicrosoftIdentityProviderConfig config = new MicrosoftIdentityProviderConfig(model);
        config.setEnabled(true);
        config.setTenantId(TENANT_ID);
        MicrosoftIdentityProvider idp = new MicrosoftIdentityProvider(null, config);

        validateEndpoints(idp, TENANT_ID);
    }

    @Test
    public void testIdentityMatchesIdTokenUsesOidClaim() {
        MicrosoftIdentityProvider idp = createProvider();
        BrokeredIdentityContext identity = new BrokeredIdentityContext(GRAPH_USER_ID, idp.getConfig());
        JsonWebToken idToken = new JsonWebToken();
        idToken.subject(ID_TOKEN_SUBJECT);
        idToken.getOtherClaims().put(OID_CLAIM, GRAPH_USER_ID);

        Assert.assertTrue(idp.identityMatchesIdToken(identity, idToken));
    }

    @Test
    public void testIdentityMatchesIdTokenWhenOidAbsent() {
        MicrosoftIdentityProvider idp = createProvider();
        BrokeredIdentityContext identity = new BrokeredIdentityContext(GRAPH_USER_ID, idp.getConfig());
        JsonWebToken idToken = new JsonWebToken();
        idToken.subject(ID_TOKEN_SUBJECT);

        Assert.assertTrue(idp.identityMatchesIdToken(identity, idToken));
    }

    @Test
    public void testIdentityMatchesIdTokenRejectsOidMismatch() {
        MicrosoftIdentityProvider idp = createProvider();
        BrokeredIdentityContext identity = new BrokeredIdentityContext(GRAPH_USER_ID, idp.getConfig());
        JsonWebToken idToken = new JsonWebToken();
        idToken.subject(ID_TOKEN_SUBJECT);
        idToken.getOtherClaims().put(OID_CLAIM, "other-object-id");

        Assert.assertFalse(idp.identityMatchesIdToken(identity, idToken));
    }

    @Test
    public void testLegacyScopePreservesUserReadOnly() {
        IdentityProviderModel model = new IdentityProviderModel();
        model.getConfig().put("defaultScope", "User.read");
        MicrosoftIdentityProviderConfig config = new MicrosoftIdentityProviderConfig(model);
        config.setEnabled(true);
        new MicrosoftIdentityProvider(null, config);

        Assert.assertEquals("User.read", config.getDefaultScope());
        Assert.assertFalse(MicrosoftIdentityProvider.isOpenIdScopeConfigured(config));
    }

    @Test
    public void testOpenIdScopeEnablesOidcValidation() {
        IdentityProviderModel model = new IdentityProviderModel();
        MicrosoftIdentityProviderConfig config = new MicrosoftIdentityProviderConfig(model);
        config.setEnabled(true);
        new MicrosoftIdentityProvider(null, config);

        Assert.assertTrue(MicrosoftIdentityProvider.isOpenIdScopeConfigured(config));
        Assert.assertTrue(config.isUseJwksUrl());
        Assert.assertTrue(config.isValidateSignature());
        Assert.assertTrue(config.isAllowClientIdAsAudience());
    }

    @Test
    public void testLegacyScopeDoesNotForceSignatureValidation() {
        IdentityProviderModel model = new IdentityProviderModel();
        model.getConfig().put("defaultScope", "User.read");
        model.getConfig().put("validateSignature", "false");
        MicrosoftIdentityProviderConfig config = new MicrosoftIdentityProviderConfig(model);
        config.setEnabled(true);
        new MicrosoftIdentityProvider(null, config);

        Assert.assertFalse(config.isValidateSignature());
    }

    @Test
    public void testLegacyFlowWithoutIdToken() throws IOException {
        IdentityProviderModel model = new IdentityProviderModel();
        model.getConfig().put("defaultScope", "User.read");
        MicrosoftIdentityProviderConfig config = new MicrosoftIdentityProviderConfig(model);
        config.setAlias("microsoft");
        config.setEnabled(true);
        TestMicrosoftIdentityProvider idp = new TestMicrosoftIdentityProvider(config);

        BrokeredIdentityContext identity = idp.getFederatedIdentity(
                "{\"access_token\":\"access-token\",\"token_type\":\"Bearer\"}");

        Assert.assertEquals(GRAPH_USER_ID, identity.getId());
        Assert.assertNull(identity.getContextData().get(OIDCIdentityProvider.VALIDATED_ID_TOKEN));
    }

    @Test
    public void testExtractIdentityExposesIdTokenClaimsToMappers() throws IOException {
        TestMicrosoftIdentityProvider idp = new TestMicrosoftIdentityProvider(createConfig());
        JsonWebToken idToken = new JsonWebToken();
        idToken.subject(ID_TOKEN_SUBJECT);
        idToken.getOtherClaims().put(OID_CLAIM, GRAPH_USER_ID);
        idToken.getOtherClaims().put("preferred_username", "user@contoso.com");
        idToken.getOtherClaims().put("upn", "user@contoso.com");

        BrokeredIdentityContext identity = idp.extractIdentity(new AccessTokenResponse(), "access-token", idToken);

        Assert.assertEquals(GRAPH_USER_ID, identity.getId());
        Assert.assertEquals("user@contoso.com", identity.getUsername());
        Assert.assertSame(idToken, identity.getContextData().get(OIDCIdentityProvider.VALIDATED_ID_TOKEN));
        Assert.assertEquals("user@contoso.com", AbstractClaimMapper.getClaimValue(identity, "preferred_username"));
        Assert.assertEquals("user@contoso.com", AbstractClaimMapper.getClaimValue(identity, "upn"));
    }

    private static void validateEndpoints(MicrosoftIdentityProvider idp, String tenant) {
        OIDCIdentityProviderConfig config = idp.getConfig();
        Assert.assertEquals("https://login.microsoftonline.com/" + tenant + "/oauth2/v2.0/authorize", config.getAuthorizationUrl());
        Assert.assertEquals("https://login.microsoftonline.com/" + tenant + "/oauth2/v2.0/token", config.getTokenUrl());
        Assert.assertEquals("https://login.microsoftonline.com/" + tenant + "/discovery/v2.0/keys", config.getJwksUrl());
        Assert.assertEquals(GRAPH_PROFILE_URL, config.getUserInfoUrl());
        Assert.assertEquals(GRAPH_PROFILE_URL, idp.getProfileEndpointForValidation(null));
    }

    private MicrosoftIdentityProvider createProvider() {
        return new MicrosoftIdentityProvider(null, createConfig());
    }

    private MicrosoftIdentityProviderConfig createConfig() {
        IdentityProviderModel model = new IdentityProviderModel();
        MicrosoftIdentityProviderConfig config = new MicrosoftIdentityProviderConfig(model);
        config.setAlias("microsoft");
        config.setEnabled(true);
        return config;
    }

    private static class TestMicrosoftIdentityProvider extends MicrosoftIdentityProvider {

        private final JsonNode profile;

        TestMicrosoftIdentityProvider(MicrosoftIdentityProviderConfig config) throws IOException {
            super(null, config);
            profile = JsonSerialization.readValue(
                    """
                    {
                      "id": "%s",
                      "mail": "user@contoso.com",
                      "givenName": "Test",
                      "surname": "User"
                    }
                    """.formatted(GRAPH_USER_ID),
                    JsonNode.class);
        }

        @Override
        protected JsonNode fetchMicrosoftProfile(String accessToken) {
            return profile;
        }
    }

}

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
package org.keycloak.test.broker.oidc;

import java.io.IOException;
import java.lang.reflect.Proxy;

import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.provider.AbstractIdentityProvider;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.resteasy.ResteasyKeycloakSession;
import org.keycloak.services.resteasy.ResteasyKeycloakSessionFactory;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.storage.adapter.InMemoryUserAdapter;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link OIDCIdentityProvider#extractIdentity} covering how the broker session ID
 * is resolved from the sid claim in the ID token and the session_state in the token response.
 */
public class OIDCIdentityProviderTest {

    private static final String IDP_ALIAS = "test-idp";
    private static final String SUBJECT = "user-subject-123";
    private static final String SID_FROM_ID_TOKEN = "sid-from-id-token";
    private static final String SESSION_STATE_FROM_TOKEN_RESPONSE = "session-state-from-token-response";

    private static final ResteasyKeycloakSessionFactory sessionFactory = new ResteasyKeycloakSessionFactory();

    @Test
    public void extractIdentity_sidInIdToken_noSessionState_usesSid() throws IOException {
        TestOIDCIdentityProvider provider = createProvider();

        JsonWebToken idToken = buildIdToken(SUBJECT, SID_FROM_ID_TOKEN);
        BrokeredIdentityContext identity = provider.extractIdentity(null, null, idToken);

        Assert.assertEquals(IDP_ALIAS + "." + SID_FROM_ID_TOKEN, identity.getBrokerSessionId());
    }

    @Test
    public void extractIdentity_sidInIdToken_sessionStateInTokenResponse_prefersSid() throws IOException {
        TestOIDCIdentityProvider provider = createProvider();

        JsonWebToken idToken = buildIdToken(SUBJECT, SID_FROM_ID_TOKEN);
        AccessTokenResponse tokenResponse = buildTokenResponse(SESSION_STATE_FROM_TOKEN_RESPONSE);

        BrokeredIdentityContext identity = provider.extractIdentity(tokenResponse, null, idToken);

        Assert.assertEquals(IDP_ALIAS + "." + SID_FROM_ID_TOKEN, identity.getBrokerSessionId());
    }

    @Test
    public void extractIdentity_noSidInIdToken_sessionStateInTokenResponse_usesSessionState() throws IOException {
        TestOIDCIdentityProvider provider = createProvider();

        JsonWebToken idToken = buildIdToken(SUBJECT, null);
        AccessTokenResponse tokenResponse = buildTokenResponse(SESSION_STATE_FROM_TOKEN_RESPONSE);

        BrokeredIdentityContext identity = provider.extractIdentity(tokenResponse, null, idToken);

        Assert.assertEquals(IDP_ALIAS + "." + SESSION_STATE_FROM_TOKEN_RESPONSE, identity.getBrokerSessionId());
    }

    @Test
    public void extractIdentity_noSidNoSessionState_brokerSessionIdIsNull() throws IOException {
        TestOIDCIdentityProvider provider = createProvider();

        JsonWebToken idToken = buildIdToken(SUBJECT, null);

        BrokeredIdentityContext identity = provider.extractIdentity(null, null, idToken);

        Assert.assertNull(identity.getBrokerSessionId());
    }

    @Test
    public void extractIdentity_sidMatchesSessionState_noWarningSideEffect() throws IOException {
        TestOIDCIdentityProvider provider = createProvider();

        JsonWebToken idToken = buildIdToken(SUBJECT, SID_FROM_ID_TOKEN);
        AccessTokenResponse tokenResponse = buildTokenResponse(SID_FROM_ID_TOKEN);

        BrokeredIdentityContext identity = provider.extractIdentity(tokenResponse, null, idToken);

        Assert.assertEquals(IDP_ALIAS + "." + SID_FROM_ID_TOKEN, identity.getBrokerSessionId());
    }

    @Test
    public void setEmailVerified_fallsBackToValidatedIdTokenFalse_whenContextEntryAbsent() {
        Assert.assertFalse("token false must be applied over the user's current true",
                resolveEmailVerified(null, false, true));
    }

    @Test
    public void setEmailVerified_fallsBackToValidatedIdTokenTrue_whenContextEntryAbsent() {
        Assert.assertTrue(resolveEmailVerified(null, true, false));
    }

    @Test
    public void setEmailVerified_contextEntryFalseTakesPrecedenceOverValidatedIdTokenTrue() {
        Assert.assertFalse(resolveEmailVerified(false, true, true));
    }

    @Test
    public void setEmailVerified_contextEntryTrueTakesPrecedenceOverValidatedIdTokenFalse() {
        Assert.assertTrue(resolveEmailVerified(true, false, false));
    }

    @Test
    public void setEmailVerified_doesNotFallBackToValidatedIdToken_whenEmailDiffers() {
        TestOIDCIdentityProvider provider = createProvider();
        provider.getConfig().setTrustEmail(true);

        JsonWebToken idToken = buildIdToken(SUBJECT, null);
        idToken.getOtherClaims().put(IDToken.EMAIL, "a@example.com");
        idToken.getOtherClaims().put(IDToken.EMAIL_VERIFIED, false);

        BrokeredIdentityContext context = new BrokeredIdentityContext(SUBJECT, provider.getConfig());
        context.setEmail("b@example.com");
        context.getContextData().put(OIDCIdentityProvider.VALIDATED_ID_TOKEN, idToken);
        // the default behavior reads the new-user note from the session
        context.setAuthenticationSession(authSession(true));

        UserModel user = createUser();
        user.setEmail("b@example.com");
        user.setEmailVerified(false);

        provider.callSetEmailVerified(user, context);

        Assert.assertTrue("the default trust-email behavior must apply, not the claim of a different address",
                user.isEmailVerified());
        Assert.assertEquals("the validated token must be kept intact for the mappers",
                false, idToken.getOtherClaims().get(IDToken.EMAIL_VERIFIED));
    }

    @Test
    public void setEmailVerified_fallsBackToValidatedIdToken_whenEmailDiffersOnlyInCase() {
        JsonWebToken idToken = buildIdToken(SUBJECT, null);
        idToken.getOtherClaims().put(IDToken.EMAIL, "a@example.com");
        idToken.getOtherClaims().put(IDToken.EMAIL_VERIFIED, false);

        Assert.assertFalse("a case-only difference is the same address, so the token claim applies",
                setEmailVerifiedWithValidatedToken(idToken, "A@Example.COM", true));
    }

    @Test
    public void setEmailVerified_usesDefault_whenValidatedIdTokenHasNoEmailVerifiedClaim() {
        JsonWebToken idToken = buildIdToken(SUBJECT, null);
        idToken.getOtherClaims().put(IDToken.EMAIL, "a@example.com");

        Assert.assertTrue("without a claim the default trust-email behavior must apply",
                setEmailVerifiedWithValidatedToken(idToken, "a@example.com", false));
    }

    @Test
    public void setEmailVerified_parsesStringEmailVerifiedClaim() {
        JsonWebToken idToken = buildIdToken(SUBJECT, null);
        idToken.getOtherClaims().put(IDToken.EMAIL, "a@example.com");
        idToken.getOtherClaims().put(IDToken.EMAIL_VERIFIED, "false");

        Assert.assertFalse("the string \"false\" must be read as false, not ignored",
                setEmailVerifiedWithValidatedToken(idToken, "a@example.com", true));
    }

    @Test
    public void setEmailVerified_usesDefault_whenValidatedIdTokenIsNotAJsonWebToken() {
        Assert.assertTrue("a raw string token cannot be read, so the default trust-email behavior must apply",
                setEmailVerifiedWithValidatedToken("raw.id.token", "a@example.com", false));
    }

    @Test
    public void resolveEmailVerified_userInfoEmailVerifiedWins_whenPresent() {
        // UserInfo explicitly says true — overrides ID token false
        Boolean result = TestOIDCIdentityProvider.callResolveEmailVerifiedFromUserInfo(
                "b@example.com", Boolean.TRUE, "a@example.com", Boolean.FALSE);
        Assert.assertTrue("UserInfo email_verified=true must win over ID token email_verified=false", result);
    }

    @Test
    public void resolveEmailVerified_userInfoEmailVerifiedFalseWins_whenPresent() {
        // UserInfo explicitly says false — overrides ID token true
        Boolean result = TestOIDCIdentityProvider.callResolveEmailVerifiedFromUserInfo(
                "b@example.com", Boolean.FALSE, "a@example.com", Boolean.TRUE);
        Assert.assertFalse("UserInfo email_verified=false must win over ID token email_verified=true", result);
    }

    @Test
    public void resolveEmailVerified_differentEmail_noUserInfoClaim_discardsIdTokenClaim() {
        // UserInfo selects a different address and provides no email_verified —
        // the ID token's false must NOT carry over to the new address
        Boolean result = TestOIDCIdentityProvider.callResolveEmailVerifiedFromUserInfo(
                "b@example.com", null, "a@example.com", Boolean.FALSE);
        Assert.assertNull("ID token email_verified must be discarded when UserInfo email differs", result);
    }

    @Test
    public void resolveEmailVerified_differentEmail_noUserInfoClaim_discardsIdTokenTrue() {
        // Same as above but ID token says true — must still be discarded
        Boolean result = TestOIDCIdentityProvider.callResolveEmailVerifiedFromUserInfo(
                "b@example.com", null, "a@example.com", Boolean.TRUE);
        Assert.assertNull("ID token email_verified=true must be discarded when UserInfo email differs", result);
    }

    @Test
    public void resolveEmailVerified_sameEmail_noUserInfoClaim_keepsIdTokenClaim() {
        // Same address, no UserInfo claim — retain the ID token value (GitLab scenario)
        Boolean result = TestOIDCIdentityProvider.callResolveEmailVerifiedFromUserInfo(
                "a@example.com", null, "a@example.com", Boolean.FALSE);
        Assert.assertFalse("ID token email_verified must be kept when UserInfo has the same email and no claim", result);
    }

    @Test
    public void resolveEmailVerified_noIdTokenEmail_noUserInfoClaim_keepsIdTokenClaim() {
        // null ID token email is treated as same (isSameEmail semantics) — retain value
        Boolean result = TestOIDCIdentityProvider.callResolveEmailVerifiedFromUserInfo(
                "b@example.com", null, null, Boolean.FALSE);
        Assert.assertFalse("null ID token email must not be treated as a mismatch", result);
    }

    @Test
    public void resolveEmailVerified_caseInsensitiveMatch_noUserInfoClaim_keepsIdTokenClaim() {
        // addresses differ only in case — equalsIgnoreCase must treat them as the same
        Boolean result = TestOIDCIdentityProvider.callResolveEmailVerifiedFromUserInfo(
                "A@Example.COM", null, "a@example.com", Boolean.FALSE);
        Assert.assertFalse("addresses that match case-insensitively must not be treated as a mismatch", result);
    }

    @Test
    public void setEmailVerified_trustEmailDisabled_ignoresTokenFallback() {
        TestOIDCIdentityProvider provider = createProvider();
        provider.getConfig().setTrustEmail(false);

        JsonWebToken idToken = buildIdToken(SUBJECT, null);
        idToken.getOtherClaims().put(IDToken.EMAIL_VERIFIED, true);

        BrokeredIdentityContext context = new BrokeredIdentityContext(SUBJECT, provider.getConfig());
        context.getContextData().put(OIDCIdentityProvider.VALIDATED_ID_TOKEN, idToken);
        context.setAuthenticationSession(authSession(true));

        UserModel user = createUser();
        user.setEmailVerified(true);

        provider.callSetEmailVerified(user, context);

        Assert.assertFalse("with trustEmail=false the superclass default applies; token fallback must not bypass it",
                user.isEmailVerified());
    }

    private boolean resolveEmailVerified(Boolean contextValue, boolean tokenValue, boolean initialUserValue) {
        TestOIDCIdentityProvider provider = createProvider();
        provider.getConfig().setTrustEmail(true);

        JsonWebToken idToken = buildIdToken(SUBJECT, null);
        idToken.getOtherClaims().put(IDToken.EMAIL_VERIFIED, tokenValue);

        BrokeredIdentityContext context = new BrokeredIdentityContext(SUBJECT, provider.getConfig());
        context.getContextData().put(OIDCIdentityProvider.VALIDATED_ID_TOKEN, idToken);
        if (contextValue != null) {
            context.getContextData().put(IDToken.EMAIL_VERIFIED, contextValue);
        }

        UserModel user = createUser();
        user.setEmailVerified(initialUserValue);
        provider.callSetEmailVerified(user, context);
        return user.isEmailVerified();
    }

    private boolean setEmailVerifiedWithValidatedToken(Object validatedIdToken, String contextEmail, boolean initialUserValue) {
        TestOIDCIdentityProvider provider = createProvider();
        provider.getConfig().setTrustEmail(true);

        BrokeredIdentityContext context = new BrokeredIdentityContext(SUBJECT, provider.getConfig());
        context.setEmail(contextEmail);
        context.getContextData().put(OIDCIdentityProvider.VALIDATED_ID_TOKEN, validatedIdToken);
        context.setAuthenticationSession(authSession(true));

        UserModel user = createUser();
        user.setEmailVerified(initialUserValue);
        provider.callSetEmailVerified(user, context);
        return user.isEmailVerified();
    }

    private AuthenticationSessionModel authSession(boolean newUser) {
        return (AuthenticationSessionModel) Proxy.newProxyInstance(
                AuthenticationSessionModel.class.getClassLoader(), new Class<?>[] { AuthenticationSessionModel.class },
                (proxy, method, args) -> "getAuthNote".equals(method.getName())
                        && AbstractIdentityProvider.BROKER_REGISTERED_NEW_USER.equals(args[0])
                        ? String.valueOf(newUser) : null);
    }

    private UserModel createUser() {
        return new InMemoryUserAdapter(new ResteasyKeycloakSession(sessionFactory), null, KeycloakModelUtils.generateId());
    }

    private TestOIDCIdentityProvider createProvider() {
        IdentityProviderModel model = new IdentityProviderModel();
        OIDCIdentityProviderConfig config = new OIDCIdentityProviderConfig(model);
        config.setAlias(IDP_ALIAS);
        config.setEnabled(true);
        config.setDisableUserInfoService(true); // avoid HTTP calls to userinfo endpoint
        return new TestOIDCIdentityProvider(config);
    }

    private static class TestOIDCIdentityProvider extends OIDCIdentityProvider {

        TestOIDCIdentityProvider(OIDCIdentityProviderConfig config) {
            super(null, config);
        }

        @Override
        public BrokeredIdentityContext extractIdentity(AccessTokenResponse tokenResponse,
                String accessToken, JsonWebToken idToken) throws IOException {
            return super.extractIdentity(tokenResponse, accessToken, idToken);
        }

        void callSetEmailVerified(UserModel user, BrokeredIdentityContext context) {
            setEmailVerified(user, context);
        }

        static Boolean callResolveEmailVerifiedFromUserInfo(String userInfoEmail, Boolean userInfoEmailVerified,
                String idTokenEmail, Boolean idTokenEmailVerified) {
            return resolveEmailVerifiedFromUserInfo(userInfoEmail, userInfoEmailVerified, idTokenEmail, idTokenEmailVerified);
        }
    }

    private JsonWebToken buildIdToken(String subject, String sid) {
        JsonWebToken token = new JsonWebToken();
        token.subject(subject);
        if (sid != null) {
            token.getOtherClaims().put("sid", sid);
        }
        return token;
    }

    private AccessTokenResponse buildTokenResponse(String sessionState) {
        AccessTokenResponse response = new AccessTokenResponse();
        response.setSessionState(sessionState);
        return response;
    }
}

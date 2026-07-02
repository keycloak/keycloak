/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.MultivaluedMap;

import org.keycloak.OAuth2Constants;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.common.util.Time;
import org.keycloak.events.EventBuilder;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.validation.Validation;
import org.keycloak.util.Booleans;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Identity provider for Microsoft account. Uses OpenID Connect with Microsoft Graph as documented at
 * <a href="https://learn.microsoft.com/en-us/entra/identity-platform/v2-protocols-oidc">Microsoft identity platform OIDC</a>
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class MicrosoftIdentityProvider extends OIDCIdentityProvider implements SocialIdentityProvider<OIDCIdentityProviderConfig> {

    private static final String AUTH_URL_TEMPLATE = "https://login.microsoftonline.com/%s/oauth2/v2.0/authorize";
    private static final String TOKEN_URL_TEMPLATE = "https://login.microsoftonline.com/%s/oauth2/v2.0/token";
    private static final String JWKS_URL_TEMPLATE = "https://login.microsoftonline.com/%s/discovery/v2.0/keys";
    private static final String PROFILE_URL = "https://graph.microsoft.com/v1.0/me/";
    private static final String LEGACY_DEFAULT_SCOPE = "User.read";
    private static final String DEFAULT_SCOPE = "profile email User.read";
    private static final String OID_CLAIM = "oid";

    private final boolean oidcFlowEnabled;

    public MicrosoftIdentityProvider(KeycloakSession session, MicrosoftIdentityProviderConfig config) {
        this(session, config, isOpenIdScopeConfigured(config));
    }

    private MicrosoftIdentityProvider(KeycloakSession session, MicrosoftIdentityProviderConfig config, boolean oidcFlowEnabled) {
        super(session, config);

        this.oidcFlowEnabled = oidcFlowEnabled;
        if (!oidcFlowEnabled) {
            restoreScopeWithoutOpenId(config);
        }

        // Use multi-tenant 'common' endpoints if not specified.
        String tenant = Optional.ofNullable(config.getTenantId()).map(String::trim).filter(t -> !t.isEmpty()).orElse("common");

        config.setAuthorizationUrl(String.format(AUTH_URL_TEMPLATE, tenant));
        config.setTokenUrl(String.format(TOKEN_URL_TEMPLATE, tenant));
        config.setJwksUrl(String.format(JWKS_URL_TEMPLATE, tenant));
        config.setUserInfoUrl(PROFILE_URL);
        if (oidcFlowEnabled) {
            config.setUseJwksUrl(true);
            config.setValidateSignature(true);
            config.setAllowClientIdAsAudience(true);
        }
    }

    /**
     * Returns {@code true} when the configured scope includes {@code openid}, or when no scope is configured yet
     * (new identity providers use the OIDC flow with ID token claims available to mappers).
     */
    static boolean isOpenIdScopeConfigured(MicrosoftIdentityProviderConfig config) {
        String scope = config.getConfig().get("defaultScope");
        if (scope == null || scope.isBlank()) {
            return true;
        }
        return Arrays.stream(scope.split("\\s+")).anyMatch(SCOPE_OPENID::equals);
    }

    private static void restoreScopeWithoutOpenId(MicrosoftIdentityProviderConfig config) {
        String scope = config.getDefaultScope();
        if (scope == null) {
            config.setDefaultScope(LEGACY_DEFAULT_SCOPE);
            return;
        }
        String restored = Arrays.stream(scope.split("\\s+"))
                .filter(s -> !s.isEmpty() && !SCOPE_OPENID.equals(s))
                .collect(Collectors.joining(" "));
        config.setDefaultScope(restored.isEmpty() ? LEGACY_DEFAULT_SCOPE : restored);
    }

    @Override
    public BrokeredIdentityContext getFederatedIdentity(String response) {
        if (oidcFlowEnabled) {
            return super.getFederatedIdentity(response);
        }
        return getFederatedIdentityLegacy(response);
    }

    private BrokeredIdentityContext getFederatedIdentityLegacy(String response) {
        String accessToken = extractTokenFromResponse(response, OAUTH2_PARAMETER_ACCESS_TOKEN);
        if (accessToken == null) {
            throw new IdentityBrokerException("No access token available in OAuth server response: " + response);
        }

        try {
            BrokeredIdentityContext context = extractIdentityFromProfile(null, fetchMicrosoftProfile(accessToken));

            if (Booleans.isTrue(getConfig().isStoreToken()) && response.startsWith("{")) {
                try {
                    AbstractOAuth2IdentityProvider.OAuthResponse tokenResponse =
                            JsonSerialization.readValue(response, AbstractOAuth2IdentityProvider.OAuthResponse.class);
                    if (tokenResponse.getExpiresIn() != null && tokenResponse.getExpiresIn() > 0) {
                        long accessTokenExpiration = Time.currentTime() + tokenResponse.getExpiresIn();
                        tokenResponse.setAccessTokenExpiration(accessTokenExpiration);
                        response = JsonSerialization.writeValueAsString(tokenResponse);
                    }
                    context.setToken(response);
                } catch (IOException e) {
                    logger.debugf("Can't store expiration date in JSON token", e);
                }
            }

            context.getContextData().put(FEDERATED_ACCESS_TOKEN, accessToken);
            return context;
        } catch (IOException e) {
            throw new IdentityBrokerException("Could not obtain user profile from Microsoft Graph", e);
        }
    }

    @Override
    protected boolean supportsExternalExchange() {
        return true;
    }

    @Override
    protected String getProfileEndpointForValidation(EventBuilder event) {
        return PROFILE_URL;
    }

    @Override
    public boolean isIssuer(String issuer, MultivaluedMap<String, String> params) {
        String requestedIssuer = params.getFirst(OAuth2Constants.SUBJECT_ISSUER);
        if (requestedIssuer == null) {
            requestedIssuer = issuer;
        }
        return requestedIssuer.equals(getConfig().getAlias());
    }

    @Override
    protected BrokeredIdentityContext exchangeExternalImpl(EventBuilder event, MultivaluedMap<String, String> params) {
        return exchangeExternalUserInfoValidationOnly(event, params);
    }

    @Override
    protected BrokeredIdentityContext extractIdentity(AccessTokenResponse tokenResponse, String accessToken, JsonWebToken idToken) throws IOException {
        JsonNode profile = fetchMicrosoftProfile(accessToken);
        BrokeredIdentityContext identity = extractIdentityFromProfile(null, profile);
        identity.getContextData().put(FEDERATED_ACCESS_TOKEN_RESPONSE, tokenResponse);
        identity.getContextData().put(VALIDATED_ID_TOKEN, idToken);
        processAccessTokenResponse(identity, tokenResponse);
        enrichBrokerSession(identity, tokenResponse, idToken);
        return identity;
    }

    private void enrichBrokerSession(BrokeredIdentityContext identity, AccessTokenResponse tokenResponse,
            JsonWebToken idToken) {
        identity.setBrokerUserId(getConfig().getAlias() + "." + identity.getId());

        String sidClaim = (String) idToken.getOtherClaims().get(IDToken.SESSION_ID);
        String sessionState = tokenResponse != null ? tokenResponse.getSessionState() : null;
        if (sidClaim != null) {
            if (sessionState != null && !sidClaim.equals(sessionState)) {
                logger.warnf("IdP '%s': sid claim '%s' differs from session_state '%s'; using sid for backchannel logout.",
                        getConfig().getAlias(), sidClaim, sessionState);
            }
            identity.setBrokerSessionId(getConfig().getAlias() + "." + sidClaim);
        } else if (sessionState != null) {
            identity.setBrokerSessionId(getConfig().getAlias() + "." + sessionState);
        }
    }

    @Override
    protected boolean identityMatchesIdToken(BrokeredIdentityContext identity, JsonWebToken idToken) {
        Object oid = idToken.getOtherClaims().get(OID_CLAIM);
        if (oid == null) {
            // Microsoft omits oid without the profile scope; some B2B guest tokens never include it.
            // Graph /me id remains the authoritative object id in that case.
            return true;
        }
        return identity.getId().equals(oid.toString());
    }

    @Override
    protected BrokeredIdentityContext extractIdentityFromProfile(EventBuilder event, JsonNode profile) {
        String id = getJsonProperty(profile, "id");
        BrokeredIdentityContext user = new BrokeredIdentityContext(id, getConfig());

        String email = getJsonProperty(profile, "mail");
        if (email == null && profile.has("userPrincipalName")) {
            String username = getJsonProperty(profile, "userPrincipalName");
            if (Validation.isEmailValid(username)) {
                email = username;
            }
        }
        user.setUsername(email != null ? email : id);
        user.setFirstName(getJsonProperty(profile, "givenName"));
        user.setLastName(getJsonProperty(profile, "surname"));
        if (email != null) {
            user.setEmail(email);
        }
        user.setIdp(this);

        AbstractJsonUserAttributeMapper.storeUserProfileForMapper(user, profile, getConfig().getAlias());
        return user;
    }

    @Override
    protected String getDefaultScopes() {
        return DEFAULT_SCOPE;
    }

    protected JsonNode fetchMicrosoftProfile(String accessToken) throws IOException {
        JsonNode profile = SimpleHttp.create(session).doGet(PROFILE_URL).auth(accessToken).asJson();
        if (profile.has("error") && !profile.get("error").isNull()) {
            throw new IdentityBrokerException("Error in Microsoft Graph API response. Payload: " + profile);
        }
        return profile;
    }

}

/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oidc.grants;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.Profile;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.organization.protocol.mappers.oidc.OrganizationScope;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.rar.AuthorizationDetailsProcessorManager;
import org.keycloak.protocol.oidc.refresh.RefreshTokenContext;
import org.keycloak.protocol.oidc.refresh.RefreshTokenProvider;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.RefreshToken;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.TokenRefreshContext;
import org.keycloak.services.clientpolicy.context.TokenRefreshResponseContext;
import org.keycloak.services.util.MtlsHoKTokenUtil;

import org.jboss.logging.Logger;

/**
 * OAuth 2.0 Refresh Token Grant
 * https://datatracker.ietf.org/doc/html/rfc6749#section-6
 *
 * @author <a href="mailto:demetrio@carretti.pro">Dmitry Telegin</a> (et al.)
 */
public class RefreshTokenGrantType extends OAuth2GrantTypeBase {

    private static final Logger logger = Logger.getLogger(RefreshTokenGrantType.class);

    @Override
    public Response process(Context context) {
        setContext(context);

        String refreshToken = formParams.getFirst(OAuth2Constants.REFRESH_TOKEN);
        if (refreshToken == null) {
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "No refresh token", Response.Status.BAD_REQUEST);
        }

        String scopeParameter = getRequestedScopes();
        String resourceParameter = formParams.getFirst(OAuth2Constants.RESOURCE);

        AccessTokenResponse res;
        try {
            // Decode the refresh token (no binding check) solely to read the session's originally
            // granted scopes, so that client policies receive a sanitized scope parameter before
            // any binding enforcement runs. Full verification — including certificate/DPoP binding
            // — happens after policies have evaluated, so that policy executors such as
            // HolderOfKeyEnforcerExecutor can enforce binding and produce the correct error code.
            RefreshToken oldRefreshToken = tokenManager.toRefreshToken(session, refreshToken);

            // Compute the scope for client policy evaluation: if no scope was requested use the
            // full session scope (RFC 6749 §6); otherwise intersect with the session scope to
            // strip ungranted scopes and preserve session-active scopes.
            String effectiveScopeParameter = computeEffectiveScope(scopeParameter, oldRefreshToken.getScope());

            String encodedTokenBeforePolicy = refreshToken;
            try {
                session.clientPolicy().triggerOnEvent(new TokenRefreshContext(formParams, client, effectiveScopeParameter));
                refreshToken = formParams.getFirst(OAuth2Constants.REFRESH_TOKEN);
            } catch (ClientPolicyException cpe) {
                event.detail(Details.REASON, Details.CLIENT_POLICY_ERROR);
                event.detail(Details.CLIENT_POLICY_ERROR, cpe.getError());
                event.detail(Details.CLIENT_POLICY_ERROR_DETAIL, cpe.getErrorDetail());
                event.error(cpe.getError());
                throw new CorsErrorResponseException(cors, cpe.getError(), cpe.getErrorDetail(), cpe.getErrorStatus());
            }

            // If a policy executor rotated the refresh token, re-decode the new token; otherwise
            // reuse the already signature-verified token to avoid a second cryptographic check.
            if (!refreshToken.equals(encodedTokenBeforePolicy)) {
                oldRefreshToken = tokenManager.toRefreshToken(session, refreshToken);
            }

            // Full verification on the pre-decoded token: type, realm issuer, expiry, client-ID,
            // and certificate/DPoP binding — without a second JWS signature check.
            // Clients that rely on HolderOfKeyEnforcerExecutor have already been rejected above
            // with invalid_grant/400; for all other MTLS/DPoP clients binding is enforced here.
            oldRefreshToken = tokenManager.verifyRefreshToken(session, realm, client, request, oldRefreshToken, true);

            // The provider already performs org-aware scope intersection via transformScopes,
            // so pass the original scopeParameter rather than the pre-intersected value.
            RefreshTokenContext refreshTokenCtx = new RefreshTokenContext(oldRefreshToken, context, this, tokenManager, scopeParameter, resourceParameter);

            RefreshTokenProvider refreshTokenProvider = session.getKeycloakSessionFactory()
                    .getProviderFactoriesStream(RefreshTokenProvider.class)
                    .sorted((f1, f2) -> f2.order() - f1.order())
                    .map(f -> session.getProvider(RefreshTokenProvider.class, f.getId()))
                    .filter(p -> p.supports(refreshTokenCtx))
                    .findFirst()
                    .orElseThrow(() -> new OAuthErrorException(Errors.INVALID_REQUEST, "No provider available to handle refresh token"));

            TokenManager.AccessTokenResponseBuilder responseBuilder = refreshTokenProvider.refreshAccessToken(refreshTokenCtx);

            checkAndBindMtlsHoKToken(responseBuilder, clientConfig.isUseRefreshToken());

            session.clientPolicy().triggerOnEvent(new TokenRefreshResponseContext(formParams, responseBuilder));

            res = responseBuilder.build();
        } catch (JWSInputException e) {
            event.detail(Details.REASON, "Invalid refresh token");
            event.error(Errors.INVALID_TOKEN);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "Invalid refresh token", Response.Status.BAD_REQUEST);
        } catch (OAuthErrorException e) {
            logger.trace(e.getMessage(), e);
            // KEYCLOAK-6771 Certificate Bound Token
            if (MtlsHoKTokenUtil.CERT_VERIFY_ERROR_DESC.equals(e.getDescription())) {
                event.detail(Details.REASON, e.getDescription());
                event.error(Errors.NOT_ALLOWED);
                throw new CorsErrorResponseException(cors, e.getError(), e.getDescription(), Response.Status.UNAUTHORIZED);
            } else {
                event.detail(Details.REASON, e.getDescription());
                event.error(Errors.INVALID_TOKEN);
                throw new CorsErrorResponseException(cors, e.getError(), e.getDescription(), Response.Status.BAD_REQUEST);
            }
        } catch (ClientPolicyException cpe) {
            event.detail(Details.REASON, Details.CLIENT_POLICY_ERROR);
            event.detail(Details.CLIENT_POLICY_ERROR, cpe.getError());
            event.detail(Details.CLIENT_POLICY_ERROR_DETAIL, cpe.getErrorDetail());
            event.error(cpe.getError());
            throw new CorsErrorResponseException(cors, cpe.getError(), cpe.getErrorDetail(), cpe.getErrorStatus());
        }

        // Sanitize authorization details before they are sent as part of the Token Response
        var authDetailsProcessor = new AuthorizationDetailsProcessorManager(session);
        authDetailsProcessor.sanitizeBeforeSendingTokenResponse(res);

        event.success();

        return cors.add(Response.ok(res, MediaType.APPLICATION_JSON_TYPE));
    }

    /**
     * Computes the effective scope for client policy evaluation during token refresh.
     *
     * When no scope is requested, returns the full session scope (RFC 6749 §6).
     * When a scope is requested, intersects it with the session scope using
     * organization-aware matching (mirrors AbstractRefreshTokenProvider.transformScopes)
     * so that organization scope narrowing (e.g. organization:* → organization:orga)
     * is preserved for policy conditions.
     */
    private String computeEffectiveScope(String requestedScope, String sessionScope) {
        if (requestedScope == null || requestedScope.isEmpty()) {
            return sessionScope != null ? sessionScope : "";
        }
        if (sessionScope == null || sessionScope.isEmpty()) {
            // Non-refresh tokens (e.g. ID tokens) carry no scope; return empty so the later
            // verifyRefreshToken() rejects them with invalid_grant rather than NPE-ing here.
            return "";
        }
        Set<String> requestedScopes = Arrays.stream(requestedScope.split(" ")).collect(Collectors.toSet());
        return Arrays.stream(sessionScope.split(" "))
                .map(scope -> {
                    if (requestedScopes.contains(scope)) {
                        return scope;
                    }
                    if (Profile.isFeatureEnabled(Profile.Feature.ORGANIZATION)) {
                        OrganizationScope orgScope = OrganizationScope.valueOfScope(session, scope);
                        return orgScope == null ? null : orgScope.resolveName(session, requestedScopes, scope);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
    }

    @Override
    public EventType getEventType() {
        return EventType.REFRESH_TOKEN;
    }
    
    @Override
    public Set<String> getTokenParameterNames() {
        return Collections.emptySet();
    }
}

/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.keycloak.protocol.oidc.tokenexchange;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.TokenExchangeContext;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.UserSessionManager;
import org.keycloak.util.JsonSerialization;

/**
 *
 * @author rmartinc
 */
public class TokenExchangeDelegationProvider extends StandardTokenExchangeProvider {

    public static final String ENFORCE_CLAIMS = "enforce_claims";
    public static final List<String> DEFAULT_ENFORCED_CLAIMS = List.of(JsonWebToken.SUBJECT, OIDCLoginProtocol.ISSUER);

    private AccessToken actorAccessToken;
    private AccessToken subjectAccessToken;

    @Override
    public boolean supports(TokenExchangeContext context) {
        if(!OIDCAdvancedConfigWrapper.fromClientModel(context.getClient()).isStandardTokenExchangeEnabled()) {
            context.setUnsupportedReason("Standard token exchange is not enabled for the requested client");
            return false;
        }

        // Subject delegation request needs the actor token
        String actorToken = context.getParams().getActorToken();
        if (actorToken != null) {
            return true;
        }

        context.setUnsupportedReason("Token exchange delegation not used because no actor_token sent");
        return false;
    }

    @Override
    protected Response tokenExchange() {
        // validate subject token
        AuthenticationManager.AuthResult subjectAuthResult = processSubjectToken();
        UserModel subjectUser = subjectAuthResult.user();
        subjectAccessToken = subjectAuthResult.token();

        // validate actor token
        String actorToken = context.getParams().getActorToken();
        String actorTokenType = context.getParams().getActorTokenType();
        if (!OAuth2Constants.ACCESS_TOKEN_TYPE.equals(actorTokenType)) {
            event.detail(Details.REASON, "actor_token_type invalid");
            event.error(Errors.INVALID_REQUEST);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Invalid actor token type", Response.Status.BAD_REQUEST);
        }

        AuthenticationManager.AuthResult actorAuthResult = AuthenticationManager.verifyIdentityToken(session, realm, session.getContext().getUri(), clientConnection, true, true, null,
                false, actorToken, context.getHeaders(), verifier -> {});
        if (actorAuthResult == null) {
            event.detail(Details.REASON, "actor_token validation failure");
            event.error(Errors.INVALID_TOKEN);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Invalid actor token", Response.Status.BAD_REQUEST);
        }
        UserModel actorUser = actorAuthResult.user();
        UserSessionModel actorSession = actorAuthResult.session();
        actorAccessToken = actorAuthResult.token();

        event.detail(Details.ACTOR_ID, actorUser.getId());
        event.detail(Details.ACTOR, actorUser.getUsername());
        if (actorAccessToken.getSessionId() != null) {
            event.detail(Details.ACTOR_SESSION_ID, actorSession.getId());
        }

        validateSenderConstrainedToken(actorAccessToken);
        if (!client.equals(realm.getClientByClientId(actorAccessToken.getIssuedFor()))) {
            forbiddenIfClientIsNotWithinTokenAudience(actorAccessToken);
        }

        // validate the subject token allows the actor in the "may_act" claim
        validateMayAct(subjectAccessToken, subjectUser, actorUser);

        // always create a transient session for delegation (no refresh token allowed)
        UserSessionModel tokenSession = new UserSessionManager(session).createUserSession(
                KeycloakModelUtils.generateId(), realm, subjectUser, subjectUser.getUsername(), clientConnection.getRemoteHost(),
                ServiceAccountConstants.CLIENT_AUTH, false, null, null, UserSessionModel.SessionPersistenceState.TRANSIENT);

        return exchangeClientToClient(subjectUser, tokenSession, subjectAccessToken, true);
    }

    @Override
    protected String getRequestedTokenType() {
        // only access token type is supported for token exchange delegation
        String requestedTokenType = params.getRequestedTokenType();
        if (requestedTokenType == null) {
            requestedTokenType = OAuth2Constants.ACCESS_TOKEN_TYPE;
            return requestedTokenType;
        }
        if (requestedTokenType.equals(OAuth2Constants.ACCESS_TOKEN_TYPE)) {
            return requestedTokenType;
        }

        event.detail(Details.REASON, "requested_token_type unsupported");
        event.error(Errors.INVALID_REQUEST);
        throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "requested_token_type unsupported", Response.Status.BAD_REQUEST);
    }

    protected void validateMayAct(AccessToken subjectAccessToken, UserModel subjectUser, UserModel actorUser) {
        if (subjectUser.getId().equals(actorUser.getId())) {
            event.detail(Details.REASON, "Actor and subject user cannot be the same user");
            event.error(Errors.INVALID_TOKEN);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Actor and subject user cannot be the same user", Response.Status.BAD_REQUEST);
        }

        Object mayActObject = subjectAccessToken.getOtherClaims().get(IDToken.MAY_ACT);
        if (!(mayActObject instanceof Map mayActMap)) {
            event.detail(Details.REASON, "Invalid may_act claim in the subject_token");
            event.error(Errors.INVALID_TOKEN);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Invalid may_act claim in the subject_token", Response.Status.BAD_REQUEST);
        }

        if (!mayActMap.containsKey(JsonWebToken.SUBJECT)) {
            event.detail(Details.REASON, "Missing mandatory 'sub' claim in may_act");
            event.error(Errors.INVALID_TOKEN);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Missing mandatory 'sub' claim in may_act", Response.Status.BAD_REQUEST);
        }

        Collection<String> enforcedClaims;
        try {
            enforcedClaims = getEnforcedClaims(mayActMap);
        } catch (IllegalArgumentException e) {
            event.detail(Details.REASON, e.getMessage());
            event.error(Errors.INVALID_TOKEN);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, e.getMessage(), Response.Status.BAD_REQUEST);
        }
        Map<String, Object> actorClaims = JsonSerialization.mapper.convertValue(actorAccessToken, Map.class);
        for (String claimName : enforcedClaims) {
            Object mayActValue = mayActMap.get(claimName);
            if (mayActValue == null) {
                continue;
            }
            Object actorValue = actorClaims.get(claimName);
            if (!mayActValue.equals(actorValue)) {
                String reason = String.format("Enforced claim '%s' in may_act does not match actor token claim '%s'", claimName, claimName);
                event.detail(Details.REASON, reason);
                event.error(Errors.INVALID_TOKEN);
                throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, reason, Response.Status.BAD_REQUEST);
            }
        }
    }

    private Collection<String> getEnforcedClaims(Map<String, Object> mayActMap) {
        Object enforceClaimsObj = mayActMap.get(ENFORCE_CLAIMS);
        if (enforceClaimsObj instanceof Collection<?> list && !list.isEmpty()) {
            Set<String> merged = new HashSet<>(DEFAULT_ENFORCED_CLAIMS);
            for (Object item : list) {
                if (!(item instanceof String s)) {
                    throw new IllegalArgumentException("enforce_claims must contain only string values");
                }
                merged.add(s);
            }
            return merged;
        }
        return DEFAULT_ENFORCED_CLAIMS;
    }

    @Override
    protected void checkRequestedAudiences(TokenManager.AccessTokenResponseBuilder responseBuilder) {
        super.checkRequestedAudiences(responseBuilder);

        // add the "act" claim using the "may_act" claim sent in the subjectToken, chain current actor if present
        Map act = new HashMap((Map) subjectAccessToken.getOtherClaims().get(IDToken.MAY_ACT)); // should be present at this point
        act.remove(ENFORCE_CLAIMS);
        Object prevAct = actorAccessToken.getOtherClaims().get(IDToken.ACT);
        if (prevAct != null) {
            act.put(IDToken.ACT, prevAct);
        }
        responseBuilder.getAccessToken().getOtherClaims().put(IDToken.ACT, act);
    }
}

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.Profile;
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
import org.keycloak.services.Urls;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.TokenExchangeDelegationRequestContext;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.UserSessionManager;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.AdminPermissions;

/**
 *
 * @author rmartinc
 */
public class TokenExchangeDelegationProvider extends StandardTokenExchangeProvider {

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
    protected void validateSubjectToken(AccessToken subjectToken) {
        // Delegation legitimately exchanges tokens carrying "may_act", and chained delegation ones carrying "act". The allowed actor is validated separately in validateDelegation() using the actor_token. So permit these tokens here, overriding the standard rejection of delegation subject tokens.
        Map<String, Object> claims = subjectToken.getOtherClaims();
        if (claims.containsKey(IDToken.MAY_ACT) && claims.containsKey(IDToken.ACT)) {
            // "may_act" authorizes the initial consented hop and "act" every hop after it, so the two never combine
            throw delegationRejected("The subject_token cannot carry both a may_act and an act claim");
        }
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

        boolean isClientDelegation = actorUser.getServiceAccountClientLink() != null;
        if (isClientDelegation) {
            event.detail(Details.ACTOR_TYPE, Details.ACTOR_TYPE_CLIENT);
            event.detail(Details.ACTOR, actorAccessToken.getIssuedFor());
        } else {
            event.detail(Details.ACTOR_TYPE, Details.ACTOR_TYPE_USER);
            event.detail(Details.ACTOR, actorUser.getUsername());
        }
        event.detail(Details.ACTOR_ID, actorUser.getId());
        if (actorAccessToken.getSessionId() != null) {
            event.detail(Details.ACTOR_SESSION_ID, actorSession.getId());
        }

        validateSenderConstrainedToken(actorAccessToken);
        if (!client.equals(realm.getClientByClientId(actorAccessToken.getIssuedFor()))) {
            forbiddenIfClientIsNotWithinTokenAudience(actorAccessToken);
        }

        // validate the delegation itself, either against the "may_act" claim or as a continuation of an existing chain
        TokenExchangeDelegationRequestContext delegationContext = createDelegationContext(isClientDelegation);
        triggerDelegationRequestEvent(delegationContext);
        validateDelegation(delegationContext, subjectUser, actorUser);

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

    private TokenExchangeDelegationRequestContext createDelegationContext(boolean isClientDelegation) {
        boolean mayActPresent = subjectAccessToken.getOtherClaims().containsKey(IDToken.MAY_ACT);
        // the "may_act" path keeps nesting the actor token's own chain, a chained request nests the subject token's one
        Object priorAct = mayActPresent
                ? actorAccessToken.getOtherClaims().get(IDToken.ACT)
                : subjectAccessToken.getOtherClaims().get(IDToken.ACT);

        // an "act" verified against this realm's keys is one we issued, which holds while subject_issuer and
        // requested_issuer are rejected for both standard and delegation exchange
        return new TokenExchangeDelegationRequestContext(context, isClientDelegation, mayActPresent,
                DelegationChain.actors(priorAct));
    }

    private void triggerDelegationRequestEvent(TokenExchangeDelegationRequestContext delegationContext) {
        try {
            session.clientPolicy().triggerOnEvent(delegationContext);
        } catch (ClientPolicyException cpe) {
            event.detail(Details.REASON, Details.CLIENT_POLICY_ERROR);
            event.detail(Details.CLIENT_POLICY_ERROR, cpe.getError());
            event.detail(Details.CLIENT_POLICY_ERROR_DETAIL, cpe.getErrorDetail());
            event.error(cpe.getError());
            throw new CorsErrorResponseException(cors, cpe.getError(), cpe.getErrorDetail(), cpe.getErrorStatus());
        }
    }

    private void validateDelegation(TokenExchangeDelegationRequestContext delegationContext, UserModel subjectUser, UserModel actorUser) {
        int depth = delegationContext.getChainDepth();
        if (depth > 1 && !delegationContext.isChainingAllowed()) {
            throw delegationRejected("Chaining an already delegated token is not allowed");
        }

        if (depth > DelegationChain.MAX_CHAIN_DEPTH) {
            throw delegationRejected("Delegation chain depth " + depth + " exceeds the maximum of " + DelegationChain.MAX_CHAIN_DEPTH);
        }

        validatePriorActors(delegationContext, actorUser);

        if (subjectUser.getId().equals(actorUser.getId())) {
            throw delegationRejected("Actor and subject user cannot be the same user");
        }

        if (delegationContext.isMayActPresent()) {
            validateMayAct(actorUser);
        } else {
            validateChainedDelegation(delegationContext, actorUser);
        }

        // "delegate" is a live rule, so it is re-checked here instead of trusted from the claim that recorded it
        requireDelegatePermission(subjectUser, actorUser);

        event.detail(Details.DELEGATION_CHAIN_DEPTH, String.valueOf(depth));
        if (depth > 1) {
            event.detail(Details.DELEGATION_CHAIN, String.join(",", actorChain(delegationContext, actorUser)));
        }
    }

    /**
     * Checks the shape of a chain this hop inherits, which is the same whichever claim authorizes the hop itself.
     */
    private void validatePriorActors(TokenExchangeDelegationRequestContext delegationContext, UserModel actorUser) {
        List<DelegationActor> priorActors = delegationContext.getPriorActors();
        if (priorActors.isEmpty()) {
            return;
        }

        if (!DelegationChain.allActorsHaveClientId(priorActors)) {
            throw delegationRejected("Only a token delegated to a client can be chained, this one was delegated to a user");
        }
        // an actor taking part twice is a delegation loop
        if (DelegationChain.containsActor(priorActors, actorUser.getId())) {
            throw delegationRejected("Actor is already part of the delegation chain in the subject_token");
        }
    }

    /**
     * Authorizes a hop that has no "may_act" claim to rely on, which means the subject token has to be a delegated
     * token continuing a chain the user consented to when it started.
     */
    private void validateChainedDelegation(TokenExchangeDelegationRequestContext delegationContext, UserModel actorUser) {
        if (delegationContext.getChainDepth() == 1) {
            throw delegationRejected("The subject_token carries no may_act claim, so the user did not consent to this delegation");
        }

        // a human actor here would be impersonation without the user ever consenting to it
        if (!delegationContext.isClientDelegation()) {
            throw delegationRejected("Only a service account actor can delegate without a may_act claim in the subject_token");
        }

        // no "may_act" means no client_id to bind the request to, so the actor token must belong to the caller
        if (!client.getClientId().equals(actorAccessToken.getIssuedFor())) {
            throw delegationRejected("Requesting client does not match the client the actor_token was issued for");
        }

        // "azp" only proves who the token was issued to, so the service account acted as must be the caller's own
        if (!client.getId().equals(actorUser.getServiceAccountClientLink())) {
            throw delegationRejected("Actor token subject is not the service account of the requesting client");
        }
    }

    /**
     * The "delegate" permission is what the user consented to at the first hop, so it is re-checked on every hop.
     * Revoking it takes effect at once rather than when the subject token expires. It is never configurable off.
     */
    private void requireDelegatePermission(UserModel subjectUser, UserModel actorUser) {
        if (!Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ_V2)) {
            throw delegationRejected("Token exchange delegation requires fine-grained admin permissions version 2");
        }

        AdminPermissionEvaluator evaluator = AdminPermissions.evaluator(session, realm, realm, actorUser);
        if (!evaluator.users().canDelegate(subjectUser)) {
            throw delegationRejected("Actor is not allowed to delegate as the subject user");
        }
    }

    private List<String> actorChain(TokenExchangeDelegationRequestContext delegationContext, UserModel actorUser) {
        List<DelegationActor> priorActors = new ArrayList<>(delegationContext.getPriorActors());
        Collections.reverse(priorActors);

        List<String> actors = new ArrayList<>(priorActors.size() + 1);
        priorActors.forEach(actor -> actors.add(actor.getIdentifier()));
        actors.add(delegationContext.isClientDelegation() ? actorAccessToken.getIssuedFor() : actorUser.getUsername());
        return actors;
    }

    private CorsErrorResponseException delegationRejected(String reason) {
        event.detail(Details.REASON, reason);
        event.error(Errors.INVALID_TOKEN);
        return new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, reason, Response.Status.BAD_REQUEST);
    }

    protected void validateMayAct(UserModel actorUser) {
        Object mayActObject = subjectAccessToken.getOtherClaims().get(IDToken.MAY_ACT);
        if (!(mayActObject instanceof Map mayActMap)) {
            throw delegationRejected("Invalid may_act claim in the subject_token");
        }

        Object issObject = mayActMap.get(OIDCLoginProtocol.ISSUER);
        if (issObject != null && !issObject.equals(Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()))) {
            throw delegationRejected("Invalid issuer in the may_act claim of the subject_token");
        }

        Object subjectObject = mayActMap.get(JsonWebToken.SUBJECT);
        if (!(subjectObject instanceof String subject)) {
            throw delegationRejected("Invalid may_act claim in the subject_token");
        }

        if (!actorUser.getId().equals(subject)) {
            throw delegationRejected("Actor user is not allowed by the may_act claim inside the subject_token");
        }

        Object clientIdObject = mayActMap.get(OAuth2Constants.CLIENT_ID);
        // an absent client_id means no client binding, but a present one must be a string to be checked
        if (clientIdObject != null) {
            if (!(clientIdObject instanceof String clientId)) {
                throw delegationRejected("Invalid may_act claim in the subject_token");
            }
            if (!clientId.equals(actorAccessToken.getIssuedFor())) {
                throw delegationRejected("Actor token client does not match the client_id in the may_act claim");
            }
            if (!clientId.equals(client.getClientId())) {
                throw delegationRejected("Requesting client does not match the client_id in the may_act claim");
            }
        }
    }

    @Override
    protected void checkRequestedAudiences(TokenManager.AccessTokenResponseBuilder responseBuilder) {
        // the next hop is named in the "audience" parameter, bounded by the ordinary scope and role resolution
        super.checkRequestedAudiences(responseBuilder);

        // a delegated token records the chain in "act", a "may_act" alongside it would make the next hop ambiguous
        responseBuilder.getAccessToken().getOtherClaims().remove(IDToken.MAY_ACT);
        responseBuilder.getAccessToken().getOtherClaims().put(IDToken.ACT, buildActClaim());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildActClaim() {
        Object mayActObject = subjectAccessToken.getOtherClaims().get(IDToken.MAY_ACT);
        if (mayActObject instanceof Map) {
            // add the "act" claim using the "may_act" claim sent in the subjectToken, chain current actor if present
            Map<String, Object> act = new HashMap<>((Map<String, Object>) mayActObject);
            Object prevAct = actorAccessToken.getOtherClaims().get(IDToken.ACT);
            if (prevAct != null) {
                act.put(IDToken.ACT, prevAct);
            }
            return act;
        }

        // chained or consent-free hop, so describe the actor here and nest the subject token's chain below it as
        // required by RFC 8693 section 4.1
        Map<String, Object> act = new LinkedHashMap<>();
        act.put(JsonWebToken.SUBJECT, actorAccessToken.getSubject());
        act.put(OAuth2Constants.CLIENT_ID, actorAccessToken.getIssuedFor());
        Object prevAct = subjectAccessToken.getOtherClaims().get(IDToken.ACT);
        if (prevAct != null) {
            act.put(IDToken.ACT, prevAct);
        }
        return act;
    }
}

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

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.*;
import org.keycloak.protocol.oidc.*;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.utils.AssertionGrantUtils;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.TokenUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OAuth 2.0 JWT Bearer grant type (RFC 7523)
 *
 * @author Ben Cresitello-Dittmar
 */
public class JWTBearerAssertionGrantType extends OAuth2GrantTypeBase {
    private List<CrossDomainTrust> trustedIssuers;

    @Override
    public Response process(Context context) {
        setContext(context);

        event.detail(Details.AUTH_METHOD, "jwt_bearer");
        event.client(client);

        this.trustedIssuers = AssertionGrantUtils.getTrustedIssuerConfigs(realm, client);

        checkAssertionGrantEnabled();
        checkTrustedIssuerConfig();
        checkConfidentialClient();

        return authenticateWithAssertion();
    }

    /**
     * Authenticate a user with a JWT bearer assertion
     *
     * @return The token endpoint response
     */
    protected Response authenticateWithAssertion() {
        String assertion = formParams.getFirst(OAuth2Constants.ASSERTION);
        checkAssertion(assertion);

        // validate assertion sig
        JsonWebToken jwt = validateAssertion(assertion);

        UserModel requestedUser = getUser(jwt);
        checkImpersonationAllowed(requestedUser);

        // log user in (transient session)
        UserSessionModel userSession = session.sessions().createUserSession(null, realm, requestedUser, requestedUser.getUsername(), clientConnection.getRemoteAddr(), "impersonate", false, null, null, UserSessionModel.SessionPersistenceState.TRANSIENT);
        event.session(userSession);

        setAssertionInUserSessionNotes(jwt, userSession);

        AuthenticationSessionModel authSession = createAuthSession(requestedUser);
        updateUserSessionFromClientAuth(userSession, clientAuthAttributes);

        // generate token response
        AccessTokenResponse res = createAccessTokenResponse(userSession, authSession);

        event.success();

        return cors.add(Response.ok(res, MediaType.APPLICATION_JSON_TYPE));
    }

    /**
     * Ensure the client has the assertion grant type enabled
     * Throws an HTTP forbidden error if the client does not have the assertion grant type enabled
     */
    public void checkAssertionGrantEnabled() {
        if (!AssertionGrantUtils.isOIDCAssertionGrantEnabled(client)){
            event.detail(Details.REASON, "assertion grant not allowed by client");
            event.error(Errors.ACCESS_DENIED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.ACCESS_DENIED, "Client not allowed to perform assertion grant", Response.Status.FORBIDDEN);
        };
    }

    /**
     * Validate the client used in the request was authenticated.
     * Throws an HTTP forbidden error if the client was not authenticated.
     */
    private void checkConfidentialClient(){
        if (client.isPublicClient()) {
            event.detail(Details.REASON, "public clients not allowed");
            event.error(Errors.ACCESS_DENIED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.ACCESS_DENIED, "Client not allowed to perform assertion grant", Response.Status.FORBIDDEN);
        }
    }

    /**
     * Get the encoded assertion from the request.
     * Throws an HTTP bad request error if the assertion was not present.
     *
     * @return The encoded assertion JWT
     */
    private void checkAssertion(String assertion){
        // ensure assertion was present in the request
        if (assertion == null) {
            event.detail(Details.REASON, "no assertion provided in request");
            event.error(Errors.INVALID_REQUEST);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Invalid request, no assertion found in request", Response.Status.BAD_REQUEST);
        }
    }

    /**
     * Get the trusted issuer configuration from the client. These will be used to validate the assertion.
     * Throws an HTTP internal server error if the client configurations could not be retrieved.
     */
    private void checkTrustedIssuerConfig(){
        // ensure the trusted issuer configs could be parsed
        if (trustedIssuers == null){
            event.detail(Details.REASON, "Unable to parse client assertion grant configuration");
            event.error(Errors.INVALID_CONFIG);
            throw new CorsErrorResponseException(cors, OAuthErrorException.SERVER_ERROR, "Invalid request, unable to parse client assertion grant configuration", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Verify the client has permissions to impersonate the user specified in the assertion JWT
     * Throws an HTTP forbidden error if the client is not allowed to impersonate the user
     *
     * @param user The user in question
     */
    private void checkImpersonationAllowed(UserModel user){
        // check client can impersonate user
        if (!AdminPermissions.management(session, realm).users().canClientImpersonate(client, user)) {
            event.detail(Details.REASON, "client not allowed to impersonate");
            event.error(Errors.ACCESS_DENIED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.ACCESS_DENIED, "Client not allowed to perform assertion grant", Response.Status.FORBIDDEN);
        }
    }

    /**
     * Validate the encoded assertion JWT against the cross-domain trusted issuers.
     * Performs the following checks:
     *      - ensure JWT is active
     *      - validate the signature against trusted issuers
     *      - ensure the issuer and audience in the token match the specified issuer and audience in the trusted issuer configuration that validated
     *        the JWT signature.
     * This function throws an HTTP forbidden error if the assertion is not valid
     *
     * @param assertion The encoded and signed assertion JWT
     * @return The parsed assertion JWT
     */
    private JsonWebToken validateAssertion(String assertion){
        JsonWebToken jws = null;

        // set in the verification loop
        boolean valid = false;
        List<String> validationErrors = new ArrayList<>();

        if (trustedIssuers.isEmpty()){
            validationErrors.add(String.format("Client %s has no configured trusted issuers", client.getClientId()));
        }

        // loop through each config to see if it can verify the assertion
        for (CrossDomainTrust config: trustedIssuers) {
            try {
                // verify the token signature and check the issuer and audience
                TokenVerifier<JsonWebToken> verifier = TokenVerifier.create(assertion, JsonWebToken.class)
                        .withChecks(
                                TokenVerifier.IS_ACTIVE,
                                new TokenVerifier.AudienceCheck(config.getAudience()),
                                new TokenVerifier.IssuerCheck(config.getIssuer()))
                        .publicKey(config.getPublicKey())
                        .verify();

                jws = verifier.getToken();
                valid = true;
            } catch (VerificationException ex) {
                validationErrors.add(String.format("(%s) %s", config.getIssuer(), ex.getMessage()));
            }
        }

        // check if validation failed
        if (!valid){
            event.detail(Details.REASON, "Failed to verify assertion: " + validationErrors);
            event.error(Errors.ACCESS_DENIED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.ACCESS_DENIED, "Invalid request, failed to verify provided assertion", Response.Status.FORBIDDEN);
        }

        return jws;
    }

    /**
     * Get the user object associated with the subject of the assertion JWT.
     * Throws an HTTP bad request error if the 'sub' claim is not present in the assertion or the user does not exist
     *
     * @param jwt The decoded assertion JWT
     * @return The user specified in the assertion JWT 'sub' claim
     */
    private UserModel getUser(JsonWebToken jwt){
        String requestedSubject = jwt.getSubject();

        // check assertion has a 'sub' specified
        if (requestedSubject == null) {
            event.detail(Details.REASON, "No subject found in assertion grant");
            event.error(Errors.INVALID_REQUEST);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Invalid assertion", Response.Status.BAD_REQUEST);
        }

        event.detail(Details.REQUESTED_SUBJECT, requestedSubject);

        // fetch user by username or ID
        UserModel requestedUser = session.users().getUserByUsername(realm, requestedSubject);
        if (requestedUser == null) {
            requestedUser = session.users().getUserById(realm, requestedSubject);
        }

        // validate user exists
        if (requestedUser == null) {
            event.detail(Details.REASON, "User not found");
            event.error(Errors.INVALID_REQUEST);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Invalid assertion", Response.Status.BAD_REQUEST);
        }

        return requestedUser;
    }

    /**
     * Set the assertion JWT as a JSON string in the user session notes so the assertion contents can be used in the
     * access token mappers during access token generation.
     * Throws an HTTP internal server error if the assertion cannot be serialized to a JSON string
     *
     * @param jwt The assertion JWT
     * @param userSession The user session to add the assertion JSON to
     */
    private void setAssertionInUserSessionNotes(JsonWebToken jwt, UserSessionModel userSession){
        try {
            // serialize the assertion JWT
            String jsonResult = JsonSerialization.writeValueAsString(jwt);
            userSession.setNote(Constants.OAUTH_ASSERTION_GRANT_NOTE, jsonResult);
        } catch (IOException ex){
            event.detail(Details.REASON, "Unable to serialize assertion");
            event.error(Errors.INVALID_REQUEST);
            throw new CorsErrorResponseException(cors, OAuthErrorException.SERVER_ERROR, "Invalid assertion", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Create an authentication session for the user
     *
     * @param user The user the authentication session is for
     * @return The authentication session for the user
     */
    private AuthenticationSessionModel createAuthSession(UserModel user){
        RootAuthenticationSessionModel rootAuthSession = new AuthenticationSessionManager(session).createAuthenticationSession(realm, false);
        AuthenticationSessionModel authSession = rootAuthSession.createAuthenticationSession(client);

        authSession.setAuthenticatedUser(user);
        authSession.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        authSession.setClientNote(OIDCLoginProtocol.ISSUER, Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()));
        authSession.setClientNote(OIDCLoginProtocol.SCOPE_PARAM, formParams.getFirst(OAuth2Constants.SCOPE));

        AuthenticationManager.setClientScopesInSession(authSession);

        return authSession;
    }

    /**
     * Build an access token response with the given user session and authentication session
     *
     * @param userSession The user session for the user authenticated as part of the assertion grant flow
     * @param authSession The authentication session for the user
     * @return The access token response
     */
    private AccessTokenResponse createAccessTokenResponse(UserSessionModel userSession, AuthenticationSessionModel authSession){
        ClientSessionContext clientSessionCtx = TokenManager.attachAuthenticationSession(this.session, userSession, authSession);

        // generate access token
        TokenManager.AccessTokenResponseBuilder responseBuilder = tokenManager.responseBuilder(realm, client, event, this.session, userSession, clientSessionCtx)
                .generateAccessToken();
        responseBuilder.getAccessToken().issuedFor(client.getClientId());

        // generate id token
        String scopeParam = clientSessionCtx.getClientSession().getNote(OAuth2Constants.SCOPE);
        if (TokenUtil.isOIDCRequest(scopeParam)) {
            responseBuilder.generateIDToken().generateAccessTokenHash();
        }

        return responseBuilder.build();
    }

    /**
     * Helper to update the user session with the client auth attributes
     *
     * @param userSession The user session
     * @param clientAuthAttributes The client auth attributes
     */
    private void updateUserSessionFromClientAuth(UserSessionModel userSession, Map<String, String> clientAuthAttributes) {
        for (Map.Entry<String, String> attr : clientAuthAttributes.entrySet()) {
            userSession.setNote(attr.getKey(), attr.getValue());
        }
    }

    @Override
    public EventType getEventType() {
        return EventType.JWT_BEARER;
    }

}

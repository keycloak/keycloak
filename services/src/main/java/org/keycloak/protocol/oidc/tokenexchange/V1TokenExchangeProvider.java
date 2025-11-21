/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Base64Url;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.LoginProtocolFactory;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.TokenExchangeContext;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.endpoints.TokenEndpoint;
import org.keycloak.protocol.saml.SamlClient;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.SamlService;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.managers.UserSessionManager;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.fgap.AdminPermissions;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.util.TokenUtil;

import org.jboss.logging.Logger;

import static org.keycloak.models.ImpersonationSessionNote.IMPERSONATOR_CLIENT;
import static org.keycloak.models.ImpersonationSessionNote.IMPERSONATOR_ID;
import static org.keycloak.models.ImpersonationSessionNote.IMPERSONATOR_USERNAME;

/**
 * V1 token exchange provider. Supports all token exchange types (standard, federated, subject impersonation)
 *
 * @author <a href="mailto:dmitryt@backbase.com">Dmitry Telegin</a>
 */
public class V1TokenExchangeProvider extends AbstractTokenExchangeProvider {

    private static final Logger logger = Logger.getLogger(V1TokenExchangeProvider.class);

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public boolean supports(TokenExchangeContext context) {
        return true;
    }

    protected Response tokenExchange() {
        KeycloakSession session = context.getSession();
        RealmModel realm = context.getRealm();
        ClientConnection clientConnection = context.getClientConnection();
        Cors cors = context.getCors();
        ClientModel client = context.getClient();
        EventBuilder event = context.getEvent();

        UserModel tokenUser = null;
        UserSessionModel tokenSession = null;
        AccessToken token = null;

        String subjectToken = context.getParams().getSubjectToken();
        if (subjectToken != null) {
            String subjectTokenType = context.getParams().getSubjectTokenType();
            if (isExternalInternalTokenExchangeRequest(this.context)) {
                String subjectIssuer = getSubjectIssuer(this.context, subjectToken, subjectTokenType);
                return exchangeExternalToken(subjectIssuer, subjectToken);
            }

            if (subjectTokenType != null && !subjectTokenType.equals(OAuth2Constants.ACCESS_TOKEN_TYPE)) {
                event.detail(Details.REASON, "subject_token supports access tokens only");
                event.error(Errors.INVALID_TOKEN);
                throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Invalid token type, must be access token", Response.Status.BAD_REQUEST);

            }

            AuthenticationManager.AuthResult authResult = AuthenticationManager.verifyIdentityToken(session, realm, session.getContext().getUri(), clientConnection, true, true, null,
                    false, subjectToken, context.getHeaders(), verifier -> {});
            if (authResult == null) {
                event.detail(Details.REASON, "subject_token validation failure");
                event.error(Errors.INVALID_TOKEN);
                throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Invalid token", Response.Status.BAD_REQUEST);
            }

            tokenUser = authResult.user();
            tokenSession = authResult.session();
            token = authResult.token();
        }

        String requestedSubject = context.getFormParams().getFirst(OAuth2Constants.REQUESTED_SUBJECT);
        boolean disallowOnHolderOfTokenMismatch = true;

        if (requestedSubject != null) {
            event.detail(Details.REQUESTED_SUBJECT, requestedSubject);
            UserModel requestedUser = session.users().getUserByUsername(realm, requestedSubject);
            if (requestedUser == null) {
                requestedUser = session.users().getUserById(realm, requestedSubject);
            }

            if (requestedUser == null) {
                // We always returned access denied to avoid username fishing
                event.detail(Details.REASON, "requested_subject not found");
                event.error(Errors.NOT_ALLOWED);
                throw new CorsErrorResponseException(cors, OAuthErrorException.ACCESS_DENIED, "Client not allowed to exchange", Response.Status.FORBIDDEN);

            }

            if (token != null) {
                event.detail(Details.IMPERSONATOR, tokenUser.getUsername());
                // for this case, the user represented by the token, must have permission to impersonate.
                AdminAuth auth = new AdminAuth(realm, token, tokenUser, client);
                if (!AdminPermissions.evaluator(session, realm, auth).users().canImpersonate(requestedUser, client)) {
                    event.detail(Details.REASON, "subject not allowed to impersonate");
                    event.error(Errors.NOT_ALLOWED);
                    throw new CorsErrorResponseException(cors, OAuthErrorException.ACCESS_DENIED, "Client not allowed to exchange", Response.Status.FORBIDDEN);
                }
            } else {
                // no token is being exchanged, this is a direct exchange.  Client must be authenticated, not public, and must be allowed
                // to impersonate
                if (client.isPublicClient()) {
                    event.detail(Details.REASON, "public clients not allowed");
                    event.error(Errors.NOT_ALLOWED);
                    throw new CorsErrorResponseException(cors, OAuthErrorException.ACCESS_DENIED, "Client not allowed to exchange", Response.Status.FORBIDDEN);

                }
                if (!AdminPermissions.management(session, realm).users().canClientImpersonate(client, requestedUser)) {
                    event.detail(Details.REASON, "client not allowed to impersonate");
                    event.error(Errors.NOT_ALLOWED);
                    throw new CorsErrorResponseException(cors, OAuthErrorException.ACCESS_DENIED, "Client not allowed to exchange", Response.Status.FORBIDDEN);
                }

                // see https://issues.redhat.com/browse/KEYCLOAK-5492
                disallowOnHolderOfTokenMismatch = false;
            }

            tokenSession = new UserSessionManager(session).createUserSession(realm, requestedUser, requestedUser.getUsername(), clientConnection.getRemoteHost(), "impersonate", false, null, null);
            if (tokenUser != null) {
                tokenSession.setNote(IMPERSONATOR_ID.toString(), tokenUser.getId());
                tokenSession.setNote(IMPERSONATOR_USERNAME.toString(), tokenUser.getUsername());
            }

            tokenUser = requestedUser;
        }

        String requestedIssuer = context.getFormParams().getFirst(OAuth2Constants.REQUESTED_ISSUER);
        if (requestedIssuer == null) {
            return exchangeClientToClient(tokenUser, tokenSession, token, disallowOnHolderOfTokenMismatch);
        } else {
            try {
                return exchangeToIdentityProvider(tokenUser, tokenSession, requestedIssuer);
            } finally {
                if (subjectToken == null) { // we are naked! So need to clean up user session
                    try {
                        session.sessions().removeUserSession(realm, tokenSession);
                    } catch (Exception ignore) {

                    }
                }
            }
        }
    }

    @Override
    protected String getRequestedTokenType() {
        String requestedTokenType = context.getParams().getRequestedTokenType();
        if (requestedTokenType == null) {
            requestedTokenType = OAuth2Constants.REFRESH_TOKEN_TYPE;
            return requestedTokenType;
        }
        if (requestedTokenType.equals(OAuth2Constants.ACCESS_TOKEN_TYPE)
                || requestedTokenType.equals(OAuth2Constants.REFRESH_TOKEN_TYPE)
                || requestedTokenType.equals(OAuth2Constants.SAML2_TOKEN_TYPE)) {
            return requestedTokenType;
        }

        event.detail(Details.REASON, "requested_token_type unsupported");
        event.error(Errors.INVALID_REQUEST);
        throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "requested_token_type unsupported", Response.Status.BAD_REQUEST);
    }

    @Override
    protected void validateAudience(AccessToken token, boolean disallowOnHolderOfTokenMismatch, List<ClientModel> targetAudienceClients) {
        ClientModel tokenHolder = token == null ? null : realm.getClientByClientId(token.getIssuedFor());
        for (ClientModel targetClient : targetAudienceClients) {
            if (targetClient.isConsentRequired()) {
                event.detail(Details.REASON, "audience requires consent");
                event.detail(Details.AUDIENCE, targetClient.getClientId());
                event.error(Errors.CONSENT_DENIED);
                throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_CLIENT, "Client requires user consent", Response.Status.BAD_REQUEST);
            }
            if (!targetClient.isEnabled()) {
                event.detail(Details.REASON, "audience client disabled");
                event.detail(Details.AUDIENCE, targetClient.getClientId());
                event.error(Errors.CLIENT_DISABLED);
                throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_CLIENT, "Client disabled", Response.Status.BAD_REQUEST);
            }
            boolean isClientTheAudience = targetClient.equals(client);
            if (isClientTheAudience) {
                if (client.isPublicClient()) {
                    // public clients can only exchange on to themselves if they are the token holder
                    forbiddenIfClientIsNotTokenHolder(disallowOnHolderOfTokenMismatch, tokenHolder);
                } else if (!client.equals(tokenHolder)) {
                    // confidential clients can only exchange to themselves if they are within the token audience
                    forbiddenIfClientIsNotWithinTokenAudience(token);
                }
            } else {
                if (client.isPublicClient()) {
                    // public clients can not exchange tokens from other client
                    forbiddenIfClientIsNotTokenHolder(disallowOnHolderOfTokenMismatch, tokenHolder);
                }
                if (!AdminPermissions.management(session, realm).clients().canExchangeTo(client, targetClient, token)) {
                    event.detail(Details.REASON, "client not allowed to exchange to audience");
                    event.detail(Details.AUDIENCE, targetClient.getClientId());
                    event.error(Errors.NOT_ALLOWED);
                    throw new CorsErrorResponseException(cors, OAuthErrorException.ACCESS_DENIED, "Client not allowed to exchange", Response.Status.FORBIDDEN);
                }
            }
        }
    }

    @Override
    protected List<String> getSupportedOAuthResponseTokenTypes() {
        return Arrays.asList(OAuth2Constants.ACCESS_TOKEN_TYPE, OAuth2Constants.REFRESH_TOKEN_TYPE);
    }

    @Override
    protected String getRequestedScope(AccessToken token, List<ClientModel> targetAudienceClients) {
        ClientModel targetClient = targetAudienceClients.get(0);
        // TODO Remove once more audiences are properly supported
        if (targetAudienceClients.size() > 1) {
            logger.warnf("Only one value of audience parameter currently supported for token exchange. Using audience '%s' and ignoring the other audiences provided", targetClient.getClientId());
        }

        String scope = formParams.getFirst(OAuth2Constants.SCOPE);
        if (token != null && token.getScope() != null && scope == null) {
            scope = token.getScope();

            Set<String> targetClientScopes = new HashSet<String>();
            targetClientScopes.addAll(targetClient.getClientScopes(true).keySet());
            targetClientScopes.addAll(targetClient.getClientScopes(false).keySet());
            //from return scope remove scopes that are not default or optional scopes for targetClient
            scope = Arrays.stream(scope.split(" ")).filter(s -> "openid".equals(s) || (targetClientScopes.contains(Profile.isFeatureEnabled(Profile.Feature.DYNAMIC_SCOPES) ? s.split(":")[0] : s))).collect(Collectors.joining(" "));
        } else if (token != null && token.getScope() != null) {
            String subjectTokenScopes = token.getScope();
            if (Profile.isFeatureEnabled(Profile.Feature.DYNAMIC_SCOPES)) {
                Set<String> subjectTokenScopesSet = Arrays.stream(subjectTokenScopes.split(" ")).map(s -> s.split(":")[0]).collect(Collectors.toSet());
                scope = Arrays.stream(scope.split(" ")).filter(sc -> subjectTokenScopesSet.contains(sc.split(":")[0])).collect(Collectors.joining(" "));
            } else {
                Set<String> subjectTokenScopesSet = Arrays.stream(subjectTokenScopes.split(" ")).collect(Collectors.toSet());
                scope = Arrays.stream(scope.split(" ")).filter(sc -> subjectTokenScopesSet.contains(sc)).collect(Collectors.joining(" "));
            }

            Set<String> targetClientScopes = new HashSet<String>();
            targetClientScopes.addAll(targetClient.getClientScopes(true).keySet());
            targetClientScopes.addAll(targetClient.getClientScopes(false).keySet());
            //from return scope remove scopes that are not default or optional scopes for targetClient
            scope = Arrays.stream(scope.split(" ")).filter(s -> "openid".equals(s) || (targetClientScopes.contains(Profile.isFeatureEnabled(Profile.Feature.DYNAMIC_SCOPES) ? s.split(":")[0] : s))).collect(Collectors.joining(" "));
        }
        return scope;
    }

    @Override
    protected void setClientToContext(List<ClientModel> targetAudienceClients) {
        ClientModel targetClient = getTargetClient(targetAudienceClients);
        session.getContext().setClient(targetClient);
    }

    protected ClientModel getTargetClient(List<ClientModel> targetAudienceClients) {
        // Make just first client into consideration in V1
        return targetAudienceClients.get(0);
    }

    @Override
    protected Response exchangeClientToOIDCClient(UserModel targetUser, UserSessionModel targetUserSession, String requestedTokenType,
                                                  List<ClientModel> targetAudienceClients, String scope, AccessToken subjectToken) {
        ClientModel targetClient = getTargetClient(targetAudienceClients);
        RootAuthenticationSessionModel rootAuthSession = new AuthenticationSessionManager(session).createAuthenticationSession(realm, false);
        AuthenticationSessionModel authSession = createSessionModel(targetUserSession, rootAuthSession, targetUser, targetClient, scope);

        AuthenticationManager.setClientScopesInSession(session, authSession);
        ClientSessionContext clientSessionCtx = TokenManager.attachAuthenticationSession(this.session, targetUserSession, authSession);

        if (!AuthenticationManager.isClientSessionValid(realm, client, targetUserSession, targetUserSession.getAuthenticatedClientSessionByClient(client.getId()))) {
            // create the requester client session if needed
            AuthenticationSessionModel clientAuthSession = createSessionModel(targetUserSession, rootAuthSession, targetUser, client, scope);
            TokenManager.attachAuthenticationSession(this.session, targetUserSession, clientAuthSession);
        }

        updateUserSessionFromClientAuth(targetUserSession);

        TokenManager.AccessTokenResponseBuilder responseBuilder = tokenManager.responseBuilder(realm, targetClient, event, this.session, targetUserSession, clientSessionCtx)
                .generateAccessToken();
        responseBuilder.getAccessToken().issuedFor(client.getClientId());

        if (targetClient != null && !targetClient.equals(client)) {
            responseBuilder.getAccessToken().addAudience(targetClient.getClientId());
        }

        if (formParams.containsKey(OAuth2Constants.REQUESTED_SUBJECT)) {
            // if "impersonation", store the client that originated the impersonated user session
            targetUserSession.setNote(IMPERSONATOR_CLIENT.toString(), client.getId());
        }

        if (targetUserSession.getPersistenceState() == UserSessionModel.SessionPersistenceState.TRANSIENT) {
            responseBuilder.getAccessToken().setSessionId(null);
        }

        String issuedTokenType;
        if (requestedTokenType.equals(OAuth2Constants.REFRESH_TOKEN_TYPE)
                && OIDCAdvancedConfigWrapper.fromClientModel(client).isUseRefreshToken()
                && targetUserSession.getPersistenceState() != UserSessionModel.SessionPersistenceState.TRANSIENT) {
            responseBuilder.generateRefreshToken();
            responseBuilder.getRefreshToken().issuedFor(client.getClientId());
            issuedTokenType = OAuth2Constants.REFRESH_TOKEN_TYPE;
        } else {
            issuedTokenType = OAuth2Constants.ACCESS_TOKEN_TYPE;
        }

        String scopeParam = clientSessionCtx.getClientSession().getNote(OAuth2Constants.SCOPE);
        if (TokenUtil.isOIDCRequest(scopeParam)) {
            responseBuilder.generateIDToken().generateAccessTokenHash();
        }

        AccessTokenResponse res = responseBuilder.build();
        res.setOtherClaims(OAuth2Constants.ISSUED_TOKEN_TYPE, issuedTokenType);

        event.detail(Details.AUDIENCE, targetClient.getClientId())
                .user(targetUser);

        event.success();

        return cors.add(Response.ok(res, MediaType.APPLICATION_JSON_TYPE));
    }

    @Override
    protected Response exchangeClientToSAML2Client(UserModel targetUser, UserSessionModel targetUserSession, String requestedTokenType, List<ClientModel> targetAudienceClients) {
        ClientModel targetClient = getTargetClient(targetAudienceClients);

        // Create authSession with target SAML 2.0 client and authenticated user
        LoginProtocolFactory factory = (LoginProtocolFactory) session.getKeycloakSessionFactory()
                .getProviderFactory(LoginProtocol.class, SamlProtocol.LOGIN_PROTOCOL);
        SamlService samlService = (SamlService) factory.createProtocolEndpoint(session, event);
        AuthenticationSessionModel authSession = samlService.getOrCreateLoginSessionForIdpInitiatedSso(session, realm,
                targetClient, null);
        if (authSession == null) {
            logger.error("SAML assertion consumer url not set up");
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_CLIENT, "Client requires assertion consumer url set up", Response.Status.BAD_REQUEST);
        }

        authSession.setAuthenticatedUser(targetUser);

        event.session(targetUserSession);

        AuthenticationManager.setClientScopesInSession(session, authSession);
        ClientSessionContext clientSessionCtx = TokenManager.attachAuthenticationSession(this.session, targetUserSession,
                authSession);

        updateUserSessionFromClientAuth(targetUserSession);

        // Create SAML 2.0 Assertion Response
        SamlClient samlClient = new SamlClient(targetClient);
        SamlProtocol samlProtocol = new TokenEndpoint.TokenExchangeSamlProtocol(samlClient).setEventBuilder(event).setHttpHeaders(headers).setRealm(realm)
                .setSession(session).setUriInfo(session.getContext().getUri());

        Response samlAssertion = samlProtocol.authenticated(authSession, targetUserSession, clientSessionCtx);
        if (samlAssertion.getStatus() != 200) {
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Can not get SAML 2.0 token", Response.Status.BAD_REQUEST);
        }
        String xmlString = (String) samlAssertion.getEntity();
        String encodedXML = Base64Url.encode(xmlString.getBytes(GeneralConstants.SAML_CHARSET));

        int assertionLifespan = samlClient.getAssertionLifespan();

        AccessTokenResponse res = new AccessTokenResponse();
        res.setToken(encodedXML);
        res.setTokenType("Bearer");
        res.setExpiresIn(assertionLifespan <= 0 ? realm.getAccessCodeLifespan() : assertionLifespan);
        res.setOtherClaims(OAuth2Constants.ISSUED_TOKEN_TYPE, requestedTokenType);

        event.detail(Details.AUDIENCE, targetClient.getClientId())
                .user(targetUser);

        event.success();

        return cors.add(Response.ok(res, MediaType.APPLICATION_JSON_TYPE));
    }
}

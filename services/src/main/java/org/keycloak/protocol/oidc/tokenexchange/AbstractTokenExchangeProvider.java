/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.ExchangeExternalToken;
import org.keycloak.broker.provider.ExchangeTokenToIdentityProviderToken;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProviderMapper;
import org.keycloak.broker.provider.IdentityProviderMapperSyncModeDelegate;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.Profile;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.common.util.Base64Url;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.light.LightweightUserAdapter;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.LoginProtocolFactory;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.TokenExchangeContext;
import org.keycloak.protocol.oidc.TokenExchangeProvider;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.endpoints.TokenEndpoint.TokenExchangeSamlProtocol;
import org.keycloak.protocol.saml.SamlClient;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.SamlService;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.Urls;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.managers.BruteForceProtector;
import org.keycloak.services.managers.UserSessionManager;
import org.keycloak.services.resources.IdentityBrokerService;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;
import org.keycloak.services.validation.Validation;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.util.TokenUtil;

import static org.keycloak.authentication.authenticators.util.AuthenticatorUtils.getDisabledByBruteForceEventError;
import static org.keycloak.models.ImpersonationSessionNote.IMPERSONATOR_CLIENT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

/**
 * Base token exchange implementation. For now for both V1 and V2 token exchange (may change in the follow-up commits)
 *
 * @author <a href="mailto:dmitryt@backbase.com">Dmitry Telegin</a>
 */
public abstract class AbstractTokenExchangeProvider implements TokenExchangeProvider {

    private static final Logger logger = Logger.getLogger(AbstractTokenExchangeProvider.class);

    protected TokenExchangeContext.Params params;
    protected MultivaluedMap<String, String> formParams;
    protected KeycloakSession session;
    protected Cors cors;
    protected RealmModel realm;
    protected ClientModel client;
    protected EventBuilder event;
    protected ClientConnection clientConnection;
    protected HttpHeaders headers;
    protected TokenManager tokenManager;
    protected Map<String, String> clientAuthAttributes;
    protected TokenExchangeContext context;

    @Override
    public Response exchange(TokenExchangeContext context) {
        this.params = context.getParams();
        this.formParams = context.getFormParams();
        this.session = context.getSession();
        this.cors = context.getCors();
        this.realm = context.getRealm();
        this.client = context.getClient();
        this.event = context.getEvent();
        this.clientConnection = context.getClientConnection();
        this.headers = context.getHeaders();
        this.tokenManager = (TokenManager)context.getTokenManager();
        this.clientAuthAttributes = context.getClientAuthAttributes();
        this.context = context;
        return tokenExchange();
    }

    @Override
    public void close() {
    }

    protected abstract Response tokenExchange();

    /**
     * Is it the request for external-internal token exchange?
     */
    protected boolean isExternalInternalTokenExchangeRequest(TokenExchangeContext context) {
        String subjectToken = context.getParams().getSubjectToken();
        KeycloakSession session = context.getSession();
        RealmModel realm = context.getRealm();
        EventBuilder event = context.getEvent();

        if (subjectToken != null) {
            String subjectTokenType = context.getParams().getSubjectTokenType();
            String realmIssuerUrl = Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName());
            String subjectIssuer = getSubjectIssuer(context, subjectToken, subjectTokenType);

            if (subjectIssuer != null && !realmIssuerUrl.equals(subjectIssuer)) {
                event.detail(OAuth2Constants.SUBJECT_ISSUER, subjectIssuer);
                return true;
            }
        }
        return false;
    }

    protected String getSubjectIssuer(TokenExchangeContext context, String subjectToken, String subjectTokenType) {
        String subjectIssuer = context.getFormParams().getFirst(OAuth2Constants.SUBJECT_ISSUER);
        if (subjectIssuer != null) return subjectIssuer;

        if (OAuth2Constants.JWT_TOKEN_TYPE.equals(subjectTokenType)) {
            try {
                JWSInput jws = new JWSInput(subjectToken);
                JsonWebToken jwt = jws.readJsonContent(JsonWebToken.class);
                return jwt.getIssuer();
            } catch (JWSInputException e) {
                context.getEvent().detail(Details.REASON, "unable to parse jwt subject_token");
                context.getEvent().error(Errors.INVALID_TOKEN);
                throw new CorsErrorResponseException(context.getCors(), OAuthErrorException.INVALID_REQUEST, "Invalid token type, must be access token", Response.Status.BAD_REQUEST);
            }
        } else {
            return null;
        }
    }

    protected Response exchangeToIdentityProvider(UserModel targetUser, UserSessionModel targetUserSession, String requestedIssuer) {
        event.detail(Details.REQUESTED_ISSUER, requestedIssuer);
        IdentityProviderModel providerModel = session.identityProviders().getByAlias(requestedIssuer);
        if (providerModel == null) {
            event.detail(Details.REASON, "unknown requested_issuer");
            event.error(Errors.UNKNOWN_IDENTITY_PROVIDER);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Invalid issuer", Response.Status.BAD_REQUEST);
        }

        IdentityProvider<?> provider = IdentityBrokerService.getIdentityProvider(session, requestedIssuer);
        if (!(provider instanceof ExchangeTokenToIdentityProviderToken)) {
            event.detail(Details.REASON, "exchange unsupported by requested_issuer");
            event.error(Errors.UNKNOWN_IDENTITY_PROVIDER);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Issuer does not support token exchange", Response.Status.BAD_REQUEST);
        }
        if (!AdminPermissions.management(session, realm).idps().canExchangeTo(client, providerModel)) {
            event.detail(Details.REASON, "client not allowed to exchange for requested_issuer");
            event.error(Errors.NOT_ALLOWED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.ACCESS_DENIED, "Client not allowed to exchange", Response.Status.FORBIDDEN);
        }
        Response response = ((ExchangeTokenToIdentityProviderToken)provider).exchangeFromToken(session.getContext().getUri(), event, client, targetUserSession, targetUser, formParams);
        return cors.add(Response.fromResponse(response));

    }

    protected String getRequestedTokenType() {
        String requestedTokenType = formParams.getFirst(OAuth2Constants.REQUESTED_TOKEN_TYPE);
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

    protected List<ClientModel> getTargetAudienceClients() {
        List<String> audienceParams = params.getAudience();
        List<ClientModel> targetAudienceClients = new ArrayList<>();
        if (audienceParams != null) {
            for (String audience : audienceParams) {
                ClientModel targetClient = realm.getClientByClientId(audience);
                if (targetClient == null) {
                    event.detail(Details.REASON, "audience not found");
                    event.detail(Details.AUDIENCE, audience);
                    event.error(Errors.CLIENT_NOT_FOUND);
                    throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_CLIENT, "Audience not found", Response.Status.BAD_REQUEST);
                } else {
                    targetAudienceClients.add(targetClient);
                }
            }
        }
        // Assume client itself is audience in case audience parameter not provided
        if (targetAudienceClients.isEmpty()) {
            targetAudienceClients.add(client);
        }
        return targetAudienceClients;
    }

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

    protected Response exchangeClientToClient(UserModel targetUser, UserSessionModel targetUserSession,
            AccessToken token, boolean disallowOnHolderOfTokenMismatch) {

        String requestedTokenType = getRequestedTokenType();
        event.detail(Details.REQUESTED_TOKEN_TYPE, requestedTokenType);
        List<ClientModel> targetAudienceClients = getTargetAudienceClients();
        validateAudience(token, disallowOnHolderOfTokenMismatch, targetAudienceClients);
        String scope = getRequestedScope(token, targetAudienceClients);

        try {
            setClientToContext(targetAudienceClients);
            if (getSupportedOAuthResponseTokenTypes().contains(requestedTokenType))
                return exchangeClientToOIDCClient(targetUser, targetUserSession, requestedTokenType, targetAudienceClients, scope, token);
            else if (OAuth2Constants.SAML2_TOKEN_TYPE.equals(requestedTokenType)) {
                return exchangeClientToSAML2Client(targetUser, targetUserSession, requestedTokenType, targetAudienceClients);
            }
        } finally {
            session.getContext().setClient(client);
        }

        throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "requested_token_type unsupported", Response.Status.BAD_REQUEST);
    }

    protected void forbiddenIfClientIsNotWithinTokenAudience(AccessToken token) {
        if (token != null && !token.hasAudience(client.getClientId())) {
            event.detail(Details.REASON, "client is not within the token audience");
            event.error(Errors.NOT_ALLOWED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.ACCESS_DENIED, "Client is not within the token audience", Response.Status.FORBIDDEN);
        }
    }

    protected void forbiddenIfClientIsNotTokenHolder(boolean disallowOnHolderOfTokenMismatch, ClientModel tokenHolder) {
        if (disallowOnHolderOfTokenMismatch && !client.equals(tokenHolder)) {
            event.detail(Details.REASON, "client is not the token holder");
            event.error(Errors.NOT_ALLOWED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.ACCESS_DENIED, "Client is not the holder of the token", Response.Status.FORBIDDEN);
        }
    }

    protected List<String> getSupportedOAuthResponseTokenTypes() {
        return Arrays.asList(OAuth2Constants.ACCESS_TOKEN_TYPE, OAuth2Constants.REFRESH_TOKEN_TYPE);
    }

    protected AuthenticationSessionModel createSessionModel(UserSessionModel targetUserSession, RootAuthenticationSessionModel rootAuthSession, UserModel targetUser, ClientModel client, String scope) {
        AuthenticationSessionModel authSession = rootAuthSession.createAuthenticationSession(client);
        authSession.setAuthenticatedUser(targetUser);
        authSession.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        authSession.setClientNote(OIDCLoginProtocol.ISSUER, Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()));
        authSession.setClientNote(OIDCLoginProtocol.SCOPE_PARAM, scope);
        return authSession;
    }

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

    protected void setClientToContext(List<ClientModel> targetAudienceClients) {
        ClientModel targetClient = getTargetClient(targetAudienceClients);
        session.getContext().setClient(targetClient);
    }

    protected ClientModel getTargetClient(List<ClientModel> targetAudienceClients) {
        // Make just first client into consideration TODO: Move this method to V1 only as V2 will properly support more audiences
        return targetAudienceClients.get(0);
    }

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
        SamlProtocol samlProtocol = new TokenExchangeSamlProtocol(samlClient).setEventBuilder(event).setHttpHeaders(headers).setRealm(realm)
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

    protected Response exchangeExternalToken(String subjectIssuer, String subjectToken) {
        // try to find the IDP whose alias matches the issuer or the subject issuer in the form params.
        ExternalExchangeContext externalExchangeContext = this.locateExchangeExternalTokenByAlias(subjectIssuer);

        if (externalExchangeContext == null) {
            event.error(Errors.INVALID_ISSUER);
            throw new CorsErrorResponseException(cors, Errors.INVALID_ISSUER, "Invalid " + OAuth2Constants.SUBJECT_ISSUER + " parameter", Response.Status.BAD_REQUEST);
        }
        if (!AdminPermissions.management(session, realm).idps().canExchangeTo(client, externalExchangeContext.idpModel())) {
            event.detail(Details.REASON, "client not allowed to exchange subject_issuer");
            event.error(Errors.NOT_ALLOWED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.ACCESS_DENIED, "Client not allowed to exchange", Response.Status.FORBIDDEN);
        }
        BrokeredIdentityContext context = externalExchangeContext.provider().exchangeExternal(event, formParams);
        if (context == null) {
            event.error(Errors.INVALID_ISSUER);
            throw new CorsErrorResponseException(cors, Errors.INVALID_ISSUER, "Invalid " + OAuth2Constants.SUBJECT_ISSUER + " parameter", Response.Status.BAD_REQUEST);
        }

        UserModel user = importUserFromExternalIdentity(context);

        UserSessionModel userSession = new UserSessionManager(session).createUserSession(realm, user, user.getUsername(), clientConnection.getRemoteHost(), "external-exchange", false, null, null);
        externalExchangeContext.provider().exchangeExternalComplete(userSession, context, formParams);

        // this must exist so that we can obtain access token from user session if idp's store tokens is off
        userSession.setNote(IdentityProvider.EXTERNAL_IDENTITY_PROVIDER, externalExchangeContext.idpModel().getAlias());
        userSession.setNote(IdentityProvider.FEDERATED_ACCESS_TOKEN, subjectToken);

        context.addSessionNotesToUserSession(userSession);

        return exchangeClientToClient(user, userSession, null, false);
    }

    protected UserModel importUserFromExternalIdentity(BrokeredIdentityContext context) {
        IdentityProviderModel identityProviderConfig = context.getIdpConfig();

        String providerId = identityProviderConfig.getAlias();

        context.getIdp().preprocessFederatedIdentity(session, realm, context);
        Set<IdentityProviderMapperModel> mappers = session.identityProviders().getMappersByAliasStream(context.getIdpConfig().getAlias())
                .collect(Collectors.toSet());
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        for (IdentityProviderMapperModel mapper : mappers) {
            IdentityProviderMapper target = (IdentityProviderMapper)sessionFactory.getProviderFactory(IdentityProviderMapper.class, mapper.getIdentityProviderMapper());
            target.preprocessFederatedIdentity(session, realm, mapper, context);
        }

        UserModel user = null;
        if (! context.getIdpConfig().isTransientUsers()) {
            FederatedIdentityModel federatedIdentityModel = new FederatedIdentityModel(providerId, context.getId(),
                    context.getUsername(), context.getToken());

            user = this.session.users().getUserByFederatedIdentity(realm, federatedIdentityModel);
        }

        if (user == null || context.getIdpConfig().isTransientUsers()) {

            logger.debugf("Federated user not found for provider '%s' and broker username '%s'.", providerId, context.getUsername());

            String username = context.getModelUsername();
            if (username == null) {
                if (this.realm.isRegistrationEmailAsUsername() && !Validation.isBlank(context.getEmail())) {
                    username = context.getEmail();
                } else if (context.getUsername() == null) {
                    username = context.getIdpConfig().getAlias() + "." + context.getId();
                } else {
                    username = context.getUsername();
                }
            }
            username = username.trim();
            context.setModelUsername(username);
            if (context.getEmail() != null && !realm.isDuplicateEmailsAllowed()) {
                UserModel existingUser = session.users().getUserByEmail(realm, context.getEmail());
                if (existingUser != null) {
                    event.error(Errors.FEDERATED_IDENTITY_EXISTS);
                    throw new CorsErrorResponseException(cors, Errors.INVALID_TOKEN, "User already exists", Response.Status.BAD_REQUEST);
                }
            }

            UserModel existingUser = session.users().getUserByUsername(realm, username);
            if (existingUser != null) {
                event.error(Errors.FEDERATED_IDENTITY_EXISTS);
                throw new CorsErrorResponseException(cors, Errors.INVALID_TOKEN, "User already exists", Response.Status.BAD_REQUEST);
            }

            if (context.getIdpConfig().isTransientUsers()) {
                String authSessionId = context.getAuthenticationSession() != null && context.getAuthenticationSession().getParentSession() != null
                                       ? context.getAuthenticationSession().getParentSession().getId() : null;
                user = new LightweightUserAdapter(session, realm, authSessionId);
            } else {
                user = session.users().addUser(realm, username);
            }
            user.setEnabled(true);
            user.setEmail(context.getEmail());
            user.setFirstName(context.getFirstName());
            user.setLastName(context.getLastName());


            if (! context.getIdpConfig().isTransientUsers()) {
                FederatedIdentityModel federatedIdentityModel = new FederatedIdentityModel(context.getIdpConfig().getAlias(), context.getId(),
                        context.getModelUsername(), context.getToken());
                session.users().addFederatedIdentity(realm, user, federatedIdentityModel);
            }

            context.getIdp().importNewUser(session, realm, user, context);

            for (IdentityProviderMapperModel mapper : mappers) {
                IdentityProviderMapper target = (IdentityProviderMapper)sessionFactory.getProviderFactory(IdentityProviderMapper.class, mapper.getIdentityProviderMapper());
                target.importNewUser(session, realm, user, mapper, context);
            }

            if (context.getIdpConfig().isTrustEmail() && !Validation.isBlank(user.getEmail())) {
                logger.debugf("Email verified automatically after registration of user '%s' through Identity provider '%s' ", user.getUsername(), context.getIdpConfig().getAlias());
                user.setEmailVerified(true);
            }

            event.clone()
                    .event(EventType.REGISTER)
                    .user(user.getId())
                    .detail(Details.REGISTER_METHOD, "token-exchange")
                    .detail(Details.EMAIL, user.getEmail())
                    .detail(Details.IDENTITY_PROVIDER, providerId)
                    .success();
        } else {
            if (!user.isEnabled()) {
                event.error(Errors.USER_DISABLED);
                throw new CorsErrorResponseException(cors, Errors.INVALID_TOKEN, "Invalid Token", Response.Status.BAD_REQUEST);
            }

            String bruteForceError = getDisabledByBruteForceEventError(session.getProvider(BruteForceProtector.class), session, realm, user);
            if (bruteForceError != null) {
                event.error(bruteForceError);
                throw new CorsErrorResponseException(cors, Errors.INVALID_TOKEN, "Invalid Token", Response.Status.BAD_REQUEST);
            }

            context.getIdp().updateBrokeredUser(session, realm, user, context);

            for (IdentityProviderMapperModel mapper : mappers) {
                IdentityProviderMapper target = (IdentityProviderMapper)sessionFactory.getProviderFactory(IdentityProviderMapper.class, mapper.getIdentityProviderMapper());
                IdentityProviderMapperSyncModeDelegate.delegateUpdateBrokeredUser(session, realm, user, mapper, context, target);
            }
        }

        // make sure user attributes are updated based on attributes set to the context
        for (Map.Entry<String, List<String>> attr : context.getAttributes().entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            if (!UserModel.USERNAME.equalsIgnoreCase(attr.getKey())) {
                user.setAttribute(attr.getKey(), attr.getValue());
            }
        }

        return user;
    }

    // TODO: move to utility class
    protected void updateUserSessionFromClientAuth(UserSessionModel userSession) {
        for (Map.Entry<String, String> attr : clientAuthAttributes.entrySet()) {
            userSession.setNote(attr.getKey(), attr.getValue());
        }
    }

    record ExternalExchangeContext (ExchangeExternalToken provider, IdentityProviderModel idpModel) {};

    private ExternalExchangeContext locateExchangeExternalTokenByAlias(String alias) {
        try {
            IdentityProvider<?> idp = IdentityBrokerService.getIdentityProvider(session, alias);

            if (idp instanceof ExchangeExternalToken external) {
                IdentityProviderModel model = session.identityProviders().getByAlias(alias);
                return new ExternalExchangeContext(external, model);
            }
        } catch (IdentityBrokerException ignore) {
        }

        return session.identityProviders().getAllStream().map(idpModel -> {
            IdentityProvider<?> idp = IdentityBrokerService.getIdentityProvider(session, idpModel.getAlias());

            if (idp instanceof ExchangeExternalToken external && external.isIssuer(alias, formParams)) {
                return new ExternalExchangeContext(external, idpModel);
            }

            return null;
        }).filter(Objects::nonNull).findFirst().orElse(null);
    }

}

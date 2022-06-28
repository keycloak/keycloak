/*
 *  Copyright 2021 Red Hat, Inc. and/or its affiliates
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
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.keycloak.protocol.oidc;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.ExchangeExternalToken;
import org.keycloak.broker.provider.ExchangeTokenToIdentityProviderToken;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.broker.provider.IdentityProviderMapper;
import org.keycloak.broker.provider.IdentityProviderMapperSyncModeDelegate;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.common.util.Base64Url;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
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
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.LoginProtocolFactory;
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
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.managers.BruteForceProtector;
import org.keycloak.services.resources.Cors;
import org.keycloak.services.resources.IdentityBrokerService;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;
import org.keycloak.services.validation.Validation;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.util.TokenUtil;

import static org.keycloak.authentication.authenticators.util.AuthenticatorUtils.getDisabledByBruteForceEventError;
import static org.keycloak.models.ImpersonationSessionNote.IMPERSONATOR_ID;
import static org.keycloak.models.ImpersonationSessionNote.IMPERSONATOR_USERNAME;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

/**
 * Default token exchange implementation
 *
 * @author <a href="mailto:dmitryt@backbase.com">Dmitry Telegin</a>
 */
public class DefaultTokenExchangeProvider implements TokenExchangeProvider {

    private static final Logger logger = Logger.getLogger(DefaultTokenExchangeProvider.class);

    private MultivaluedMap<String, String> formParams;
    private KeycloakSession session;
    private Cors cors;
    private RealmModel realm;
    private ClientModel client;
    private EventBuilder event;
    private ClientConnection clientConnection;
    private HttpHeaders headers;
    private TokenManager tokenManager;
    private Map<String, String> clientAuthAttributes;

    @Override
    public boolean supports(TokenExchangeContext context) {
        return true;
    }

    @Override
    public Response exchange(TokenExchangeContext context) {
        this.formParams = context.getFormParams();
        this.session = context.getSession();
        this.cors = (Cors)context.getCors();
        this.realm = context.getRealm();
        this.client = context.getClient();
        this.event = context.getEvent();
        this.clientConnection = context.getClientConnection();
        this.headers = context.getHeaders();
        this.tokenManager = (TokenManager)context.getTokenManager();
        this.clientAuthAttributes = context.getClientAuthAttributes();
        return tokenExchange();
    }

    @Override
    public void close() {
    }

    protected Response tokenExchange() {

        UserModel tokenUser = null;
        UserSessionModel tokenSession = null;
        AccessToken token = null;

        String subjectToken = formParams.getFirst(OAuth2Constants.SUBJECT_TOKEN);
        if (subjectToken != null) {
            String subjectTokenType = formParams.getFirst(OAuth2Constants.SUBJECT_TOKEN_TYPE);
            String realmIssuerUrl = Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName());
            String subjectIssuer = formParams.getFirst(OAuth2Constants.SUBJECT_ISSUER);

            if (subjectIssuer == null && OAuth2Constants.JWT_TOKEN_TYPE.equals(subjectTokenType)) {
                try {
                    JWSInput jws = new JWSInput(subjectToken);
                    JsonWebToken jwt = jws.readJsonContent(JsonWebToken.class);
                    subjectIssuer = jwt.getIssuer();
                } catch (JWSInputException e) {
                    event.detail(Details.REASON, "unable to parse jwt subject_token");
                    event.error(Errors.INVALID_TOKEN);
                    throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_TOKEN, "Invalid token type, must be access token", Response.Status.BAD_REQUEST);

                }
            }

            if (subjectIssuer != null && !realmIssuerUrl.equals(subjectIssuer)) {
                event.detail(OAuth2Constants.SUBJECT_ISSUER, subjectIssuer);
                return exchangeExternalToken(subjectIssuer, subjectToken);

            }

            if (subjectTokenType != null && !subjectTokenType.equals(OAuth2Constants.ACCESS_TOKEN_TYPE)) {
                event.detail(Details.REASON, "subject_token supports access tokens only");
                event.error(Errors.INVALID_TOKEN);
                throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_TOKEN, "Invalid token type, must be access token", Response.Status.BAD_REQUEST);

            }

            AuthenticationManager.AuthResult authResult = AuthenticationManager.verifyIdentityToken(session, realm, session.getContext().getUri(), clientConnection, true, true, null, false, subjectToken, headers);
            if (authResult == null) {
                event.detail(Details.REASON, "subject_token validation failure");
                event.error(Errors.INVALID_TOKEN);
                throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_TOKEN, "Invalid token", Response.Status.BAD_REQUEST);
            }

            tokenUser = authResult.getUser();
            tokenSession = authResult.getSession();
            token = authResult.getToken();
        }

        String requestedSubject = formParams.getFirst(OAuth2Constants.REQUESTED_SUBJECT);
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

            tokenSession = session.sessions().createUserSession(realm, requestedUser, requestedUser.getUsername(), clientConnection.getRemoteAddr(), "impersonate", false, null, null);
            if (tokenUser != null) {
                tokenSession.setNote(IMPERSONATOR_ID.toString(), tokenUser.getId());
                tokenSession.setNote(IMPERSONATOR_USERNAME.toString(), tokenUser.getUsername());
            }

            tokenUser = requestedUser;
        }

        String requestedIssuer = formParams.getFirst(OAuth2Constants.REQUESTED_ISSUER);
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

    protected Response exchangeToIdentityProvider(UserModel targetUser, UserSessionModel targetUserSession, String requestedIssuer) {
        event.detail(Details.REQUESTED_ISSUER, requestedIssuer);
        IdentityProviderModel providerModel = realm.getIdentityProviderByAlias(requestedIssuer);
        if (providerModel == null) {
            event.detail(Details.REASON, "unknown requested_issuer");
            event.error(Errors.UNKNOWN_IDENTITY_PROVIDER);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Invalid issuer", Response.Status.BAD_REQUEST);
        }

        IdentityProvider provider = IdentityBrokerService.getIdentityProvider(session, realm, requestedIssuer);
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
        return cors.builder(Response.fromResponse(response)).build();

    }

    protected Response exchangeClientToClient(UserModel targetUser, UserSessionModel targetUserSession,
            AccessToken token, boolean disallowOnHolderOfTokenMismatch) {
        String requestedTokenType = formParams.getFirst(OAuth2Constants.REQUESTED_TOKEN_TYPE);
        if (requestedTokenType == null) {
            requestedTokenType = OAuth2Constants.REFRESH_TOKEN_TYPE;
        } else if (!requestedTokenType.equals(OAuth2Constants.ACCESS_TOKEN_TYPE) &&
                !requestedTokenType.equals(OAuth2Constants.REFRESH_TOKEN_TYPE) &&
                !requestedTokenType.equals(OAuth2Constants.SAML2_TOKEN_TYPE)) {
            event.detail(Details.REASON, "requested_token_type unsupported");
            event.error(Errors.INVALID_REQUEST);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "requested_token_type unsupported", Response.Status.BAD_REQUEST);

        }

        String audience = formParams.getFirst(OAuth2Constants.AUDIENCE);
        ClientModel tokenHolder = token == null ? null : realm.getClientByClientId(token.getIssuedFor());
        ClientModel targetClient = client;

        if (audience != null) {
            targetClient = realm.getClientByClientId(audience);
            if (targetClient == null) {
                event.detail(Details.REASON, "audience not found");
                event.error(Errors.CLIENT_NOT_FOUND);
                throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_CLIENT, "Audience not found", Response.Status.BAD_REQUEST);

            }
        }

        if (targetClient.isConsentRequired()) {
            event.detail(Details.REASON, "audience requires consent");
            event.error(Errors.CONSENT_DENIED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_CLIENT, "Client requires user consent", Response.Status.BAD_REQUEST);
        }

        boolean isClientTheAudience = client.equals(targetClient);

        if (isClientTheAudience) {
            if (client.isPublicClient()) {
                // public clients can only exchange on to themselves if they are the token holder
                forbiddenIfClientIsNotTokenHolder(disallowOnHolderOfTokenMismatch, tokenHolder);
            } else if (!client.equals(tokenHolder)) {
                // confidential clients can only exchange to themselves if they are within the token audience
                forbiddenIfClientIsNotWithinTokenAudience(token, tokenHolder);
            }
        } else {
            if (client.isPublicClient()) {
                // public clients can not exchange tokens from other client
                forbiddenIfClientIsNotTokenHolder(disallowOnHolderOfTokenMismatch, tokenHolder);
            }
            if (!AdminPermissions.management(session, realm).clients().canExchangeTo(client, targetClient)) {
                event.detail(Details.REASON, "client not allowed to exchange to audience");
                event.error(Errors.NOT_ALLOWED);
                throw new CorsErrorResponseException(cors, OAuthErrorException.ACCESS_DENIED, "Client not allowed to exchange", Response.Status.FORBIDDEN);
            }
        }

        String scope = formParams.getFirst(OAuth2Constants.SCOPE);

        switch (requestedTokenType) {
            case OAuth2Constants.ACCESS_TOKEN_TYPE:
            case OAuth2Constants.REFRESH_TOKEN_TYPE:
                return exchangeClientToOIDCClient(targetUser, targetUserSession, requestedTokenType, targetClient, audience, scope);
            case OAuth2Constants.SAML2_TOKEN_TYPE:
                return exchangeClientToSAML2Client(targetUser, targetUserSession, requestedTokenType, targetClient, audience, scope);
        }

        throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "requested_token_type unsupported", Response.Status.BAD_REQUEST);
    }

    private void forbiddenIfClientIsNotWithinTokenAudience(AccessToken token, ClientModel tokenHolder) {
        if (token != null && !token.hasAudience(client.getClientId())) {
            event.detail(Details.REASON, "client is not within the token audience");
            event.error(Errors.NOT_ALLOWED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.ACCESS_DENIED, "Client is not within the token audience", Response.Status.FORBIDDEN);
        }
    }

    private void forbiddenIfClientIsNotTokenHolder(boolean disallowOnHolderOfTokenMismatch, ClientModel tokenHolder) {
        if (disallowOnHolderOfTokenMismatch && !client.equals(tokenHolder)) {
            event.detail(Details.REASON, "client is not the token holder");
            event.error(Errors.NOT_ALLOWED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.ACCESS_DENIED, "Client is not the holder of the token", Response.Status.FORBIDDEN);
        }
    }

    protected Response exchangeClientToOIDCClient(UserModel targetUser, UserSessionModel targetUserSession, String requestedTokenType,
                                                  ClientModel targetClient, String audience, String scope) {
        RootAuthenticationSessionModel rootAuthSession = new AuthenticationSessionManager(session).createAuthenticationSession(realm, false);
        AuthenticationSessionModel authSession = rootAuthSession.createAuthenticationSession(targetClient);

        authSession.setAuthenticatedUser(targetUser);
        authSession.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        authSession.setClientNote(OIDCLoginProtocol.ISSUER, Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()));
        authSession.setClientNote(OIDCLoginProtocol.SCOPE_PARAM, scope);

        if (targetUserSession == null) {
            // if no session is associated with a subject_token, a stateless session is created to only allow building a token to the audience
            targetUserSession = session.sessions().createUserSession(authSession.getParentSession().getId(), realm, targetUser, targetUser.getUsername(),
                    clientConnection.getRemoteAddr(), ServiceAccountConstants.CLIENT_AUTH, false, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);

        }

        event.session(targetUserSession);

        AuthenticationManager.setClientScopesInSession(authSession);
        ClientSessionContext clientSessionCtx = TokenManager.attachAuthenticationSession(this.session, targetUserSession, authSession);

        updateUserSessionFromClientAuth(targetUserSession);

        TokenManager.AccessTokenResponseBuilder responseBuilder = tokenManager.responseBuilder(realm, targetClient, event, this.session, targetUserSession, clientSessionCtx)
                .generateAccessToken();
        responseBuilder.getAccessToken().issuedFor(client.getClientId());

        if (audience != null) {
            responseBuilder.getAccessToken().addAudience(audience);
        }

        if (requestedTokenType.equals(OAuth2Constants.REFRESH_TOKEN_TYPE)
            && OIDCAdvancedConfigWrapper.fromClientModel(client).isUseRefreshToken()) {
            responseBuilder.generateRefreshToken();
            responseBuilder.getRefreshToken().issuedFor(client.getClientId());
        }

        String scopeParam = clientSessionCtx.getClientSession().getNote(OAuth2Constants.SCOPE);
        if (TokenUtil.isOIDCRequest(scopeParam)) {
            responseBuilder.generateIDToken().generateAccessTokenHash();
        }

        AccessTokenResponse res = responseBuilder.build();
        event.detail(Details.AUDIENCE, targetClient.getClientId());

        event.success();

        return cors.builder(Response.ok(res, MediaType.APPLICATION_JSON_TYPE)).build();
    }

    protected Response exchangeClientToSAML2Client(UserModel targetUser, UserSessionModel targetUserSession, String requestedTokenType,
                                                  ClientModel targetClient, String audience, String scope) {
        // Create authSession with target SAML 2.0 client and authenticated user
        LoginProtocolFactory factory = (LoginProtocolFactory) session.getKeycloakSessionFactory()
                .getProviderFactory(LoginProtocol.class, SamlProtocol.LOGIN_PROTOCOL);
        SamlService samlService = (SamlService) factory.createProtocolEndpoint(realm, event);
        ResteasyProviderFactory.getInstance().injectProperties(samlService);
        AuthenticationSessionModel authSession = samlService.getOrCreateLoginSessionForIdpInitiatedSso(session, realm,
                targetClient, null);
        if (authSession == null) {
            logger.error("SAML assertion consumer url not set up");
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_CLIENT, "Client requires assertion consumer url set up", Response.Status.BAD_REQUEST);
        }

        authSession.setAuthenticatedUser(targetUser);

        event.session(targetUserSession);

        AuthenticationManager.setClientScopesInSession(authSession);
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

        event.detail(Details.AUDIENCE, targetClient.getClientId());
        event.success();

        return cors.builder(Response.ok(res, MediaType.APPLICATION_JSON_TYPE)).build();
    }

    protected Response exchangeExternalToken(String issuer, String subjectToken) {
        AtomicReference<ExchangeExternalToken> externalIdp = new AtomicReference<>(null);
        AtomicReference<IdentityProviderModel> externalIdpModel = new AtomicReference<>(null);

        realm.getIdentityProvidersStream().filter(idpModel -> {
            IdentityProviderFactory factory = IdentityBrokerService.getIdentityProviderFactory(session, idpModel);
            IdentityProvider idp = factory.create(session, idpModel);
            if (idp instanceof ExchangeExternalToken) {
                ExchangeExternalToken external = (ExchangeExternalToken) idp;
                if (idpModel.getAlias().equals(issuer) || external.isIssuer(issuer, formParams)) {
                    externalIdp.set(external);
                    externalIdpModel.set(idpModel);
                    return true;
                }
            }
            return false;
        }).findFirst();


        if (externalIdp.get() == null) {
            event.error(Errors.INVALID_ISSUER);
            throw new CorsErrorResponseException(cors, Errors.INVALID_ISSUER, "Invalid " + OAuth2Constants.SUBJECT_ISSUER + " parameter", Response.Status.BAD_REQUEST);
        }
        if (!AdminPermissions.management(session, realm).idps().canExchangeTo(client, externalIdpModel.get())) {
            event.detail(Details.REASON, "client not allowed to exchange subject_issuer");
            event.error(Errors.NOT_ALLOWED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.ACCESS_DENIED, "Client not allowed to exchange", Response.Status.FORBIDDEN);
        }
        BrokeredIdentityContext context = externalIdp.get().exchangeExternal(event, formParams);
        if (context == null) {
            event.error(Errors.INVALID_ISSUER);
            throw new CorsErrorResponseException(cors, Errors.INVALID_ISSUER, "Invalid " + OAuth2Constants.SUBJECT_ISSUER + " parameter", Response.Status.BAD_REQUEST);
        }

        UserModel user = importUserFromExternalIdentity(context);

        UserSessionModel userSession = session.sessions().createUserSession(realm, user, user.getUsername(), clientConnection.getRemoteAddr(), "external-exchange", false, null, null);
        externalIdp.get().exchangeExternalComplete(userSession, context, formParams);

        // this must exist so that we can obtain access token from user session if idp's store tokens is off
        userSession.setNote(IdentityProvider.EXTERNAL_IDENTITY_PROVIDER, externalIdpModel.get().getAlias());
        userSession.setNote(IdentityProvider.FEDERATED_ACCESS_TOKEN, subjectToken);

        return exchangeClientToClient(user, userSession, null, false);
    }

    protected UserModel importUserFromExternalIdentity(BrokeredIdentityContext context) {
        IdentityProviderModel identityProviderConfig = context.getIdpConfig();

        String providerId = identityProviderConfig.getAlias();

        // do we need this?
        //AuthenticationSessionModel authenticationSession = clientCode.getClientSession();
        //context.setAuthenticationSession(authenticationSession);
        //session.getContext().setClient(authenticationSession.getClient());

        context.getIdp().preprocessFederatedIdentity(session, realm, context);
        Set<IdentityProviderMapperModel> mappers = realm.getIdentityProviderMappersByAliasStream(context.getIdpConfig().getAlias())
                .collect(Collectors.toSet());
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        for (IdentityProviderMapperModel mapper : mappers) {
            IdentityProviderMapper target = (IdentityProviderMapper)sessionFactory.getProviderFactory(IdentityProviderMapper.class, mapper.getIdentityProviderMapper());
            target.preprocessFederatedIdentity(session, realm, mapper, context);
        }

        FederatedIdentityModel federatedIdentityModel = new FederatedIdentityModel(providerId, context.getId(),
                context.getUsername(), context.getToken());

        UserModel user = this.session.users().getUserByFederatedIdentity(realm, federatedIdentityModel);

        if (user == null) {

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


            user = session.users().addUser(realm, username);
            user.setEnabled(true);
            user.setEmail(context.getEmail());
            user.setFirstName(context.getFirstName());
            user.setLastName(context.getLastName());


            federatedIdentityModel = new FederatedIdentityModel(context.getIdpConfig().getAlias(), context.getId(),
                    context.getUsername(), context.getToken());
            session.users().addFederatedIdentity(realm, user, federatedIdentityModel);

            context.getIdp().importNewUser(session, realm, user, context);

            for (IdentityProviderMapperModel mapper : mappers) {
                IdentityProviderMapper target = (IdentityProviderMapper)sessionFactory.getProviderFactory(IdentityProviderMapper.class, mapper.getIdentityProviderMapper());
                target.importNewUser(session, realm, user, mapper, context);
            }

            if (context.getIdpConfig().isTrustEmail() && !Validation.isBlank(user.getEmail())) {
                logger.debugf("Email verified automatically after registration of user '%s' through Identity provider '%s' ", user.getUsername(), context.getIdpConfig().getAlias());
                user.setEmailVerified(true);
            }
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
        return user;
    }

    // TODO: move to utility class
    private void updateUserSessionFromClientAuth(UserSessionModel userSession) {
        for (Map.Entry<String, String> attr : clientAuthAttributes.entrySet()) {
            userSession.setNote(attr.getKey(), attr.getValue());
        }
    }

}

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.ExchangeExternalToken;
import org.keycloak.broker.provider.ExchangeTokenToIdentityProviderToken;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProviderMapper;
import org.keycloak.broker.provider.IdentityProviderMapperSyncModeDelegate;
import org.keycloak.broker.provider.UserAuthenticationIdentityProvider;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderQuery;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.light.LightweightUserAdapter;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.TokenExchangeContext;
import org.keycloak.protocol.oidc.TokenExchangeProvider;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.Urls;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.managers.BruteForceProtector;
import org.keycloak.services.managers.UserSessionManager;
import org.keycloak.services.resources.IdentityBrokerService;
import org.keycloak.services.resources.admin.fgap.AdminPermissions;
import org.keycloak.services.validation.Validation;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.util.Booleans;

import org.jboss.logging.Logger;

import static org.keycloak.authentication.authenticators.util.AuthenticatorUtils.getDisabledByBruteForceEventError;
import static org.keycloak.models.IdentityProviderType.EXCHANGE_EXTERNAL_TOKEN;

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

    protected abstract String getRequestedTokenType();

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

    protected abstract void validateAudience(AccessToken token, boolean disallowOnHolderOfTokenMismatch, List<ClientModel> targetAudienceClients);

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

    protected abstract List<String> getSupportedOAuthResponseTokenTypes();

    protected AuthenticationSessionModel createSessionModel(UserSessionModel targetUserSession, RootAuthenticationSessionModel rootAuthSession, UserModel targetUser, ClientModel client, String scope) {
        AuthenticationSessionModel authSession = rootAuthSession.createAuthenticationSession(client);
        authSession.setAuthenticatedUser(targetUser);
        authSession.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        authSession.setClientNote(OIDCLoginProtocol.ISSUER, Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()));
        authSession.setClientNote(OIDCLoginProtocol.SCOPE_PARAM, scope);
        return authSession;
    }

    protected abstract String getRequestedScope(AccessToken token, List<ClientModel> targetAudienceClients);

    protected void setClientToContext(List<ClientModel> targetAudienceClients) {
        // The client requesting exchange is set in the context
        session.getContext().setClient(client);
    }

    protected abstract Response exchangeClientToOIDCClient(UserModel targetUser, UserSessionModel targetUserSession, String requestedTokenType,
                                                  List<ClientModel> targetAudienceClients, String scope, AccessToken subjectToken);

    protected abstract Response exchangeClientToSAML2Client(UserModel targetUser, UserSessionModel targetUserSession, String requestedTokenType, List<ClientModel> targetAudienceClients);

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
        BrokeredIdentityContext context = externalExchangeContext.provider().exchangeExternal(this, this.context);
        if (context == null) {
            event.error(Errors.INVALID_ISSUER);
            throw new CorsErrorResponseException(cors, Errors.INVALID_ISSUER, "Invalid " + OAuth2Constants.SUBJECT_ISSUER + " parameter", Response.Status.BAD_REQUEST);
        }

        UserModel user = importUserFromExternalIdentity(context);

        UserSessionModel userSession = new UserSessionManager(session).createUserSession(realm, user, user.getUsername(), clientConnection.getRemoteHost(), "external-exchange", false, null, null);
        externalExchangeContext.provider().exchangeExternalComplete(userSession, context, formParams);

        // this must exist so that we can obtain access token from user session if idp's store tokens is off
        userSession.setNote(UserAuthenticationIdentityProvider.EXTERNAL_IDENTITY_PROVIDER, externalExchangeContext.idpModel().getAlias());
        userSession.setNote(UserAuthenticationIdentityProvider.FEDERATED_ACCESS_TOKEN, subjectToken);

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
        if (!context.getIdpConfig().isTransientUsers()) {
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

            if (Booleans.isTrue(context.getIdpConfig().isTrustEmail()) && !Validation.isBlank(user.getEmail())) {
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

    protected record ExternalExchangeContext (ExchangeExternalToken provider, IdentityProviderModel idpModel) {};

    protected ExternalExchangeContext locateExchangeExternalTokenByAlias(String alias) {
        try {
            IdentityProvider<?> idp = IdentityBrokerService.getIdentityProvider(session, alias);

            if (idp instanceof ExchangeExternalToken external) {
                IdentityProviderModel model = session.identityProviders().getByAlias(alias);
                return new ExternalExchangeContext(external, model);
            }
        } catch (IdentityBrokerException ignore) {
        }

        return session.identityProviders().getAllStream(IdentityProviderQuery.type(EXCHANGE_EXTERNAL_TOKEN)).map(idpModel -> {
            IdentityProvider<?> idp = IdentityBrokerService.getIdentityProvider(session, idpModel.getAlias());

            if (idp instanceof ExchangeExternalToken external && external.isIssuer(alias, formParams)) {
                return new ExternalExchangeContext(external, idpModel);
            }

            return null;
        }).filter(Objects::nonNull).findFirst().orElse(null);
    }

}

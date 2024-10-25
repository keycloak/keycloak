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
package org.keycloak.services.resources;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;
import org.keycloak.authentication.authenticators.broker.IdpConfirmOverrideLinkAuthenticator;
import org.keycloak.broker.provider.ExchangeTokenToIdentityProviderToken;
import org.keycloak.http.HttpRequest;
import org.keycloak.OAuthErrorException;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.PostBrokerLoginConstants;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.broker.provider.IdentityProviderMapper;
import org.keycloak.broker.provider.IdentityProviderMapperSyncModeDelegate;
import org.keycloak.broker.provider.util.IdentityBrokerState;
import org.keycloak.broker.saml.SAMLEndpoint;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.common.util.Time;
import org.keycloak.dom.saml.v2.protocol.StatusResponseType;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.locale.LocaleSelectorProvider;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.Constants;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.light.LightweightUserAdapter;
import org.keycloak.models.utils.AuthenticationFlowResolver;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.protocol.saml.SamlSessionUtils;
import org.keycloak.protocol.saml.preprocessor.SamlAuthenticationPreprocessor;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.ErrorPageException;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.managers.BruteForceProtector;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.resources.account.AccountConsole;
import org.keycloak.services.util.AuthenticationFlowURLHelper;
import org.keycloak.services.util.BrowserHistoryHelper;
import org.keycloak.services.util.CacheControlUtil;
import org.keycloak.services.util.DefaultClientSessionContext;
import org.keycloak.services.validation.Validation;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.util.JsonSerialization;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p></p>
 *
 * @author Pedro Igor
 */
public class IdentityBrokerService implements IdentityProvider.AuthenticationCallback {

    // Authentication session note, which references identity provider that is currently linked
    private static final String LINKING_IDENTITY_PROVIDER = "LINKING_IDENTITY_PROVIDER";

    private static final Logger logger = Logger.getLogger(IdentityBrokerService.class);

    private final RealmModel realmModel;

    private final KeycloakSession session;

    private final ClientConnection clientConnection;

    private final HttpRequest request;

    private final HttpHeaders headers;

    private EventBuilder event;


    public IdentityBrokerService(KeycloakSession session) {
        this.session = session;
        this.clientConnection= session.getContext().getConnection();
        realmModel = session.getContext().getRealm();
        if (realmModel == null) {
            throw new IllegalArgumentException("Realm can not be null.");
        }
        this.request = session.getContext().getHttpRequest();
        this.headers = session.getContext().getRequestHeaders();
    }

    public void init() {
        this.event = new EventBuilder(realmModel, session, clientConnection).event(EventType.IDENTITY_PROVIDER_LOGIN);
    }

    private void checkRealm() {
        if (!realmModel.isEnabled()) {
            event.error(Errors.REALM_DISABLED);
            throw new ErrorPageException(session, Response.Status.BAD_REQUEST, Messages.REALM_NOT_ENABLED);
        }
    }

    private ClientModel checkClient(String clientId) {
        if (clientId == null) {
            event.error(Errors.INVALID_REQUEST);
            throw new ErrorPageException(session, Response.Status.BAD_REQUEST, Messages.MISSING_PARAMETER, OIDCLoginProtocol.CLIENT_ID_PARAM);
        }

        event.client(clientId);

        ClientModel client = realmModel.getClientByClientId(clientId);
        if (client == null) {
            event.error(Errors.CLIENT_NOT_FOUND);
            throw new ErrorPageException(session, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
        }

        if (!client.isEnabled()) {
            event.error(Errors.CLIENT_DISABLED);
            throw new ErrorPageException(session, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
        }
        return client;

    }

    /**
     * Closes off CORS preflight requests for account linking
     *
     * @param providerAlias
     * @return
     */
    @OPTIONS
    @Path("/{provider_alias}/link")
    public Response clientIntiatedAccountLinkingPreflight(@PathParam("provider_alias") String providerAlias) {
        return Response.status(403).build(); // don't allow preflight
    }


    @GET
    @NoCache
    @Path("/{provider_alias}/link")
    public Response clientInitiatedAccountLinking(@PathParam("provider_alias") String providerAlias,
                                                  @QueryParam("redirect_uri") String redirectUri,
                                                  @QueryParam("client_id") String clientId,
                                                  @QueryParam("nonce") String nonce,
                                                  @QueryParam("hash") String hash
    ) {
        this.event.event(EventType.CLIENT_INITIATED_ACCOUNT_LINKING);
        checkRealm();
        ClientModel client = checkClient(clientId);
        redirectUri = RedirectUtils.verifyRedirectUri(session, redirectUri, client);
        if (redirectUri == null) {
            event.error(Errors.INVALID_REDIRECT_URI);
            throw new ErrorPageException(session, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
        }

        event.detail(Details.REDIRECT_URI, redirectUri);

        if (nonce == null || hash == null) {
            event.error(Errors.INVALID_REDIRECT_URI);
            throw new ErrorPageException(session, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);

        }

        AuthenticationManager.AuthResult cookieResult = AuthenticationManager.authenticateIdentityCookie(session, realmModel, true);
        String errorParam = "link_error";
        if (cookieResult == null) {
            event.error(Errors.NOT_LOGGED_IN);
            UriBuilder builder = UriBuilder.fromUri(redirectUri)
                    .queryParam(errorParam, Errors.NOT_LOGGED_IN)
                    .queryParam("nonce", nonce);

            return Response.status(302).location(builder.build()).build();
        }

        cookieResult.getSession();
        event.session(cookieResult.getSession());
        event.user(cookieResult.getUser());
        event.detail(Details.USERNAME, cookieResult.getUser().getUsername());

        AuthenticatedClientSessionModel clientSession = null;
        for (AuthenticatedClientSessionModel cs : cookieResult.getSession().getAuthenticatedClientSessions().values()) {
            if (cs.getClient().getClientId().equals(clientId)) {
                byte[] decoded = Base64Url.decode(hash);
                MessageDigest md = null;
                try {
                    md = MessageDigest.getInstance("SHA-256");
                } catch (NoSuchAlgorithmException e) {
                    throw new ErrorPageException(session, Response.Status.INTERNAL_SERVER_ERROR, Messages.UNEXPECTED_ERROR_HANDLING_REQUEST);
                }
                String input = nonce + cookieResult.getSession().getId() + clientId + providerAlias;
                byte[] check = md.digest(input.getBytes(StandardCharsets.UTF_8));
                if (MessageDigest.isEqual(decoded, check)) {
                    clientSession = cs;
                    break;
                }
            }
        }
        if (clientSession == null) {
            event.error(Errors.INVALID_TOKEN);
            throw new ErrorPageException(session, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
        }

        event.detail(Details.IDENTITY_PROVIDER, providerAlias);

        ClientModel accountService = this.realmModel.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
        if (!accountService.getId().equals(client.getId())) {
            RoleModel manageAccountRole = accountService.getRole(AccountRoles.MANAGE_ACCOUNT);

            // Ensure user has role and client has "role scope" for this role
            ClientSessionContext ctx = DefaultClientSessionContext.fromClientSessionScopeParameter(clientSession, session);
            Set<RoleModel> userAccountRoles = ctx.getRolesStream().collect(Collectors.toSet());

            if (!userAccountRoles.contains(manageAccountRole)) {
                RoleModel linkRole = accountService.getRole(AccountRoles.MANAGE_ACCOUNT_LINKS);
                if (!userAccountRoles.contains(linkRole)) {
                    event.error(Errors.NOT_ALLOWED);
                    UriBuilder builder = UriBuilder.fromUri(redirectUri)
                            .queryParam(errorParam, Errors.NOT_ALLOWED)
                            .queryParam("nonce", nonce);
                    return Response.status(302).location(builder.build()).build();
                }
            }
        }


        IdentityProviderModel identityProviderModel = session.identityProviders().getByAlias(providerAlias);
        if (identityProviderModel == null) {
            event.error(Errors.UNKNOWN_IDENTITY_PROVIDER);
            UriBuilder builder = UriBuilder.fromUri(redirectUri)
                    .queryParam(errorParam, Errors.UNKNOWN_IDENTITY_PROVIDER)
                    .queryParam("nonce", nonce);
            return Response.status(302).location(builder.build()).build();

        }


        // Create AuthenticationSessionModel with same ID like userSession and refresh cookie
        UserSessionModel userSession = cookieResult.getSession();

        // Auth session with ID corresponding to our userSession may already exists in some rare cases (EG. if some client tried to login in another browser tab with "prompt=login")
        RootAuthenticationSessionModel rootAuthSession = session.authenticationSessions().getRootAuthenticationSession(realmModel, userSession.getId());
        if (rootAuthSession == null) {
            rootAuthSession = session.authenticationSessions().createRootAuthenticationSession(realmModel, userSession.getId());
        }

        AuthenticationSessionModel authSession = rootAuthSession.createAuthenticationSession(client);

        // Refresh the cookie
        new AuthenticationSessionManager(session).setAuthSessionCookie(userSession.getId());

        ClientSessionCode<AuthenticationSessionModel> clientSessionCode = new ClientSessionCode<>(session, realmModel, authSession);
        clientSessionCode.setAction(AuthenticationSessionModel.Action.AUTHENTICATE.name());
        clientSessionCode.getOrGenerateCode();
        authSession.setProtocol(client.getProtocol());
        authSession.setRedirectUri(redirectUri);
        authSession.setClientNote(OIDCLoginProtocol.STATE_PARAM, UUID.randomUUID().toString());
        authSession.setAuthNote(LINKING_IDENTITY_PROVIDER, cookieResult.getSession().getId() + clientId + providerAlias);

        event.detail(Details.CODE_ID, userSession.getId());
        event.success();

        try {
            IdentityProvider<?> identityProvider = getIdentityProvider(session, providerAlias);
            Response response = identityProvider.performLogin(createAuthenticationRequest(identityProvider, providerAlias, clientSessionCode));

            if (response != null) {
                if (isDebugEnabled()) {
                    logger.debugf("Identity provider [%s] is going to send a request [%s].", identityProvider, response);
                }
                return response;
            }
        } catch (IdentityBrokerException e) {
            return redirectToErrorPage(authSession, Response.Status.INTERNAL_SERVER_ERROR, Messages.COULD_NOT_SEND_AUTHENTICATION_REQUEST, e, providerAlias);
        } catch (Exception e) {
            return redirectToErrorPage(authSession, Response.Status.INTERNAL_SERVER_ERROR, Messages.UNEXPECTED_ERROR_HANDLING_REQUEST, e, providerAlias);
        }

        return redirectToErrorPage(authSession, Response.Status.INTERNAL_SERVER_ERROR, Messages.COULD_NOT_PROCEED_WITH_AUTHENTICATION_REQUEST);

    }


    @POST
    @Path("/{provider_alias}/login")
    public Response performPostLogin(@PathParam("provider_alias") String providerAlias,
                                     @QueryParam(LoginActionsService.SESSION_CODE) String code,
                                     @QueryParam(Constants.CLIENT_ID) String clientId,
                                     @QueryParam(Constants.CLIENT_DATA) String clientData,
                                     @QueryParam(Constants.TAB_ID) String tabId,
                                     @QueryParam(OIDCLoginProtocol.LOGIN_HINT_PARAM) String loginHint) {
        return performLogin(providerAlias, code, clientId, tabId, clientData, loginHint);
    }

    @GET
    @NoCache
    @Path("/{provider_alias}/login")
    public Response performLogin(@PathParam("provider_alias") String providerAlias,
                                 @QueryParam(LoginActionsService.SESSION_CODE) String code,
                                 @QueryParam(Constants.CLIENT_ID) String clientId,
                                 @QueryParam(Constants.TAB_ID) String tabId,
                                 @QueryParam(Constants.CLIENT_DATA) String clientData,
                                 @QueryParam(OIDCLoginProtocol.LOGIN_HINT_PARAM) String loginHint) {
        this.event.detail(Details.IDENTITY_PROVIDER, providerAlias);

        if (isDebugEnabled()) {
            logger.debugf("Sending authentication request to identity provider [%s].", providerAlias);
        }

        try {
            AuthenticationSessionModel authSession = parseSessionCode(code, clientId, tabId, clientData);

            ClientSessionCode<AuthenticationSessionModel> clientSessionCode = new ClientSessionCode<>(session, realmModel, authSession);
            clientSessionCode.setAction(AuthenticationSessionModel.Action.AUTHENTICATE.name());
            IdentityProviderModel identityProviderModel = session.identityProviders().getByAlias(providerAlias);
            if (identityProviderModel == null) {
                throw new IdentityBrokerException("Identity Provider [" + providerAlias + "] not found.");
            }
            if (identityProviderModel.isLinkOnly()) {
                throw new IdentityBrokerException("Identity Provider [" + providerAlias + "] is not allowed to perform a login.");
            }
            if (clientSessionCode.getClientSession() != null && loginHint != null) {
                clientSessionCode.getClientSession().setClientNote(OIDCLoginProtocol.LOGIN_HINT_PARAM, loginHint);
            }

            IdentityProviderFactory<?> providerFactory = getIdentityProviderFactory(session, identityProviderModel);

            IdentityProvider<?> identityProvider = providerFactory.create(session, identityProviderModel);

            Response response = identityProvider.performLogin(createAuthenticationRequest(identityProvider, providerAlias, clientSessionCode));

            if (response != null) {
                if (isDebugEnabled()) {
                    logger.debugf("Identity provider [%s] is going to send a request [%s].", identityProvider.getConfig().getAlias(), response);
                }
                return response;
            }
        } catch (WebApplicationException e) {
            return e.getResponse();
        } catch (IdentityBrokerException e) {
            return redirectToErrorPage(Response.Status.BAD_GATEWAY, Messages.COULD_NOT_SEND_AUTHENTICATION_REQUEST, e, providerAlias);
        } catch (Exception e) {
            return redirectToErrorPage(Response.Status.INTERNAL_SERVER_ERROR, Messages.UNEXPECTED_ERROR_HANDLING_REQUEST, e, providerAlias);
        }

        return redirectToErrorPage(Response.Status.INTERNAL_SERVER_ERROR, Messages.COULD_NOT_PROCEED_WITH_AUTHENTICATION_REQUEST);
    }

    @Override
    public Response retryLogin(IdentityProvider<?> identityProvider, AuthenticationSessionModel authSession) {
        ClientSessionCode<AuthenticationSessionModel> clientSessionCode = new ClientSessionCode<>(session, realmModel, authSession);
        clientSessionCode.setAction(AuthenticationSessionModel.Action.AUTHENTICATE.name());
        Response response = identityProvider.performLogin(createAuthenticationRequest(identityProvider, identityProvider.getConfig().getAlias(), clientSessionCode));

        if (response != null) {
            event.detail(Details.IDENTITY_PROVIDER, identityProvider.getConfig().getAlias())
                    .detail(Details.LOGIN_RETRY, "true")
                    .success();

            if (isDebugEnabled()) {
                logger.debugf("Identity provider [%s] is going to retry a login request [%s].", identityProvider.getConfig().getAlias(), response);
            }
            return response;
        }
        return redirectToErrorPage(Response.Status.INTERNAL_SERVER_ERROR, Messages.COULD_NOT_PROCEED_WITH_AUTHENTICATION_REQUEST);
    }

    @Path("{provider_alias}/endpoint")
    public Object getEndpoint(@PathParam("provider_alias") String providerAlias) {
        IdentityProvider identityProvider;

        try {
            identityProvider = getIdentityProvider(session, providerAlias);
        } catch (IdentityBrokerException e) {
            throw new NotFoundException(e.getMessage());
        }

        return identityProvider.callback(realmModel, this, event);
    }

    @Path("{provider_alias}/token")
    @OPTIONS
    public Response retrieveTokenPreflight() {
        return Cors.builder().auth().preflight().add(Response.ok());
    }

    @GET
    @NoCache
    @Path("{provider_alias}/token")
    public Response retrieveToken(@PathParam("provider_alias") String providerAlias) {
        return getToken(providerAlias, false);
    }

    private boolean canReadBrokerToken(AccessToken token) {
        Map<String, AccessToken.Access> resourceAccess = token.getResourceAccess();
        AccessToken.Access brokerRoles = resourceAccess == null ? null : resourceAccess.get(Constants.BROKER_SERVICE_CLIENT_ID);
        return brokerRoles != null && brokerRoles.isUserInRole(Constants.READ_TOKEN_ROLE);
    }

    private Response getToken(String providerAlias, boolean forceRetrieval) {
        this.event.event(EventType.IDENTITY_PROVIDER_RETRIEVE_TOKEN);

        try {
            AuthenticationManager.AuthResult authResult = new AppAuthManager.BearerTokenAuthenticator(session)
                    .setRealm(realmModel)
                    .setConnection(clientConnection)
                    .setHeaders(request.getHttpHeaders())
                    .authenticate();

            if (authResult != null) {
                AccessToken token = authResult.getToken();
                ClientModel clientModel = authResult.getClient();

                session.getContext().setClient(clientModel);

                ClientModel brokerClient = realmModel.getClientByClientId(Constants.BROKER_SERVICE_CLIENT_ID);
                if (brokerClient == null) {
                    return corsResponse(forbidden("Realm has not migrated to support the broker token exchange service"), clientModel);

                }
                if (!canReadBrokerToken(token)) {
                    return corsResponse(forbidden("Client [" + clientModel.getClientId() + "] not authorized to retrieve tokens from identity provider [" + providerAlias + "]."), clientModel);

                }

                IdentityProvider identityProvider = getIdentityProvider(session, providerAlias);
                IdentityProviderModel identityProviderConfig = getIdentityProviderConfig(providerAlias);

                if (identityProviderConfig.isStoreToken()) {
                    FederatedIdentityModel identity = this.session.users().getFederatedIdentity(this.realmModel, authResult.getUser(), providerAlias);

                    if (identity == null) {
                        return corsResponse(badRequest("User [" + authResult.getUser().getId() + "] is not associated with identity provider [" + providerAlias + "]."), clientModel);
                    }

                    if (identity.getToken() == null) {
                        return corsResponse(notFound("No token stored for user [" + authResult.getUser().getId() + "] with associated identity provider [" + providerAlias + "]."), clientModel);
                    }

                    this.event.success();

                    return corsResponse(identityProvider.retrieveToken(session, identity), clientModel);
                }

                return corsResponse(badRequest("Identity Provider [" + providerAlias + "] does not support this operation."), clientModel);
            }

            return badRequest("Invalid token.");
        } catch (IdentityBrokerException e) {
            return redirectToErrorPage(Response.Status.BAD_GATEWAY, Messages.COULD_NOT_OBTAIN_TOKEN, e, providerAlias);
        }  catch (Exception e) {
            return redirectToErrorPage(Response.Status.BAD_GATEWAY, Messages.UNEXPECTED_ERROR_RETRIEVING_TOKEN, e, providerAlias);
        }
    }

    public Response authenticated(BrokeredIdentityContext context) {
        IdentityProviderModel identityProviderConfig = context.getIdpConfig();
        AuthenticationSessionModel authenticationSession = context.getAuthenticationSession();

        String providerAlias = identityProviderConfig.getAlias();
        if (!identityProviderConfig.isStoreToken()) {
            if (isDebugEnabled()) {
                logger.debugf("Token will not be stored for identity provider [%s].", providerAlias);
            }
            context.setToken(null);
        }

        StatusResponseType loginResponse = (StatusResponseType) context.getContextData().get(SAMLEndpoint.SAML_LOGIN_RESPONSE);
        if (loginResponse != null) {
            for(Iterator<SamlAuthenticationPreprocessor> it = SamlSessionUtils.getSamlAuthenticationPreprocessorIterator(session); it.hasNext();) {
                loginResponse = it.next().beforeProcessingLoginResponse(loginResponse, authenticationSession);
            }
        }

        session.getContext().setClient(authenticationSession.getClient());

        context.getIdp().preprocessFederatedIdentity(session, realmModel, context);
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        session.identityProviders().getMappersByAliasStream(context.getIdpConfig().getAlias()).forEach(mapper -> {
            IdentityProviderMapper target = (IdentityProviderMapper) sessionFactory
                    .getProviderFactory(IdentityProviderMapper.class, mapper.getIdentityProviderMapper());
            target.preprocessFederatedIdentity(session, realmModel, mapper, context);
        });

        FederatedIdentityModel federatedIdentityModel = new FederatedIdentityModel(providerAlias, context.getId(),
                context.getUsername(), context.getToken());

        this.event.event(EventType.IDENTITY_PROVIDER_LOGIN)
                .detail(Details.REDIRECT_URI, authenticationSession.getRedirectUri())
                .detail(Details.IDENTITY_PROVIDER, providerAlias)
                .detail(Details.IDENTITY_PROVIDER_USERNAME, context.getUsername());

        UserModel federatedUser = this.session.users().getUserByFederatedIdentity(this.realmModel, federatedIdentityModel);
        boolean shouldMigrateId = false;
        // try to find the user using legacy ID
        if (federatedUser == null && context.getLegacyId() != null) {
            federatedIdentityModel = new FederatedIdentityModel(federatedIdentityModel, context.getLegacyId());
            federatedUser = this.session.users().getUserByFederatedIdentity(this.realmModel, federatedIdentityModel);
            shouldMigrateId = true;
        }

        // Check if federatedUser is already authenticated (this means linking social into existing federatedUser account)
        UserSessionModel userSession = new AuthenticationSessionManager(session).getUserSession(authenticationSession);
        if (shouldPerformAccountLinking(authenticationSession, userSession, providerAlias)) {
            return performAccountLinking(authenticationSession, userSession, context, federatedIdentityModel, federatedUser);
        }

        if (federatedUser == null) {

            logger.debugf("Federated user not found for provider '%s' and broker username '%s'", providerAlias, context.getUsername());
            authenticationSession.setAuthNote(AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME, context.getUsername());

            String username = context.getModelUsername();
            if (username == null) {
                if (this.realmModel.isRegistrationEmailAsUsername() && !Validation.isBlank(context.getEmail())) {
                    username = context.getEmail();
                } else if (context.getUsername() == null) {
                    username = context.getIdpConfig().getAlias() + "." + context.getId();
                } else {
                    username = context.getUsername();
                }
            }
            username = username.trim();
            context.setModelUsername(username);

            SerializedBrokeredIdentityContext ctx0 = SerializedBrokeredIdentityContext.readFromAuthenticationSession(authenticationSession, AbstractIdpAuthenticator.BROKERED_CONTEXT_NOTE);
            if (ctx0 != null) {
                SerializedBrokeredIdentityContext ctx1 = SerializedBrokeredIdentityContext.serialize(context);
                ctx1.saveToAuthenticationSession(authenticationSession, AbstractIdpAuthenticator.NESTED_FIRST_BROKER_CONTEXT);
                logger.warnv("Nested first broker flow detected: {0} -> {1}", ctx0.getIdentityProviderId(), ctx1.getIdentityProviderId());
                logger.debug("Resuming last execution");
                URI redirect = new AuthenticationFlowURLHelper(session, realmModel, session.getContext().getUri())
                    .getLastExecutionUrl(authenticationSession);
                return Response.status(Status.FOUND).location(redirect).build();
            }

            logger.debug("Redirecting to flow for firstBrokerLogin");

            boolean forwardedPassiveLogin = "true".equals(authenticationSession.getAuthNote(AuthenticationProcessor.FORWARDED_PASSIVE_LOGIN));

            String userRequestedLocale = authenticationSession.getAuthNote(LocaleSelectorProvider.USER_REQUEST_LOCALE);
            // Redirect to firstBrokerLogin after successful login and ensure that previous authentication state removed
            AuthenticationProcessor.resetFlow(authenticationSession, LoginActionsService.FIRST_BROKER_LOGIN_PATH);
            if (userRequestedLocale != null) {
                authenticationSession.setAuthNote(LocaleSelectorProvider.USER_REQUEST_LOCALE, userRequestedLocale);
            }

            // Set the FORWARDED_PASSIVE_LOGIN note (if needed) after resetting the session so it is not lost.
            if (forwardedPassiveLogin) {
                authenticationSession.setAuthNote(AuthenticationProcessor.FORWARDED_PASSIVE_LOGIN, "true");
            }

            SerializedBrokeredIdentityContext ctx = SerializedBrokeredIdentityContext.serialize(context);
            ctx.saveToAuthenticationSession(authenticationSession, AbstractIdpAuthenticator.BROKERED_CONTEXT_NOTE);

            URI redirect = LoginActionsService.firstBrokerLoginProcessor(session.getContext().getUri())
                    .queryParam(Constants.CLIENT_ID, authenticationSession.getClient().getClientId())
                    .queryParam(Constants.TAB_ID, authenticationSession.getTabId())
                    .queryParam(Constants.CLIENT_DATA, AuthenticationProcessor.getClientData(session, authenticationSession))
                    .build(realmModel.getName());
            return Response.status(302).location(redirect).build();

        } else {
            authenticationSession.setAuthNote(AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME, federatedUser.getUsername());
            Response response = validateUser(authenticationSession, federatedUser, realmModel);
            if (response != null) {
                return response;
            }

            updateFederatedIdentity(context, federatedUser);
            if (shouldMigrateId) {
                migrateFederatedIdentityId(context, federatedUser);
            }
            authenticationSession.setAuthenticatedUser(federatedUser);

            return finishOrRedirectToPostBrokerLogin(authenticationSession, context, false);
        }
    }


    public Response validateUser(AuthenticationSessionModel authSession, UserModel user, RealmModel realm) {
        if (!user.isEnabled()) {
            event.error(Errors.USER_DISABLED);
            return ErrorPage.error(session, authSession, Response.Status.BAD_REQUEST, Messages.ACCOUNT_DISABLED);
        }
        if (realm.isBruteForceProtected()) {
            if (session.getProvider(BruteForceProtector.class).isTemporarilyDisabled(session, realm, user)) {
                event.error(Errors.USER_TEMPORARILY_DISABLED);
                return ErrorPage.error(session, authSession, Response.Status.BAD_REQUEST, Messages.ACCOUNT_DISABLED);
            }
        }
        return null;
    }

    // Callback from LoginActionsService after first login with broker was done and Keycloak account is successfully linked/created
    @GET
    @NoCache
    @Path("/after-first-broker-login")
    public Response afterFirstBrokerLogin(@QueryParam(LoginActionsService.SESSION_CODE) String code,
                                          @QueryParam(Constants.CLIENT_ID) String clientId,
                                          @QueryParam(Constants.CLIENT_DATA) String clientData,
                                          @QueryParam(Constants.TAB_ID) String tabId) {
        AuthenticationSessionModel authSession = parseSessionCode(code, clientId, tabId, clientData);
        return afterFirstBrokerLogin(authSession);
    }

    private Response afterFirstBrokerLogin(AuthenticationSessionModel authSession) {
        try {
            this.event.detail(Details.CODE_ID, authSession.getParentSession().getId())
                    .removeDetail("auth_method");

            SerializedBrokeredIdentityContext serializedCtx = SerializedBrokeredIdentityContext.readFromAuthenticationSession(authSession, AbstractIdpAuthenticator.BROKERED_CONTEXT_NOTE);
            if (serializedCtx == null) {
                throw new IdentityBrokerException("Not found serialized context in clientSession");
            }
            BrokeredIdentityContext context = serializedCtx.deserialize(session, authSession);
            String providerAlias = context.getIdpConfig().getAlias();

            event.detail(Details.IDENTITY_PROVIDER, providerAlias);
            event.detail(Details.IDENTITY_PROVIDER_USERNAME, context.getUsername());

            // Ensure the first-broker-login flow was successfully finished
            String authProvider = authSession.getAuthNote(AbstractIdpAuthenticator.FIRST_BROKER_LOGIN_SUCCESS);
            if (authProvider == null || !authProvider.equals(providerAlias)) {
                throw new IdentityBrokerException("Invalid request. Not found the flag that first-broker-login flow was finished");
            }

            // firstBrokerLogin workflow finished. Removing note now
            authSession.removeAuthNote(AbstractIdpAuthenticator.BROKERED_CONTEXT_NOTE);

            UserModel federatedUser = authSession.getAuthenticatedUser();
            if (federatedUser == null) {
                throw new IdentityBrokerException("Couldn't found authenticated federatedUser in authentication session");
            }

            event.user(federatedUser);
            event.detail(Details.USERNAME, federatedUser.getUsername());

            if (context.getIdpConfig().isAddReadTokenRoleOnCreate()) {
                ClientModel brokerClient = realmModel.getClientByClientId(Constants.BROKER_SERVICE_CLIENT_ID);
                if (brokerClient == null) {
                    throw new IdentityBrokerException("Client 'broker' not available. Maybe realm has not migrated to support the broker token exchange service");
                }
                RoleModel readTokenRole = brokerClient.getRole(Constants.READ_TOKEN_ROLE);
                federatedUser.grantRole(readTokenRole);
            }

            // Add federated identity link here
            if (!(federatedUser instanceof LightweightUserAdapter)) {
                checkOverrideLink(authSession, federatedUser, providerAlias);

                FederatedIdentityModel federatedIdentityModel = new FederatedIdentityModel(context.getIdpConfig().getAlias(), context.getId(),
                        context.getUsername(), context.getToken());
                session.users().addFederatedIdentity(realmModel, federatedUser, federatedIdentityModel);
            }


            String isRegisteredNewUser = authSession.getAuthNote(AbstractIdpAuthenticator.BROKER_REGISTERED_NEW_USER);
            if (Boolean.parseBoolean(isRegisteredNewUser)) {

                logger.debugf("Registered new user '%s' after first login with identity provider '%s'. Identity provider username is '%s' . ", federatedUser.getUsername(), providerAlias, context.getUsername());

                context.getIdp().importNewUser(session, realmModel, federatedUser, context);
                KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
                session.identityProviders().getMappersByAliasStream(providerAlias).forEach(mapper -> {
                    IdentityProviderMapper target = (IdentityProviderMapper) sessionFactory
                            .getProviderFactory(IdentityProviderMapper.class, mapper.getIdentityProviderMapper());
                    target.importNewUser(session, realmModel, federatedUser, mapper, context);
                });

                if (context.getIdpConfig().isTrustEmail() && !Validation.isBlank(federatedUser.getEmail()) && !Boolean.parseBoolean(authSession.getAuthNote(AbstractIdpAuthenticator.UPDATE_PROFILE_EMAIL_CHANGED))) {
                    logger.debugf("Email verified automatically after registration of user '%s' through Identity provider '%s' ", federatedUser.getUsername(), context.getIdpConfig().getAlias());
                    federatedUser.setEmailVerified(true);
                }

                event.clone()
                        .event(EventType.REGISTER)
                        .detail(Details.REGISTER_METHOD, "broker")
                        .detail(Details.EMAIL, federatedUser.getEmail())
                        .success();

            } else {
                logger.debugf("Linked existing keycloak user '%s' with identity provider '%s' . Identity provider username is '%s' .", federatedUser.getUsername(), providerAlias, context.getUsername());

                event.event(EventType.FEDERATED_IDENTITY_LINK)
                        .success();

                updateFederatedIdentity(context, federatedUser);
            }

            return finishOrRedirectToPostBrokerLogin(authSession, context, true);

        }  catch (Exception e) {
            return redirectToErrorPage(authSession, Response.Status.INTERNAL_SERVER_ERROR, Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR, e);
        }
    }

    private void checkOverrideLink(AuthenticationSessionModel authSession, UserModel federatedUser, String providerAlias) {
        String isOverride = authSession.getAuthNote(IdpConfirmOverrideLinkAuthenticator.OVERRIDE_LINK);
        if (!Boolean.parseBoolean(isOverride)) {
            return;
        }

        FederatedIdentityModel previous = session.users()
                .getFederatedIdentity(realmModel, federatedUser, providerAlias);
        if (previous == null) {
            return;
        }

        session.users().removeFederatedIdentity(realmModel, federatedUser, providerAlias);

        event.clone()
                .event(EventType.FEDERATED_IDENTITY_OVERRIDE_LINK)
                .detail(Details.PREF_PREVIOUS + Details.IDENTITY_PROVIDER_USERNAME, previous.getUserName())
                .success();
    }

    private Response finishOrRedirectToPostBrokerLogin(AuthenticationSessionModel authSession, BrokeredIdentityContext context, boolean wasFirstBrokerLogin) {
        String postBrokerLoginFlowId = context.getIdpConfig().getPostBrokerLoginFlowId();
        if (postBrokerLoginFlowId == null) {

            logger.debugf("Skip redirect to postBrokerLogin flow. PostBrokerLogin flow not set for identityProvider '%s'.", context.getIdpConfig().getAlias());
            return afterPostBrokerLoginFlowSuccess(authSession, context, wasFirstBrokerLogin);
        } else {

            logger.debugf("Redirect to postBrokerLogin flow after authentication with identityProvider '%s'.", context.getIdpConfig().getAlias());

            authSession.getParentSession().setTimestamp(Time.currentTime());

            SerializedBrokeredIdentityContext ctx = SerializedBrokeredIdentityContext.serialize(context);
            ctx.saveToAuthenticationSession(authSession, PostBrokerLoginConstants.PBL_BROKERED_IDENTITY_CONTEXT);

            authSession.setAuthNote(PostBrokerLoginConstants.PBL_AFTER_FIRST_BROKER_LOGIN, String.valueOf(wasFirstBrokerLogin));

            URI redirect = LoginActionsService.postBrokerLoginProcessor(session.getContext().getUri())
                    .queryParam(Constants.CLIENT_ID, authSession.getClient().getClientId())
                    .queryParam(Constants.TAB_ID, authSession.getTabId())
                    .queryParam(Constants.CLIENT_DATA, AuthenticationProcessor.getClientData(session, authSession))
                    .build(realmModel.getName());
            return Response.status(302).location(redirect).build();
        }
    }


    // Callback from LoginActionsService after postBrokerLogin flow is finished
    @GET
    @NoCache
    @Path("/after-post-broker-login")
    public Response afterPostBrokerLoginFlow(@QueryParam(LoginActionsService.SESSION_CODE) String code,
                                             @QueryParam(Constants.CLIENT_ID) String clientId,
                                             @QueryParam(Constants.CLIENT_DATA) String clientData,
                                             @QueryParam(Constants.TAB_ID) String tabId) {
        AuthenticationSessionModel authenticationSession = parseSessionCode(code, clientId, tabId, clientData);

        try {
            SerializedBrokeredIdentityContext serializedCtx = SerializedBrokeredIdentityContext.readFromAuthenticationSession(authenticationSession, PostBrokerLoginConstants.PBL_BROKERED_IDENTITY_CONTEXT);
            if (serializedCtx == null) {
                throw new IdentityBrokerException("Not found serialized context in clientSession. Note " + PostBrokerLoginConstants.PBL_BROKERED_IDENTITY_CONTEXT + " was null");
            }
            BrokeredIdentityContext context = serializedCtx.deserialize(session, authenticationSession);

            String wasFirstBrokerLoginNote = authenticationSession.getAuthNote(PostBrokerLoginConstants.PBL_AFTER_FIRST_BROKER_LOGIN);
            boolean wasFirstBrokerLogin = Boolean.parseBoolean(wasFirstBrokerLoginNote);

            // Ensure the post-broker-login flow was successfully finished
            String authStateNoteKey = PostBrokerLoginConstants.PBL_AUTH_STATE_PREFIX + context.getIdpConfig().getAlias();
            String authState = authenticationSession.getAuthNote(authStateNoteKey);
            if (!Boolean.parseBoolean(authState)) {
                throw new IdentityBrokerException("Invalid request. Not found the flag that post-broker-login flow was finished");
            }

            // remove notes
            authenticationSession.removeAuthNote(PostBrokerLoginConstants.PBL_BROKERED_IDENTITY_CONTEXT);
            authenticationSession.removeAuthNote(PostBrokerLoginConstants.PBL_AFTER_FIRST_BROKER_LOGIN);

            return afterPostBrokerLoginFlowSuccess(authenticationSession, context, wasFirstBrokerLogin);
        } catch (IdentityBrokerException e) {
            return redirectToErrorPage(authenticationSession, Response.Status.INTERNAL_SERVER_ERROR, Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR, e);
        }
    }

    private Response afterPostBrokerLoginFlowSuccess(AuthenticationSessionModel authSession, BrokeredIdentityContext context, boolean wasFirstBrokerLogin) {
        String providerAlias = context.getIdpConfig().getAlias();
        UserModel federatedUser = authSession.getAuthenticatedUser();

        if (wasFirstBrokerLogin) {
            return finishBrokerAuthentication(context, federatedUser, authSession, providerAlias);
        } else {

            boolean firstBrokerLoginInProgress = (authSession.getAuthNote(AbstractIdpAuthenticator.BROKERED_CONTEXT_NOTE) != null);
            if (firstBrokerLoginInProgress) {
                logger.debugf("Reauthenticated with broker '%s' when linking user '%s' with other broker", context.getIdpConfig().getAlias(), federatedUser.getUsername());

                SerializedBrokeredIdentityContext serializedCtx = SerializedBrokeredIdentityContext.readFromAuthenticationSession(authSession, AbstractIdpAuthenticator.BROKERED_CONTEXT_NOTE);
                authSession.setAuthNote(AbstractIdpAuthenticator.FIRST_BROKER_LOGIN_SUCCESS, serializedCtx.getIdentityProviderId());

                return afterFirstBrokerLogin(authSession);
            } else {
                return finishBrokerAuthentication(context, federatedUser, authSession, providerAlias);
            }
        }
    }


    private Response finishBrokerAuthentication(BrokeredIdentityContext context, UserModel federatedUser, AuthenticationSessionModel authSession, String providerAlias) {
        authSession.setAuthNote(AuthenticationProcessor.BROKER_SESSION_ID, context.getBrokerSessionId());
        authSession.setAuthNote(AuthenticationProcessor.BROKER_USER_ID, context.getBrokerUserId());

        this.event.user(federatedUser);

        context.getIdp().authenticationFinished(authSession, context);
        authSession.setUserSessionNote(Details.IDENTITY_PROVIDER, providerAlias);
        authSession.setUserSessionNote(Details.IDENTITY_PROVIDER_USERNAME, context.getUsername());

        event.detail(Details.IDENTITY_PROVIDER, providerAlias)
                .detail(Details.IDENTITY_PROVIDER_USERNAME, context.getUsername());

        if (isDebugEnabled()) {
            logger.debugf("Performing local authentication for user [%s].", federatedUser);
        }

        AuthenticationManager.setClientScopesInSession(session, authSession);

        String nextRequiredAction = AuthenticationManager.nextRequiredAction(session, authSession, request, event);
        if (nextRequiredAction != null) {
            if ("true".equals(authSession.getAuthNote(AuthenticationProcessor.FORWARDED_PASSIVE_LOGIN))) {
                logger.errorf("Required action %s found. Auth requests using prompt=none are incompatible with required actions", nextRequiredAction);
                return checkPassiveLoginError(authSession, OAuthErrorException.INTERACTION_REQUIRED);
            }
            return AuthenticationManager.redirectToRequiredActions(session, realmModel, authSession, session.getContext().getUri(), nextRequiredAction);
        } else {
            event.detail(Details.CODE_ID, authSession.getParentSession().getId());  // todo This should be set elsewhere.  find out why tests fail.  Don't know where this is supposed to be set
            return AuthenticationManager.finishedRequiredActions(session, authSession, null, clientConnection, request, session.getContext().getUri(), event);
        }
    }


    @Override
    public Response cancelled(IdentityProviderModel idpConfig) {
        AuthenticationSessionModel authSession = session.getContext().getAuthenticationSession();

        String idpDisplayName = KeycloakModelUtils.getIdentityProviderDisplayName(session, idpConfig);
        Response accountManagementFailedLinking = checkAccountManagementFailedLinking(authSession, Messages.ACCESS_DENIED_WHEN_IDP_AUTH, idpDisplayName);
        if (accountManagementFailedLinking != null) {
            return accountManagementFailedLinking;
        }

        return browserAuthentication(authSession, Messages.ACCESS_DENIED_WHEN_IDP_AUTH, idpDisplayName);
    }

    @Override
    public Response error(String message) {
        AuthenticationSessionModel authSession = session.getContext().getAuthenticationSession();

        Response accountManagementFailedLinking = checkAccountManagementFailedLinking(authSession, message);
        if (accountManagementFailedLinking != null) {
            return accountManagementFailedLinking;
        }

        Response passiveLoginErrorReturned = checkPassiveLoginError(authSession, message);
        if (passiveLoginErrorReturned != null) {
            return passiveLoginErrorReturned;
        }

        return browserAuthentication(authSession, message);
    }


    private boolean shouldPerformAccountLinking(AuthenticationSessionModel authSession, UserSessionModel userSession, String providerAlias) {
        String noteFromSession = authSession.getAuthNote(LINKING_IDENTITY_PROVIDER);
        if (noteFromSession == null) {
            return false;
        }

        boolean linkingValid;
        if (userSession == null) {
            linkingValid = false;
        } else {
            String expectedNote = userSession.getId() + authSession.getClient().getClientId() + providerAlias;
            linkingValid = expectedNote.equals(noteFromSession);
        }

        if (linkingValid) {
            authSession.removeAuthNote(LINKING_IDENTITY_PROVIDER);
            return true;
        } else {
            throw new ErrorPageException(session, Response.Status.BAD_REQUEST, Messages.BROKER_LINKING_SESSION_EXPIRED);
        }
    }


    private Response performAccountLinking(AuthenticationSessionModel authSession, UserSessionModel userSession, BrokeredIdentityContext context, FederatedIdentityModel newModel, UserModel federatedUser) {
        logger.debugf("Will try to link identity provider [%s] to user [%s]", context.getIdpConfig().getAlias(), userSession.getUser().getUsername());

        this.event.event(EventType.FEDERATED_IDENTITY_LINK);



        UserModel authenticatedUser = userSession.getUser();
        authSession.setAuthenticatedUser(authenticatedUser);

        if (federatedUser != null && !authenticatedUser.getId().equals(federatedUser.getId())) {
            return redirectToErrorWhenLinkingFailed(authSession, Messages.IDENTITY_PROVIDER_ALREADY_LINKED, context.getIdpConfig().getAlias());
        }

        if (!authenticatedUser.hasRole(this.realmModel.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID).getRole(AccountRoles.MANAGE_ACCOUNT))) {
            return redirectToErrorPage(authSession, Response.Status.FORBIDDEN, Messages.INSUFFICIENT_PERMISSION);
        }

        if (!authenticatedUser.isEnabled()) {
            return redirectToErrorWhenLinkingFailed(authSession, Messages.ACCOUNT_DISABLED);
        }



        if (federatedUser != null) {
            if (context.getIdpConfig().isStoreToken()) {
                FederatedIdentityModel oldModel = this.session.users().getFederatedIdentity(this.realmModel, federatedUser, context.getIdpConfig().getAlias());
                if (!ObjectUtil.isEqualOrBothNull(context.getToken(), oldModel.getToken())) {
                    this.session.users().updateFederatedIdentity(this.realmModel, federatedUser, newModel);
                    if (isDebugEnabled()) {
                        logger.debugf("Identity [%s] update with response from identity provider [%s].", federatedUser, context.getIdpConfig().getAlias());
                    }
                }
            }
        } else {
            this.session.users().addFederatedIdentity(this.realmModel, authenticatedUser, newModel);
            federatedUser = authenticatedUser;
        }

        updateFederatedIdentity(context, federatedUser);

        context.getIdp().authenticationFinished(authSession, context);

        AuthenticationManager.setClientScopesInSession(session, authSession);
        TokenManager.attachAuthenticationSession(session, userSession, authSession);

        if (isDebugEnabled()) {
            logger.debugf("Linking account [%s] from identity provider [%s] to user [%s].", newModel, context.getIdpConfig().getAlias(), authenticatedUser);
        }

        this.event.user(authenticatedUser)
                .detail(Details.USERNAME, authenticatedUser.getUsername())
                .detail(Details.IDENTITY_PROVIDER, newModel.getIdentityProvider())
                .detail(Details.IDENTITY_PROVIDER_USERNAME, newModel.getUserName())
                .success();

        // we do this to make sure that the parent IDP is logged out when this user session is complete.
        // But for the case when userSession was previously authenticated with broker1 and now is linked to another broker2, we shouldn't override broker1 notes with the broker2 for sure.
        // Maybe broker logout should be rather always skiped in case of broker-linking
        if (userSession.getNote(Details.IDENTITY_PROVIDER) == null) {
            userSession.setNote(Details.IDENTITY_PROVIDER, context.getIdpConfig().getAlias());
            userSession.setNote(Details.IDENTITY_PROVIDER_USERNAME, context.getUsername());
        }

        return Response.status(302).location(UriBuilder.fromUri(authSession.getRedirectUri()).build()).build();
    }


    private Response redirectToErrorWhenLinkingFailed(AuthenticationSessionModel authSession, String message, Object... parameters) {
        if (authSession.getClient() != null && authSession.getClient().getClientId().equals(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID)) {
            return redirectToAccountErrorPage(authSession, message, parameters);
        } else {
            return redirectToErrorPage(authSession, Response.Status.BAD_REQUEST, message, parameters); // Should rather redirect to app instead and display error here?
        }
    }


    private void updateFederatedIdentity(BrokeredIdentityContext context, UserModel federatedUser) {
        FederatedIdentityModel federatedIdentityModel = this.session.users().getFederatedIdentity(this.realmModel, federatedUser, context.getIdpConfig().getAlias());

        if (context.getIdpConfig().getSyncMode() == IdentityProviderSyncMode.FORCE) {
            setBasicUserAttributes(context, federatedUser);

            if (!Objects.equals(context.getUsername(), federatedIdentityModel.getUserName())) {
                federatedIdentityModel = new FederatedIdentityModel(federatedIdentityModel.getIdentityProvider(),
                        federatedIdentityModel.getUserId(), context.getUsername(),
                        federatedIdentityModel.getToken());

                this.session.users().updateFederatedIdentity(this.realmModel, federatedUser, federatedIdentityModel);
            }
        }

        // Skip DB write if tokens are null or equal
        updateToken(context, federatedUser, federatedIdentityModel);
        context.getIdp().updateBrokeredUser(session, realmModel, federatedUser, context);
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        session.identityProviders().getMappersByAliasStream(context.getIdpConfig().getAlias()).forEach(mapper -> {
            IdentityProviderMapper target = (IdentityProviderMapper) sessionFactory
                    .getProviderFactory(IdentityProviderMapper.class, mapper.getIdentityProviderMapper());
            IdentityProviderMapperSyncModeDelegate.delegateUpdateBrokeredUser(session, realmModel, federatedUser, mapper, context, target);
        });
    }

    private void setBasicUserAttributes(BrokeredIdentityContext context, UserModel federatedUser) {
        setDiffAttrToConsumer(federatedUser.getEmail(), context.getEmail(), email -> setEmail(context, federatedUser, email), true);
        setDiffAttrToConsumer(federatedUser.getFirstName(), context.getFirstName(), federatedUser::setFirstName, false);
        setDiffAttrToConsumer(federatedUser.getLastName(), context.getLastName(), federatedUser::setLastName, false);
    }

    private void setDiffAttrToConsumer(String actualValue, String newValue, Consumer<String> consumer, boolean ignoreCase) {
        String actualValueNotNull = Optional.ofNullable(actualValue).orElse("");
        if (newValue != null && !(ignoreCase? newValue.equalsIgnoreCase(actualValueNotNull) : newValue.equals(actualValueNotNull))) {
            consumer.accept(newValue);
        }
    }

    private void setEmail(BrokeredIdentityContext context, UserModel federatedUser, String newEmail) {
        federatedUser.setEmail(newEmail);
        // change email verified depending on if it is trusted or not
        if (context.getIdpConfig().isTrustEmail() && !Boolean.parseBoolean(context.getAuthenticationSession().getAuthNote(AbstractIdpAuthenticator.UPDATE_PROFILE_EMAIL_CHANGED))) {
            logger.tracef("Email verified automatically after updating user '%s' through Identity provider '%s' ", federatedUser.getUsername(), context.getIdpConfig().getAlias());
            federatedUser.setEmailVerified(true);
        } else {
            logger.tracef("Email verified reset to false after updating user '%s' through Identity provider '%s' ", federatedUser.getUsername(), context.getIdpConfig().getAlias());
            federatedUser.setEmailVerified(false);
        }
    }

    private void migrateFederatedIdentityId(BrokeredIdentityContext context, UserModel federatedUser) {
        FederatedIdentityModel identityModel = this.session.users().getFederatedIdentity(this.realmModel, federatedUser, context.getIdpConfig().getAlias());
        FederatedIdentityModel migratedIdentityModel = new FederatedIdentityModel(identityModel, context.getId());

        // since ID is a partial key we need to recreate the identity
        session.users().removeFederatedIdentity(realmModel, federatedUser, identityModel.getIdentityProvider());
        session.users().addFederatedIdentity(realmModel, federatedUser, migratedIdentityModel);
        logger.debugf("Federated user ID was migrated from %s to %s", identityModel.getUserId(), migratedIdentityModel.getUserId());
    }

    private void updateToken(BrokeredIdentityContext context, UserModel federatedUser, FederatedIdentityModel federatedIdentityModel) {
        if (context.getIdpConfig().isStoreToken() && !ObjectUtil.isEqualOrBothNull(context.getToken(), federatedIdentityModel.getToken())) {
            // like in OIDCIdentityProvider.exchangeStoredToken()
            // we shouldn't override the refresh token if it is null in the context and not null in the DB
            // as for google IDP it will be lost forever
            if (federatedIdentityModel.getToken() != null && ExchangeTokenToIdentityProviderToken.class.isInstance(context.getIdp())) {
                try {
                    AccessTokenResponse previousResponse = JsonSerialization.readValue(federatedIdentityModel.getToken(), AccessTokenResponse.class);
                    AccessTokenResponse newResponse = JsonSerialization.readValue(context.getToken(), AccessTokenResponse.class);

                    if (newResponse.getRefreshToken() == null && previousResponse.getRefreshToken() != null) {
                        newResponse.setRefreshToken(previousResponse.getRefreshToken());
                        newResponse.setRefreshExpiresIn(previousResponse.getRefreshExpiresIn());
                    }

                    federatedIdentityModel.setToken(JsonSerialization.writeValueAsString(newResponse));
                } catch (IOException ioe) {
                    logger.debugf("Token deserialization failed for identity provider %s:  %s", context.getIdpConfig().getAlias(), ioe.getMessage());
                    federatedIdentityModel.setToken(context.getToken());
                }
            } else {
                federatedIdentityModel.setToken(context.getToken());
            }

            this.session.users().updateFederatedIdentity(this.realmModel, federatedUser, federatedIdentityModel);
            logger.debugf("Identity [%s] update with response from identity provider [%s].", federatedUser, context.getIdpConfig().getAlias());
        }
    }

    @Override
    public AuthenticationSessionModel getAndVerifyAuthenticationSession(String encodedCode) {
        IdentityBrokerState state = IdentityBrokerState.encoded(encodedCode, realmModel);
        String code = state.getDecodedState();
        String clientId = state.getClientId();
        String tabId = state.getTabId();
        String clientData = state.getClientData();
        return parseSessionCode(code, clientId, tabId, clientData);
    }

    /**
     * This method will throw JAX-RS exception in case it is not able to retrieve AuthenticationSessionModel. It never returns null
     */
    private AuthenticationSessionModel parseSessionCode(String code, String clientId, String tabId, String clientData) {
        if (code == null || clientId == null || tabId == null) {
            logger.debugf("Invalid request. Authorization code, clientId or tabId was null. Code=%s, clientId=%s, tabID=%s", code, clientId, tabId);
            Response staleCodeError = redirectToErrorPage(Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
            throw new WebApplicationException(staleCodeError);
        }

        SessionCodeChecks checks = new SessionCodeChecks(realmModel, session.getContext().getUri(), request, clientConnection, session, event, null, code, null, clientId, tabId, clientData, LoginActionsService.AUTHENTICATE_PATH);
        checks.initialVerify();
        if (!checks.verifyActiveAndValidAction(AuthenticationSessionModel.Action.AUTHENTICATE.name(), ClientSessionCode.ActionType.LOGIN)) {

            AuthenticationSessionModel authSession = checks.getAuthenticationSession();
            if (authSession != null) {
                // Check if error happened during login or during linking from account management
                Response accountManagementFailedLinking = checkAccountManagementFailedLinking(authSession, Messages.STALE_CODE_ACCOUNT);
                if (accountManagementFailedLinking != null) {
                    throw new WebApplicationException(accountManagementFailedLinking);
                } else {
                    Response errorResponse = checks.getResponse();

                    // Remove "code" from browser history
                    errorResponse = BrowserHistoryHelper.getInstance().saveResponseAndRedirect(session, authSession, errorResponse, true, request);
                    throw new WebApplicationException(errorResponse);
                }
            } else {
                throw new WebApplicationException(checks.getResponse());
            }
        } else {
            if (isDebugEnabled()) {
                logger.debugf("Authorization code is valid.");
            }

            return checks.getClientCode().getClientSession();
        }
    }

    private Response checkAccountManagementFailedLinking(AuthenticationSessionModel authSession, String error, Object... parameters) {
        UserSessionModel userSession = new AuthenticationSessionManager(session).getUserSession(authSession);
        if (userSession != null && authSession.getClient() != null && authSession.getClient().getClientId().equals(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID)) {

            this.event.event(EventType.FEDERATED_IDENTITY_LINK);
            UserModel user = userSession.getUser();
            this.event.user(user);
            this.event.detail(Details.USERNAME, user.getUsername());

            return redirectToAccountErrorPage(authSession, error, parameters);
        } else {
            return null;
        }
    }

    /**
     * Checks if specified message matches one of the passive login error messages and if it does builds a response that
     * redirects the error back to the client.
     *
     * @param authSession the authentication session.
     * @param message the error message.
     * @return a {@code {@link Response}} that redirects the error message back to the client if the {@code message} is one
     * of the passive login error messages, or {@code null} if it is not.
     */
    private Response checkPassiveLoginError(AuthenticationSessionModel authSession, String message) {
        LoginProtocol.Error error = OAuthErrorException.LOGIN_REQUIRED.equals(message) ? LoginProtocol.Error.PASSIVE_LOGIN_REQUIRED :
                (OAuthErrorException.INTERACTION_REQUIRED.equals(message) ? LoginProtocol.Error.PASSIVE_INTERACTION_REQUIRED : null);
        if (error != null) {
            LoginProtocol protocol = session.getProvider(LoginProtocol.class, authSession.getProtocol());
            protocol.setRealm(realmModel)
                    .setHttpHeaders(headers)
                    .setUriInfo(session.getContext().getUri())
                    .setEventBuilder(event);
            return protocol.sendError(authSession, error);
        }
        return null;
    }

    private AuthenticationRequest createAuthenticationRequest(IdentityProvider<?> identityProvider, String providerAlias, ClientSessionCode<AuthenticationSessionModel> clientSessionCode) {
        AuthenticationSessionModel authSession = null;
        IdentityBrokerState encodedState = null;

        if (clientSessionCode != null) {
            authSession = clientSessionCode.getClientSession();
            String relayState = clientSessionCode.getOrGenerateCode();
            String clientData = identityProvider.supportsLongStateParameter() ? AuthenticationProcessor.getClientData(session, authSession) : null;
            encodedState = IdentityBrokerState.decoded(relayState, authSession.getClient().getId(), authSession.getClient().getClientId(), authSession.getTabId(), clientData);
        }

        return new AuthenticationRequest(this.session, this.realmModel, authSession, this.request, this.session.getContext().getUri(), encodedState, getRedirectUri(providerAlias));
    }

    private String getRedirectUri(String providerAlias) {
        return Urls.identityProviderAuthnResponse(this.session.getContext().getUri().getBaseUri(), providerAlias, this.realmModel.getName()).toString();
    }

    private Response redirectToErrorPage(AuthenticationSessionModel authSession, Response.Status status, String message, Object ... parameters) {
        return redirectToErrorPage(authSession, status, message, null, parameters);
    }

    private Response redirectToErrorPage(Response.Status status, String message, Object ... parameters) {
        return redirectToErrorPage(null, status, message, null, parameters);
    }

    private Response redirectToErrorPage(Response.Status status, String message, Throwable throwable, Object ... parameters) {
        return redirectToErrorPage(null, status, message, throwable, parameters);
    }

    private Response redirectToErrorPage(AuthenticationSessionModel authSession, Response.Status status, String message, Throwable throwable, Object ... parameters) {
        if (message == null) {
            message = Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR;
        }

        fireErrorEvent(message, throwable);

        if (throwable instanceof WebApplicationException) {
            WebApplicationException webEx = (WebApplicationException) throwable;
            return webEx.getResponse();
        }

        throw new ErrorPageException(this.session, authSession, status, message, parameters);
    }

    private Response redirectToAccountErrorPage(AuthenticationSessionModel authSession, String message, Object ... parameters) {
        fireErrorEvent(message);

        FormMessage errorMessage = new FormMessage(message, parameters);
        try {
            String serializedError = JsonSerialization.writeValueAsString(errorMessage);
            authSession.setAuthNote(AccountConsole.ACCOUNT_MGMT_FORWARDED_ERROR_NOTE, serializedError);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        URI accountServiceUri = UriBuilder.fromUri(authSession.getRedirectUri()).queryParam(Constants.TAB_ID, authSession.getTabId()).build();
        return Response.status(302).location(accountServiceUri).build();
    }


    protected Response browserAuthentication(AuthenticationSessionModel authSession, String errorMessage, Object... parameters) {
        this.event.event(EventType.LOGIN);
        AuthenticationFlowModel flow = AuthenticationFlowResolver.resolveBrowserFlow(authSession);
        String flowId = flow.getId();
        AuthenticationProcessor processor = new AuthenticationProcessor();
        processor.setAuthenticationSession(authSession)
                .setFlowPath(LoginActionsService.AUTHENTICATE_PATH)
                .setFlowId(flowId)
                .setBrowserFlow(true)
                .setConnection(clientConnection)
                .setEventBuilder(event)
                .setRealm(realmModel)
                .setSession(session)
                .setUriInfo(session.getContext().getUri())
                .setRequest(request);
        if (errorMessage != null) processor.setForwardedErrorMessage(new FormMessage(null, errorMessage, parameters));

        try {
            CacheControlUtil.noBackButtonCacheControlHeader(session);
            return processor.authenticate();
        } catch (Exception e) {
            return processor.handleBrowserException(e);
        }
    }


    private Response badRequest(String message) {
        fireErrorEvent(message);
        throw ErrorResponse.error(message, Response.Status.BAD_REQUEST);
    }

    private Response forbidden(String message) {
        fireErrorEvent(message);
        throw ErrorResponse.error(message, Response.Status.FORBIDDEN);
    }

    private Response notFound(String message) {
        fireErrorEvent(message);
        throw ErrorResponse.error(message, Response.Status.NOT_FOUND);
    }

    public static IdentityProvider<?> getIdentityProvider(KeycloakSession session, String alias) {
        IdentityProviderModel identityProviderModel = session.identityProviders().getByAlias(alias);

        if (identityProviderModel != null) {
            IdentityProviderFactory<?> providerFactory = getIdentityProviderFactory(session, identityProviderModel);

            if (providerFactory == null) {
                throw new IdentityBrokerException("Could not find factory for identity provider [" + alias + "].");
            }

            return providerFactory.create(session, identityProviderModel);
        }

        throw new IdentityBrokerException("Identity Provider [" + alias + "] not found.");
    }

    public static IdentityProviderFactory<?> getIdentityProviderFactory(KeycloakSession session, IdentityProviderModel model) {
        return Stream.concat(session.getKeycloakSessionFactory().getProviderFactoriesStream(IdentityProvider.class),
                session.getKeycloakSessionFactory().getProviderFactoriesStream(SocialIdentityProvider.class))
                .filter(providerFactory -> Objects.equals(providerFactory.getId(), model.getProviderId()))
                .map(IdentityProviderFactory.class::cast)
                .findFirst()
                .orElse(null);
    }

    private IdentityProviderModel getIdentityProviderConfig(String providerAlias) {
        IdentityProviderModel model = session.identityProviders().getByAlias(providerAlias);
        if (model == null) {
            throw new IdentityBrokerException("Configuration for identity provider [" + providerAlias + "] not found.");
        }
        return model;
    }

    private Response corsResponse(Response response, ClientModel clientModel) {
        return Cors.builder().auth().allowedOrigins(session, clientModel).add(Response.fromResponse(response));
    }

    private void fireErrorEvent(String message, Throwable throwable) {
        if (!this.event.getEvent().getType().toString().endsWith("_ERROR")) {
            boolean newTransaction = !this.session.getTransactionManager().isActive();

            try {
                if (newTransaction) {
                    this.session.getTransactionManager().begin();
                }

                this.event.error(message);

                if (newTransaction) {
                    this.session.getTransactionManager().commit();
                }
            } catch (Exception e) {
                ServicesLogger.LOGGER.couldNotFireEvent(e);
                rollback();
            }
        }

        if (throwable != null) {
            logger.error(message, throwable);
        } else {
            logger.error(message);
        }
    }

    private void fireErrorEvent(String message) {
        fireErrorEvent(message, null);
    }

    private boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    private void rollback() {
        if (this.session.getTransactionManager().isActive()) {
            this.session.getTransactionManager().rollback();
        }
    }

}

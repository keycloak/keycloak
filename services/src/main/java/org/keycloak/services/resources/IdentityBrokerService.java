/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.services.resources;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.ClientConnection;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.broker.provider.IdentityProviderMapper;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.Constants;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationManager.AuthResult;
import org.keycloak.services.managers.BruteForceProtector;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.Urls;
import org.keycloak.services.validation.Validation;
import org.keycloak.social.SocialIdentityProvider;
import org.keycloak.util.ObjectUtil;

import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.keycloak.models.AccountRoles.MANAGE_ACCOUNT;
import static org.keycloak.models.ClientSessionModel.Action.AUTHENTICATE;
import static org.keycloak.models.Constants.ACCOUNT_MANAGEMENT_CLIENT_ID;
import static org.keycloak.models.UserModel.RequiredAction.UPDATE_PROFILE;

/**
 * <p></p>
 *
 * @author Pedro Igor
 */
public class IdentityBrokerService implements IdentityProvider.AuthenticationCallback {

    private static final Logger LOGGER = Logger.getLogger(IdentityBrokerService.class);
    public static final String BROKER_PROVIDER_ID = "BROKER_PROVIDER_ID";

    private final RealmModel realmModel;

    @Context
    private UriInfo uriInfo;

    @Context
    private KeycloakSession session;

    @Context
    private ClientConnection clientConnection;

    @Context
    private HttpRequest request;

    @Context
    private HttpHeaders headers;

    private EventBuilder event;

    private BruteForceProtector protector;

    public IdentityBrokerService(RealmModel realmModel, BruteForceProtector protector) {
        if (realmModel == null) {
            throw new IllegalArgumentException("Realm can not be null.");
        }
        this.protector = protector;
        this.realmModel = realmModel;
    }

    public void init() {
        this.event = new EventBuilder(realmModel, session, clientConnection).event(EventType.IDENTITY_PROVIDER_LOGIN);
    }

    @GET
    @Path("/{provider_id}/login")
    public Response performLogin(@PathParam("provider_id") String providerId, @QueryParam("code") String code) {
        this.event.detail(Details.IDENTITY_PROVIDER, providerId);

        if (isDebugEnabled()) {
            LOGGER.debugf("Sending authentication request to identity provider [%s].", providerId);
        }

        try {
            ClientSessionCode clientSessionCode = parseClientSessionCode(code);
            IdentityProvider identityProvider = getIdentityProvider(session, realmModel, providerId);
            Response response = identityProvider.performLogin(createAuthenticationRequest(providerId, clientSessionCode));

            if (response != null) {
                if (isDebugEnabled()) {
                    LOGGER.debugf("Identity provider [%s] is going to send a request [%s].", identityProvider, response);
                }
                return response;
            }
        } catch (IdentityBrokerException e) {
            return redirectToErrorPage(Messages.COULD_NOT_SEND_AUTHENTICATION_REQUEST, e, providerId);
        } catch (Exception e) {
            return redirectToErrorPage(Messages.UNEXPECTED_ERROR_HANDLING_REQUEST, e, providerId);
        }

        return redirectToErrorPage(Messages.COULD_NOT_PROCEED_WITH_AUTHENTICATION_REQUEST);
    }

    @Path("{provider_id}/endpoint")
    public Object getEndpoint(@PathParam("provider_id") String providerId) {
        IdentityProvider identityProvider = getIdentityProvider(session, realmModel, providerId);
        Object callback = identityProvider.callback(realmModel, this, event);
        ResteasyProviderFactory.getInstance().injectProperties(callback);
        //resourceContext.initResource(brokerService);
        return callback;


    }

    @Path("{provider_id}/token")
    @OPTIONS
    public Response retrieveTokenPreflight() {
        return Cors.add(this.request, Response.ok()).auth().preflight().build();
    }

    @GET
    @Path("{provider_id}/token")
    public Response retrieveToken(@PathParam("provider_id") String providerId) {
        return getToken(providerId, false);
    }

    private Response getToken(String providerId, boolean forceRetrieval) {
        this.event.event(EventType.IDENTITY_PROVIDER_RETRIEVE_TOKEN);

        try {
            AppAuthManager authManager = new AppAuthManager();
            AuthResult authResult = authManager.authenticateBearerToken(this.session, this.realmModel, this.uriInfo, this.clientConnection, this.request.getHttpHeaders());

            if (authResult != null) {
                AccessToken token = authResult.getToken();
                String[] audience = token.getAudience();
                ClientModel clientModel = this.realmModel.getClientByClientId(audience[0]);

                if (clientModel == null) {
                    return badRequest("Invalid client.");
                }

                session.getContext().setClient(clientModel);

                ClientModel brokerClient = realmModel.getClientByClientId(Constants.BROKER_SERVICE_CLIENT_ID);
                if (brokerClient == null) {
                    return corsResponse(forbidden("Realm has not migrated to support the broker token exchange service"), clientModel);

                }
                Map<String, AccessToken.Access> resourceAccess = token.getResourceAccess();
                AccessToken.Access brokerRoles = resourceAccess == null ? null : resourceAccess.get(Constants.BROKER_SERVICE_CLIENT_ID);
                if (brokerRoles == null || !brokerRoles.isUserInRole(Constants.READ_TOKEN_ROLE)) {
                    return corsResponse(forbidden("Client [" + audience + "] not authorized to retrieve tokens from identity provider [" + providerId + "]."), clientModel);

                }

                IdentityProvider identityProvider = getIdentityProvider(session, realmModel, providerId);
                IdentityProviderModel identityProviderConfig = getIdentityProviderConfig(providerId);

                if (identityProviderConfig.isStoreToken()) {
                    FederatedIdentityModel identity = this.session.users().getFederatedIdentity(authResult.getUser(), providerId, this.realmModel);

                    if (identity == null) {
                        return corsResponse(badRequest("User [" + authResult.getUser().getId() + "] is not associated with identity provider [" + providerId + "]."), clientModel);
                    }

                    this.event.success();

                    return corsResponse(identityProvider.retrieveToken(identity), clientModel);
                }

                return corsResponse(badRequest("Identity Provider [" + providerId + "] does not support this operation."), clientModel);
            }

            return badRequest("Invalid token.");
        } catch (IdentityBrokerException e) {
            return redirectToErrorPage(Messages.COULD_NOT_OBTAIN_TOKEN, e, providerId);
        }  catch (Exception e) {
            return redirectToErrorPage(Messages.UNEXPECTED_ERROR_RETRIEVING_TOKEN, e, providerId);
        }
    }

    public Response authenticated(BrokeredIdentityContext context) {
        ClientSessionCode clientCode = null;
        IdentityProviderModel identityProviderConfig = context.getIdpConfig();
        try {
            clientCode = parseClientSessionCode(context.getCode());
        } catch (Exception e) {
            return redirectToErrorPage(Messages.IDENTITY_PROVIDER_AUTHENTICATION_FAILED, e, identityProviderConfig.getProviderId());

        }
        String providerId = identityProviderConfig.getAlias();
        if (!identityProviderConfig.isStoreToken()) {
            if (isDebugEnabled()) {
                LOGGER.debugf("Token will not be stored for identity provider [%s].", providerId);
            }
            context.setToken(null);
        }

        ClientSessionModel clientSession = clientCode.getClientSession();
        context.setClientSession(clientSession);

        session.getContext().setClient(clientSession.getClient());

        context.getIdp().preprocessFederatedIdentity(session, realmModel, context);
        Set<IdentityProviderMapperModel> mappers = realmModel.getIdentityProviderMappersByAlias(context.getIdpConfig().getAlias());
        if (mappers != null) {
            KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
            for (IdentityProviderMapperModel mapper : mappers) {
                IdentityProviderMapper target = (IdentityProviderMapper)sessionFactory.getProviderFactory(IdentityProviderMapper.class, mapper.getIdentityProviderMapper());
                target.preprocessFederatedIdentity(session, realmModel, mapper, context);
            }
        }

        FederatedIdentityModel federatedIdentityModel = new FederatedIdentityModel(providerId, context.getId(),
                context.getUsername(), context.getToken());

        this.event.event(EventType.IDENTITY_PROVIDER_LOGIN)
                .detail(Details.REDIRECT_URI, clientSession.getRedirectUri())
                .detail(Details.IDENTITY_PROVIDER_USERNAME, context.getUsername());

        UserModel federatedUser = this.session.users().getUserByFederatedIdentity(federatedIdentityModel, this.realmModel);

        // Check if federatedUser is already authenticated (this means linking social into existing federatedUser account)
        if (clientSession.getUserSession() != null) {
            return performAccountLinking(clientSession, context, federatedIdentityModel, federatedUser);
        }

        if (federatedUser == null) {
            try {
                federatedUser = createUser(context);

                if (IdentityProviderRepresentation.UPFLM_ON.equals(identityProviderConfig.getUpdateProfileFirstLoginMode()) 
                        || (IdentityProviderRepresentation.UPFLM_MISSING.equals(identityProviderConfig.getUpdateProfileFirstLoginMode()) && !Validation.validateUserMandatoryFields(realmModel, federatedUser))) {
                    if (isDebugEnabled()) {
                        LOGGER.debugf("Identity provider requires update profile action.", federatedUser);
                    }
                    federatedUser.addRequiredAction(UPDATE_PROFILE);
                }
                if(identityProviderConfig.isTrustEmail() && !Validation.isBlank(federatedUser.getEmail())){
                    federatedUser.setEmailVerified(true);
                }
            } catch (Exception e) {
                return redirectToLoginPage(e, clientCode);
            }
        } else {
            updateFederatedIdentity(context, federatedUser);
        }


        UserSessionModel userSession = this.session.sessions()
                .createUserSession(this.realmModel, federatedUser, federatedUser.getUsername(), this.clientConnection.getRemoteAddr(), "broker", false, context.getBrokerSessionId(), context.getBrokerUserId());

        this.event.user(federatedUser);
        this.event.session(userSession);

        TokenManager.attachClientSession(userSession, clientSession);
        context.getIdp().attachUserSession(userSession, clientSession, context);
        userSession.setNote(BROKER_PROVIDER_ID, providerId);

        if (isDebugEnabled()) {
            LOGGER.debugf("Performing local authentication for user [%s].", federatedUser);
        }

        return AuthenticationManager.nextActionAfterAuthentication(this.session, userSession, clientSession, this.clientConnection, this.request,
                this.uriInfo, event);
    }

    @Override
    public Response cancelled(String code) {
        ClientSessionCode clientCode = ClientSessionCode.parse(code, this.session, this.realmModel);
        if (clientCode.getClientSession() == null || !clientCode.isValid(AUTHENTICATE.name())) {
            return redirectToErrorPage(Messages.INVALID_CODE);
        }

        return browserAuthentication(clientCode.getClientSession(), null);
    }

    @Override
    public Response error(String code, String message) {
        ClientSessionCode clientCode = ClientSessionCode.parse(code, this.session, this.realmModel);
        if (clientCode.getClientSession() == null || !clientCode.isValid(AUTHENTICATE.name())) {
            return redirectToErrorPage(Messages.INVALID_CODE);
        }
        return browserAuthentication(clientCode.getClientSession(), message);
    }

    private Response performAccountLinking(ClientSessionModel clientSession, BrokeredIdentityContext context, FederatedIdentityModel federatedIdentityModel, UserModel federatedUser) {
        this.event.event(EventType.IDENTITY_PROVIDER_ACCCOUNT_LINKING);

        if (federatedUser != null) {
            return redirectToErrorPage(Messages.IDENTITY_PROVIDER_ALREADY_LINKED, context.getIdpConfig().getAlias());
        }

        UserModel authenticatedUser = clientSession.getUserSession().getUser();

        if (isDebugEnabled()) {
            LOGGER.debugf("Linking account [%s] from identity provider [%s] to user [%s].", federatedIdentityModel, context.getIdpConfig().getAlias(), authenticatedUser);
        }

        if (!authenticatedUser.isEnabled()) {
            fireErrorEvent(Errors.USER_DISABLED);
            return redirectToErrorPage(Messages.ACCOUNT_DISABLED);
        }

        if (!authenticatedUser.hasRole(this.realmModel.getClientByClientId(ACCOUNT_MANAGEMENT_CLIENT_ID).getRole(MANAGE_ACCOUNT))) {
            fireErrorEvent(Errors.NOT_ALLOWED);
            return redirectToErrorPage(Messages.INSUFFICIENT_PERMISSION);
        }

        this.session.users().addFederatedIdentity(this.realmModel, authenticatedUser, federatedIdentityModel);
        context.getIdp().attachUserSession(clientSession.getUserSession(), clientSession, context);

        this.event.success();
        return Response.status(302).location(UriBuilder.fromUri(clientSession.getRedirectUri()).build()).build();
    }

    private void updateFederatedIdentity(BrokeredIdentityContext context, UserModel federatedUser) {
        FederatedIdentityModel federatedIdentityModel = this.session.users().getFederatedIdentity(federatedUser, context.getIdpConfig().getAlias(), this.realmModel);

        // Skip DB write if tokens are null or equal
        if (context.getIdpConfig().isStoreToken() && !ObjectUtil.isEqualOrNull(context.getToken(), federatedIdentityModel.getToken())) {
            federatedIdentityModel.setToken(context.getToken());

            this.session.users().updateFederatedIdentity(this.realmModel, federatedUser, federatedIdentityModel);

            if (isDebugEnabled()) {
                LOGGER.debugf("Identity [%s] update with response from identity provider [%s].", federatedUser, context.getIdpConfig().getAlias());
            }
        }
        context.getIdp().updateBrokeredUser(session, realmModel, federatedUser, context);
        Set<IdentityProviderMapperModel> mappers = realmModel.getIdentityProviderMappersByAlias(context.getIdpConfig().getAlias());
        if (mappers != null) {
            KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
            for (IdentityProviderMapperModel mapper : mappers) {
                IdentityProviderMapper target = (IdentityProviderMapper)sessionFactory.getProviderFactory(IdentityProviderMapper.class, mapper.getIdentityProviderMapper());
                target.updateBrokeredUser(session, realmModel, federatedUser, mapper, context);
            }
        }

    }

    private ClientSessionCode parseClientSessionCode(String code) {
        ClientSessionCode clientCode = ClientSessionCode.parse(code, this.session, this.realmModel);

        if (clientCode != null && clientCode.isValid(AUTHENTICATE.name())) {
            ClientSessionModel clientSession = clientCode.getClientSession();

            if (clientSession != null) {
                ClientModel client = clientSession.getClient();

                if (client == null) {
                    throw new IdentityBrokerException("Invalid client");
                }

                LOGGER.debugf("Got authorization code from client [%s].", client.getClientId());
                this.event.client(client);

                if (clientSession.getUserSession() != null) {
                    this.event.session(clientSession.getUserSession());
                }
            }

            if (isDebugEnabled()) {
                LOGGER.debugf("Authorization code is valid.");
            }

            return clientCode;
        }

        throw new IdentityBrokerException("Invalid code, please login again through your client.");
    }

    private AuthenticationRequest createAuthenticationRequest(String providerId, ClientSessionCode clientSessionCode) {
        ClientSessionModel clientSession = null;
        String relayState = null;

        if (clientSessionCode != null) {
            clientSession = clientSessionCode.getClientSession();
            relayState = clientSessionCode.getCode();
        }

        return new AuthenticationRequest(this.session, this.realmModel, clientSession, this.request, this.uriInfo, relayState, getRedirectUri(providerId));
    }

    private String getRedirectUri(String providerId) {
        return Urls.identityProviderAuthnResponse(this.uriInfo.getBaseUri(), providerId, this.realmModel.getName()).toString();
    }

    private Response redirectToErrorPage(String message, Object ... parameters) {
        return redirectToErrorPage(message, null, parameters);
    }

    private Response redirectToErrorPage(String message, Throwable throwable, Object ... parameters) {
        if (message == null) {
            message = Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR;
        }

        fireErrorEvent(message, throwable);
        return ErrorPage.error(this.session, message, parameters);
    }

    private Response redirectToLoginPage(Throwable t, ClientSessionCode clientCode) {
        String message = t.getMessage();

        if (message == null) {
            message = Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR;
        }

        fireErrorEvent(message);
        return browserAuthentication(clientCode.getClientSession(), message);
    }

    protected Response browserAuthentication(ClientSessionModel clientSession, String errorMessage) {
        this.event.event(EventType.LOGIN);
        AuthenticationFlowModel flow = realmModel.getBrowserFlow();
        String flowId = flow.getId();
        AuthenticationProcessor processor = new AuthenticationProcessor();
        processor.setClientSession(clientSession)
                .setFlowPath(LoginActionsService.AUTHENTICATE_PATH)
                .setFlowId(flowId)
                .setConnection(clientConnection)
                .setEventBuilder(event)
                .setProtector(protector)
                .setRealm(realmModel)
                .setSession(session)
                .setUriInfo(uriInfo)
                .setRequest(request);
        if (errorMessage != null) processor.setForwardedErrorMessage(new FormMessage(null, errorMessage));

        try {
            return processor.authenticate();
        } catch (Exception e) {
            return processor.handleBrowserException(e);
        }
    }


    private Response badRequest(String message) {
        fireErrorEvent(message);
        return ErrorResponse.error(message, Status.BAD_REQUEST);
    }

    private Response forbidden(String message) {
        fireErrorEvent(message);
        return ErrorResponse.error(message, Status.FORBIDDEN);
    }

    public static IdentityProvider getIdentityProvider(KeycloakSession session, RealmModel realm, String alias) {
        IdentityProviderModel identityProviderModel = realm.getIdentityProviderByAlias(alias);

        if (identityProviderModel != null) {
            IdentityProviderFactory providerFactory = getIdentityProviderFactory(session, identityProviderModel);

            if (providerFactory == null) {
                throw new IdentityBrokerException("Could not find factory for identity provider [" + alias + "].");
            }

            return providerFactory.create(identityProviderModel);
        }

        throw new IdentityBrokerException("Identity Provider [" + alias + "] not found.");
    }

    private static IdentityProviderFactory getIdentityProviderFactory(KeycloakSession session, IdentityProviderModel model) {
        Map<String, IdentityProviderFactory> availableProviders = new HashMap<String, IdentityProviderFactory>();
        List<ProviderFactory> allProviders = new ArrayList<ProviderFactory>();

        allProviders.addAll(session.getKeycloakSessionFactory().getProviderFactories(IdentityProvider.class));
        allProviders.addAll(session.getKeycloakSessionFactory().getProviderFactories(SocialIdentityProvider.class));

        for (ProviderFactory providerFactory : allProviders) {
            availableProviders.put(providerFactory.getId(), (IdentityProviderFactory) providerFactory);
        }

        return availableProviders.get(model.getProviderId());
    }

    private IdentityProviderModel getIdentityProviderConfig(String providerId) {
        for (IdentityProviderModel model : this.realmModel.getIdentityProviders()) {
            if (model.getAlias().equals(providerId)) {
                return model;
            }
        }

        throw new IdentityBrokerException("Configuration for identity provider [" + providerId + "] not found.");
    }

    private UserModel createUser(BrokeredIdentityContext context) {
        FederatedIdentityModel federatedIdentityModel = new FederatedIdentityModel(context.getIdpConfig().getAlias(), context.getId(),
                context.getUsername(), context.getToken());
        // Check if no user already exists with this username or email
        UserModel existingUser = null;

        if (context.getEmail() != null) {
            existingUser = this.session.users().getUserByEmail(context.getEmail(), this.realmModel);
        }

        if (existingUser != null) {
            fireErrorEvent(Errors.FEDERATED_IDENTITY_EMAIL_EXISTS);
            throw new IdentityBrokerException(Messages.FEDERATED_IDENTITY_EMAIL_EXISTS);
        }
        String username = context.getModelUsername();
        if (username == null) {
            username = context.getUsername();
            if (this.realmModel.isRegistrationEmailAsUsername() && !Validation.isBlank(context.getEmail())) {
                username = context.getEmail();
            } else if (username == null) {
                username = context.getIdpConfig().getAlias() + "." + context.getId();
            } else {
                username = context.getIdpConfig().getAlias() + "." + context.getUsername();
            }
        }
        if (username == null) {
            LOGGER.warn("Unknown username");
            fireErrorEvent(Errors.FEDERATED_IDENTITY_USERNAME_EXISTS);
            throw new IdentityBrokerException(Messages.FEDERATED_IDENTITY_USERNAME_EXISTS);

        }
        username = username.trim();

        existingUser = this.session.users().getUserByUsername(username, this.realmModel);

        if (existingUser != null) {
            fireErrorEvent(Errors.FEDERATED_IDENTITY_USERNAME_EXISTS);
            throw new IdentityBrokerException(Messages.FEDERATED_IDENTITY_USERNAME_EXISTS);
        }

        if (isDebugEnabled()) {
            LOGGER.debugf("Creating account from identity [%s].", federatedIdentityModel);
        }

        UserModel federatedUser = this.session.users().addUser(this.realmModel, username);

        if (isDebugEnabled()) {
            LOGGER.debugf("Account [%s] created.", federatedUser);
        }

        federatedUser.setEnabled(true);
        federatedUser.setEmail(context.getEmail());
        federatedUser.setFirstName(context.getFirstName());
        federatedUser.setLastName(context.getLastName());


        if (context.getIdpConfig().isAddReadTokenRoleOnCreate()) {
            RoleModel readTokenRole = realmModel.getClientByClientId(Constants.BROKER_SERVICE_CLIENT_ID).getRole(Constants.READ_TOKEN_ROLE);
            federatedUser.grantRole(readTokenRole);
        }

        if (context.getIdpConfig().isStoreToken()) {
            federatedIdentityModel.setToken(context.getToken());
        }

        this.session.users().addFederatedIdentity(this.realmModel, federatedUser, federatedIdentityModel);

        context.getIdp().importNewUser(session, realmModel, federatedUser, context);
        Set<IdentityProviderMapperModel> mappers = realmModel.getIdentityProviderMappersByAlias(context.getIdpConfig().getAlias());
        if (mappers != null) {
            KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
            for (IdentityProviderMapperModel mapper : mappers) {
                IdentityProviderMapper target = (IdentityProviderMapper)sessionFactory.getProviderFactory(IdentityProviderMapper.class, mapper.getIdentityProviderMapper());
                target.importNewUser(session, realmModel, federatedUser, mapper, context);
            }
        }


        this.event.clone().user(federatedUser).event(EventType.REGISTER)
                .detail(Details.IDENTITY_PROVIDER, federatedIdentityModel.getIdentityProvider())
                .detail(Details.IDENTITY_PROVIDER_USERNAME, context.getUsername())
                .removeDetail("auth_method")
                .success();

        return federatedUser;
    }

    private Response corsResponse(Response response, ClientModel clientModel) {
        return Cors.add(this.request, Response.fromResponse(response)).auth().allowedOrigins(clientModel).build();
    }

    private void fireErrorEvent(String message, Throwable throwable) {
        if (!this.event.getEvent().getType().toString().endsWith("_ERROR")) {
            boolean newTransaction = !this.session.getTransaction().isActive();

            try {
                if (newTransaction) {
                    this.session.getTransaction().begin();
                }

                this.event.error(message);

                if (newTransaction) {
                    this.session.getTransaction().commit();
                }
            } catch (Exception e) {
                LOGGER.error("Could not fire event.", e);
                rollback();
            }
        }

        if (throwable != null) {
            LOGGER.error(message, throwable);
        } else {
            LOGGER.error(message);
        }
    }

    private void fireErrorEvent(String message) {
        fireErrorEvent(message, null);
    }

    private boolean isDebugEnabled() {
        return LOGGER.isDebugEnabled();
    }

    private void rollback() {
        if (this.session.getTransaction().isActive()) {
            this.session.getTransaction().rollback();
        }
    }
}

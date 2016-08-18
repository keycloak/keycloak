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

import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.PostBrokerLoginConstants;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.common.ClientConnection;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.broker.provider.IdentityProviderMapper;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
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
import org.keycloak.models.utils.FormMessage;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager.AuthResult;
import org.keycloak.services.managers.BruteForceProtector;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.Urls;
import org.keycloak.services.util.CacheControlUtil;
import org.keycloak.services.validation.Validation;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.keycloak.models.AccountRoles.MANAGE_ACCOUNT;
import static org.keycloak.models.ClientSessionModel.Action.AUTHENTICATE;
import static org.keycloak.models.Constants.ACCOUNT_MANAGEMENT_CLIENT_ID;

/**
 * <p></p>
 *
 * @author Pedro Igor
 */
public class IdentityBrokerService implements IdentityProvider.AuthenticationCallback {

    private static final ServicesLogger logger = ServicesLogger.ROOT_LOGGER;

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


    public IdentityBrokerService(RealmModel realmModel) {
        if (realmModel == null) {
            throw new IllegalArgumentException("Realm can not be null.");
        }
        this.realmModel = realmModel;
    }

    public void init() {
        this.event = new EventBuilder(realmModel, session, clientConnection).event(EventType.IDENTITY_PROVIDER_LOGIN);
    }

    @POST
    @Path("/{provider_id}/login")
    public Response performPostLogin(@PathParam("provider_id") String providerId, @QueryParam("code") String code) {
        return performLogin(providerId, code);
    }

    @GET
    @Path("/{provider_id}/login")
    public Response performLogin(@PathParam("provider_id") String providerId, @QueryParam("code") String code) {
        this.event.detail(Details.IDENTITY_PROVIDER, providerId);

        if (isDebugEnabled()) {
            logger.debugf("Sending authentication request to identity provider [%s].", providerId);
        }

        try {
            ParsedCodeContext parsedCode = parseClientSessionCode(code);
            if (parsedCode.response != null) {
                return parsedCode.response;
            }

            ClientSessionCode clientSessionCode = parsedCode.clientSessionCode;
            IdentityProvider identityProvider = getIdentityProvider(session, realmModel, providerId);
            Response response = identityProvider.performLogin(createAuthenticationRequest(providerId, clientSessionCode));

            if (response != null) {
                if (isDebugEnabled()) {
                    logger.debugf("Identity provider [%s] is going to send a request [%s].", identityProvider, response);
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
                    return corsResponse(forbidden("Client [" + clientModel.getClientId() + "] not authorized to retrieve tokens from identity provider [" + providerId + "]."), clientModel);

                }

                IdentityProvider identityProvider = getIdentityProvider(session, realmModel, providerId);
                IdentityProviderModel identityProviderConfig = getIdentityProviderConfig(providerId);

                if (identityProviderConfig.isStoreToken()) {
                    FederatedIdentityModel identity = this.session.users().getFederatedIdentity(authResult.getUser(), providerId, this.realmModel);

                    if (identity == null) {
                        return corsResponse(badRequest("User [" + authResult.getUser().getId() + "] is not associated with identity provider [" + providerId + "]."), clientModel);
                    }

                    this.event.success();

                    return corsResponse(identityProvider.retrieveToken(session, identity), clientModel);
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
        IdentityProviderModel identityProviderConfig = context.getIdpConfig();

        ParsedCodeContext parsedCode = parseClientSessionCode(context.getCode());
        if (parsedCode.response != null) {
            return parsedCode.response;
        }
        ClientSessionCode clientCode = parsedCode.clientSessionCode;

        String providerId = identityProviderConfig.getAlias();
        if (!identityProviderConfig.isStoreToken()) {
            if (isDebugEnabled()) {
                logger.debugf("Token will not be stored for identity provider [%s].", providerId);
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

            logger.debugf("Federated user not found for provider '%s' and broker username '%s' . Redirecting to flow for firstBrokerLogin", providerId, context.getUsername());

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

            clientSession.setTimestamp(Time.currentTime());

            SerializedBrokeredIdentityContext ctx = SerializedBrokeredIdentityContext.serialize(context);
            ctx.saveToClientSession(clientSession, AbstractIdpAuthenticator.BROKERED_CONTEXT_NOTE);

            URI redirect = LoginActionsService.firstBrokerLoginProcessor(uriInfo)
                    .queryParam(OAuth2Constants.CODE, context.getCode())
                    .build(realmModel.getName());
            return Response.status(302).location(redirect).build();

        } else {
            Response response = validateUser(federatedUser, realmModel);
            if (response != null) {
                return response;
            }

            updateFederatedIdentity(context, federatedUser);
            clientSession.setAuthenticatedUser(federatedUser);

            return finishOrRedirectToPostBrokerLogin(clientSession, context, false);
        }
    }

    public Response validateUser(UserModel user, RealmModel realm) {
        if (!user.isEnabled()) {
            event.error(Errors.USER_DISABLED);
            return ErrorPage.error(session, Messages.ACCOUNT_DISABLED);
        }
        if (realm.isBruteForceProtected()) {
            if (session.getProvider(BruteForceProtector.class).isTemporarilyDisabled(session, realm, user)) {
                event.error(Errors.USER_TEMPORARILY_DISABLED);
                return ErrorPage.error(session, Messages.ACCOUNT_DISABLED);
            }
        }
        return null;
    }

    // Callback from LoginActionsService after first login with broker was done and Keycloak account is successfully linked/created
    @GET
    @Path("/after-first-broker-login")
    public Response afterFirstBrokerLogin(@QueryParam("code") String code) {
        ParsedCodeContext parsedCode = parseClientSessionCode(code);
        if (parsedCode.response != null) {
            return parsedCode.response;
        }
        ClientSessionModel clientSession = parsedCode.clientSessionCode.getClientSession();

        try {
            this.event.detail(Details.CODE_ID, clientSession.getId())
                    .removeDetail("auth_method");

            SerializedBrokeredIdentityContext serializedCtx = SerializedBrokeredIdentityContext.readFromClientSession(clientSession, AbstractIdpAuthenticator.BROKERED_CONTEXT_NOTE);
            if (serializedCtx == null) {
                throw new IdentityBrokerException("Not found serialized context in clientSession");
            }
            BrokeredIdentityContext context = serializedCtx.deserialize(session, clientSession);
            String providerId = context.getIdpConfig().getAlias();

            event.detail(Details.IDENTITY_PROVIDER, providerId);
            event.detail(Details.IDENTITY_PROVIDER_USERNAME, context.getUsername());

            // firstBrokerLogin workflow finished. Removing note now
            clientSession.removeNote(AbstractIdpAuthenticator.BROKERED_CONTEXT_NOTE);

            UserModel federatedUser = clientSession.getAuthenticatedUser();
            if (federatedUser == null) {
                throw new IdentityBrokerException("Couldn't found authenticated federatedUser in clientSession");
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
            FederatedIdentityModel federatedIdentityModel = new FederatedIdentityModel(context.getIdpConfig().getAlias(), context.getId(),
                    context.getUsername(), context.getToken());
            session.users().addFederatedIdentity(realmModel, federatedUser, federatedIdentityModel);


            String isRegisteredNewUser = clientSession.getNote(AbstractIdpAuthenticator.BROKER_REGISTERED_NEW_USER);
            if (Boolean.parseBoolean(isRegisteredNewUser)) {

                logger.debugf("Registered new user '%s' after first login with identity provider '%s'. Identity provider username is '%s' . ", federatedUser.getUsername(), providerId, context.getUsername());

                context.getIdp().importNewUser(session, realmModel, federatedUser, context);
                Set<IdentityProviderMapperModel> mappers = realmModel.getIdentityProviderMappersByAlias(providerId);
                if (mappers != null) {
                    KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
                    for (IdentityProviderMapperModel mapper : mappers) {
                        IdentityProviderMapper target = (IdentityProviderMapper)sessionFactory.getProviderFactory(IdentityProviderMapper.class, mapper.getIdentityProviderMapper());
                        target.importNewUser(session, realmModel, federatedUser, mapper, context);
                    }
                }

                if (context.getIdpConfig().isTrustEmail() && !Validation.isBlank(federatedUser.getEmail()) && !Boolean.parseBoolean(clientSession.getNote(AbstractIdpAuthenticator.UPDATE_PROFILE_EMAIL_CHANGED))) {
                    logger.debugf("Email verified automatically after registration of user '%s' through Identity provider '%s' ", federatedUser.getUsername(), context.getIdpConfig().getAlias());
                    federatedUser.setEmailVerified(true);
                }

                event.event(EventType.REGISTER)
                        .detail(Details.REGISTER_METHOD, "broker")
                        .detail(Details.EMAIL, federatedUser.getEmail())
                        .success();

            } else {
                logger.debugf("Linked existing keycloak user '%s' with identity provider '%s' . Identity provider username is '%s' .", federatedUser.getUsername(), providerId, context.getUsername());

                event.event(EventType.FEDERATED_IDENTITY_LINK)
                        .success();

                updateFederatedIdentity(context, federatedUser);
            }

            return finishOrRedirectToPostBrokerLogin(clientSession, context, true);

        }  catch (Exception e) {
            return redirectToErrorPage(Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR, e);
        }
    }


    private Response finishOrRedirectToPostBrokerLogin(ClientSessionModel clientSession, BrokeredIdentityContext context, boolean wasFirstBrokerLogin) {
        String postBrokerLoginFlowId = context.getIdpConfig().getPostBrokerLoginFlowId();
        if (postBrokerLoginFlowId == null) {

            logger.debugf("Skip redirect to postBrokerLogin flow. PostBrokerLogin flow not set for identityProvider '%s'.", context.getIdpConfig().getAlias());
            return afterPostBrokerLoginFlowSuccess(clientSession, context, wasFirstBrokerLogin);
        } else {

            logger.debugf("Redirect to postBrokerLogin flow after authentication with identityProvider '%s'.", context.getIdpConfig().getAlias());

            clientSession.setTimestamp(Time.currentTime());

            SerializedBrokeredIdentityContext ctx = SerializedBrokeredIdentityContext.serialize(context);
            ctx.saveToClientSession(clientSession, PostBrokerLoginConstants.PBL_BROKERED_IDENTITY_CONTEXT);

            clientSession.setNote(PostBrokerLoginConstants.PBL_AFTER_FIRST_BROKER_LOGIN, String.valueOf(wasFirstBrokerLogin));

            URI redirect = LoginActionsService.postBrokerLoginProcessor(uriInfo)
                    .queryParam(OAuth2Constants.CODE, context.getCode())
                    .build(realmModel.getName());
            return Response.status(302).location(redirect).build();
        }
    }


    // Callback from LoginActionsService after postBrokerLogin flow is finished
    @GET
    @Path("/after-post-broker-login")
    public Response afterPostBrokerLoginFlow(@QueryParam("code") String code) {
        ParsedCodeContext parsedCode = parseClientSessionCode(code);
        if (parsedCode.response != null) {
            return parsedCode.response;
        }
        ClientSessionModel clientSession = parsedCode.clientSessionCode.getClientSession();

        try {
            SerializedBrokeredIdentityContext serializedCtx = SerializedBrokeredIdentityContext.readFromClientSession(clientSession, PostBrokerLoginConstants.PBL_BROKERED_IDENTITY_CONTEXT);
            if (serializedCtx == null) {
                throw new IdentityBrokerException("Not found serialized context in clientSession. Note " + PostBrokerLoginConstants.PBL_BROKERED_IDENTITY_CONTEXT + " was null");
            }
            BrokeredIdentityContext context = serializedCtx.deserialize(session, clientSession);

            String wasFirstBrokerLoginNote = clientSession.getNote(PostBrokerLoginConstants.PBL_AFTER_FIRST_BROKER_LOGIN);
            boolean wasFirstBrokerLogin = Boolean.parseBoolean(wasFirstBrokerLoginNote);

            // Ensure the post-broker-login flow was successfully finished
            String authStateNoteKey = PostBrokerLoginConstants.PBL_AUTH_STATE_PREFIX + context.getIdpConfig().getAlias();
            String authState = clientSession.getNote(authStateNoteKey);
            if (!Boolean.parseBoolean(authState)) {
                throw new IdentityBrokerException("Invalid request. Not found the flag that post-broker-login flow was finished");
            }

            // remove notes
            clientSession.removeNote(PostBrokerLoginConstants.PBL_BROKERED_IDENTITY_CONTEXT);
            clientSession.removeNote(PostBrokerLoginConstants.PBL_AFTER_FIRST_BROKER_LOGIN);

            return afterPostBrokerLoginFlowSuccess(clientSession, context, wasFirstBrokerLogin);
        } catch (IdentityBrokerException e) {
            return redirectToErrorPage(Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR, e);
        }
    }

    private Response afterPostBrokerLoginFlowSuccess(ClientSessionModel clientSession, BrokeredIdentityContext context, boolean wasFirstBrokerLogin) {
        String providerId = context.getIdpConfig().getAlias();
        UserModel federatedUser = clientSession.getAuthenticatedUser();

        if (wasFirstBrokerLogin) {

            String isDifferentBrowser = clientSession.getNote(AbstractIdpAuthenticator.IS_DIFFERENT_BROWSER);
            if (Boolean.parseBoolean(isDifferentBrowser)) {
                session.sessions().removeClientSession(realmModel, clientSession);
                return session.getProvider(LoginFormsProvider.class)
                        .setSuccess(Messages.IDENTITY_PROVIDER_LINK_SUCCESS, context.getIdpConfig().getAlias(), context.getUsername())
                        .createInfoPage();
            } else {
                return finishBrokerAuthentication(context, federatedUser, clientSession, providerId);
            }

        } else {

            boolean firstBrokerLoginInProgress = (clientSession.getNote(AbstractIdpAuthenticator.BROKERED_CONTEXT_NOTE) != null);
            if (firstBrokerLoginInProgress) {
                logger.debugf("Reauthenticated with broker '%s' when linking user '%s' with other broker", context.getIdpConfig().getAlias(), federatedUser.getUsername());

                UserModel linkingUser = AbstractIdpAuthenticator.getExistingUser(session, realmModel, clientSession);
                if (!linkingUser.getId().equals(federatedUser.getId())) {
                    return redirectToErrorPage(Messages.IDENTITY_PROVIDER_DIFFERENT_USER_MESSAGE, federatedUser.getUsername(), linkingUser.getUsername());
                }

                return afterFirstBrokerLogin(context.getCode());
            } else {
                return finishBrokerAuthentication(context, federatedUser, clientSession, providerId);
            }
        }
    }


    private Response finishBrokerAuthentication(BrokeredIdentityContext context, UserModel federatedUser, ClientSessionModel clientSession, String providerId) {
        UserSessionModel userSession = this.session.sessions()
                .createUserSession(this.realmModel, federatedUser, federatedUser.getUsername(), this.clientConnection.getRemoteAddr(), "broker", false, context.getBrokerSessionId(), context.getBrokerUserId());

        this.event.user(federatedUser);
        this.event.session(userSession);

        TokenManager.attachClientSession(userSession, clientSession);
        context.getIdp().attachUserSession(userSession, clientSession, context);
        userSession.setNote(Details.IDENTITY_PROVIDER, providerId);
        userSession.setNote(Details.IDENTITY_PROVIDER_USERNAME, context.getUsername());

        if (isDebugEnabled()) {
            logger.debugf("Performing local authentication for user [%s].", federatedUser);
        }

        return AuthenticationProcessor.redirectToRequiredActions(realmModel, clientSession, uriInfo);
    }


    @Override
    public Response cancelled(String code) {
        ParsedCodeContext parsedCode = parseClientSessionCode(code);
        if (parsedCode.response != null) {
            return parsedCode.response;
        }
        ClientSessionCode clientCode = parsedCode.clientSessionCode;

        Response accountManagementFailedLinking = checkAccountManagementFailedLinking(clientCode.getClientSession(), Messages.CONSENT_DENIED);
        if (accountManagementFailedLinking != null) {
            return accountManagementFailedLinking;
        }

        return browserAuthentication(clientCode.getClientSession(), null);
    }

    @Override
    public Response error(String code, String message) {
        ParsedCodeContext parsedCode = parseClientSessionCode(code);
        if (parsedCode.response != null) {
            return parsedCode.response;
        }
        ClientSessionCode clientCode = parsedCode.clientSessionCode;

        Response accountManagementFailedLinking = checkAccountManagementFailedLinking(clientCode.getClientSession(), message);
        if (accountManagementFailedLinking != null) {
            return accountManagementFailedLinking;
        }

        return browserAuthentication(clientCode.getClientSession(), message);
    }

    private Response performAccountLinking(ClientSessionModel clientSession, BrokeredIdentityContext context, FederatedIdentityModel federatedIdentityModel, UserModel federatedUser) {
        this.event.event(EventType.FEDERATED_IDENTITY_LINK);

        if (federatedUser != null) {
            return redirectToAccountErrorPage(clientSession, Messages.IDENTITY_PROVIDER_ALREADY_LINKED, context.getIdpConfig().getAlias());
        }

        UserModel authenticatedUser = clientSession.getUserSession().getUser();

        if (isDebugEnabled()) {
            logger.debugf("Linking account [%s] from identity provider [%s] to user [%s].", federatedIdentityModel, context.getIdpConfig().getAlias(), authenticatedUser);
        }

        if (!authenticatedUser.isEnabled()) {
            return redirectToAccountErrorPage(clientSession, Messages.ACCOUNT_DISABLED);
        }

        if (!authenticatedUser.hasRole(this.realmModel.getClientByClientId(ACCOUNT_MANAGEMENT_CLIENT_ID).getRole(MANAGE_ACCOUNT))) {
            return redirectToErrorPage(Messages.INSUFFICIENT_PERMISSION);
        }

        this.session.users().addFederatedIdentity(this.realmModel, authenticatedUser, federatedIdentityModel);
        context.getIdp().attachUserSession(clientSession.getUserSession(), clientSession, context);

        this.event.user(authenticatedUser)
                .detail(Details.USERNAME, authenticatedUser.getUsername())
                .detail(Details.IDENTITY_PROVIDER, federatedIdentityModel.getIdentityProvider())
                .detail(Details.IDENTITY_PROVIDER_USERNAME, federatedIdentityModel.getUserName())
                .success();
        return Response.status(302).location(UriBuilder.fromUri(clientSession.getRedirectUri()).build()).build();
    }

    private void updateFederatedIdentity(BrokeredIdentityContext context, UserModel federatedUser) {
        FederatedIdentityModel federatedIdentityModel = this.session.users().getFederatedIdentity(federatedUser, context.getIdpConfig().getAlias(), this.realmModel);

        // Skip DB write if tokens are null or equal
        if (context.getIdpConfig().isStoreToken() && !ObjectUtil.isEqualOrBothNull(context.getToken(), federatedIdentityModel.getToken())) {
            federatedIdentityModel.setToken(context.getToken());

            this.session.users().updateFederatedIdentity(this.realmModel, federatedUser, federatedIdentityModel);

            if (isDebugEnabled()) {
                logger.debugf("Identity [%s] update with response from identity provider [%s].", federatedUser, context.getIdpConfig().getAlias());
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

    private ParsedCodeContext parseClientSessionCode(String code) {
        ClientSessionCode clientCode = ClientSessionCode.parse(code, this.session, this.realmModel);

        if (clientCode != null) {
            ClientSessionModel clientSession = clientCode.getClientSession();

            if (clientSession.getUserSession() != null) {
                this.event.session(clientSession.getUserSession());
            }

            ClientModel client = clientSession.getClient();

            if (client != null) {

                logger.debugf("Got authorization code from client [%s].", client.getClientId());
                this.event.client(client);
                this.session.getContext().setClient(client);

                if (!clientCode.isValid(AUTHENTICATE.name(), ClientSessionCode.ActionType.LOGIN)) {
                    logger.debugf("Authorization code is not valid. Client session ID: %s, Client session's action: %s", clientSession.getId(), clientSession.getAction());

                    // Check if error happened during login or during linking from account management
                    Response accountManagementFailedLinking = checkAccountManagementFailedLinking(clientCode.getClientSession(), Messages.STALE_CODE_ACCOUNT);
                    Response staleCodeError = (accountManagementFailedLinking != null) ? accountManagementFailedLinking : redirectToErrorPage(Messages.STALE_CODE);


                    return ParsedCodeContext.response(staleCodeError);
                }

                if (isDebugEnabled()) {
                    logger.debugf("Authorization code is valid.");
                }

                return ParsedCodeContext.clientSessionCode(clientCode);
            }
        }

        logger.debugf("Authorization code is not valid. Code: %s", code);
        Response staleCodeError = redirectToErrorPage(Messages.STALE_CODE);
        return ParsedCodeContext.response(staleCodeError);
    }

    private Response checkAccountManagementFailedLinking(ClientSessionModel clientSession, String error, Object... parameters) {
        if (clientSession.getUserSession() != null && clientSession.getClient() != null && clientSession.getClient().getClientId().equals(ACCOUNT_MANAGEMENT_CLIENT_ID)) {

            this.event.event(EventType.FEDERATED_IDENTITY_LINK);
            UserModel user = clientSession.getUserSession().getUser();
            this.event.user(user);
            this.event.detail(Details.USERNAME, user.getUsername());

            return redirectToAccountErrorPage(clientSession, error, parameters);
        } else {
            return null;
        }
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

    private Response redirectToAccountErrorPage(ClientSessionModel clientSession, String message, Object ... parameters) {
        fireErrorEvent(message);

        FormMessage errorMessage = new FormMessage(message, parameters);
        try {
            String serializedError = JsonSerialization.writeValueAsString(errorMessage);
            clientSession.setNote(AccountService.ACCOUNT_MGMT_FORWARDED_ERROR_NOTE, serializedError);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        return Response.status(302).location(UriBuilder.fromUri(clientSession.getRedirectUri()).build()).build();
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
                .setBrowserFlow(true)
                .setConnection(clientConnection)
                .setEventBuilder(event)
                .setRealm(realmModel)
                .setSession(session)
                .setUriInfo(uriInfo)
                .setRequest(request);
        if (errorMessage != null) processor.setForwardedErrorMessage(new FormMessage(null, errorMessage));

        try {
            CacheControlUtil.noBackButtonCacheControlHeader();
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
        IdentityProviderModel model = this.realmModel.getIdentityProviderByAlias(providerId);
        if (model == null) {
            throw new IdentityBrokerException("Configuration for identity provider [" + providerId + "] not found.");
        }
        return model;
    }

    private Response corsResponse(Response response, ClientModel clientModel) {
        return Cors.add(this.request, Response.fromResponse(response)).auth().allowedOrigins(clientModel).build();
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
                logger.couldNotFireEvent(e);
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


    private static class ParsedCodeContext {
        private ClientSessionCode clientSessionCode;
        private Response response;

        public static ParsedCodeContext clientSessionCode(ClientSessionCode clientSessionCode) {
            ParsedCodeContext ctx = new ParsedCodeContext();
            ctx.clientSessionCode = clientSessionCode;
            return ctx;
        }

        public static ParsedCodeContext response(Response response) {
            ParsedCodeContext ctx = new ParsedCodeContext();
            ctx.response = response;
            return ctx;
        }
    }
}

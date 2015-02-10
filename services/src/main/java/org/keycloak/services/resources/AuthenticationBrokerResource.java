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
import org.keycloak.ClientConnection;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.AuthenticationResponse;
import org.keycloak.broker.provider.FederatedIdentity;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationManager.AuthResult;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.managers.EventsManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.flows.Flows;
import org.keycloak.social.SocialIdentityProvider;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.keycloak.models.AccountRoles.MANAGE_ACCOUNT;
import static org.keycloak.models.ClientSessionModel.Action.AUTHENTICATE;
import static org.keycloak.models.Constants.ACCOUNT_MANAGEMENT_APP;
import static org.keycloak.models.UserModel.RequiredAction.UPDATE_PROFILE;

/**
 * @author Pedro Igor
 */
@Path("/broker")
public class AuthenticationBrokerResource {

    private static final Logger logger = Logger.getLogger(AuthenticationBrokerResource.class);

    @Context
    private UriInfo uriInfo;

    @Context
    private KeycloakSession session;

    @Context
    private ClientConnection clientConnection;

    @Context
    private HttpRequest request;

    @GET
    @Path("{realm}/login")
    public Response performLogin(@PathParam("realm") String realmName,
            @QueryParam("provider_id") String providerId,
            @QueryParam("code") String code) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(realmName);
        ClientSessionCode clientCode = isValidAuthorizationCode(code, realm);

        if (clientCode == null) {
            return redirectToErrorPage(realm, "Invalid code, please login again through your application.");
        }

        EventBuilder event = new EventsManager(realm, session, clientConnection).createEventBuilder()
                .event(EventType.LOGIN)
                .detail(Details.AUTH_METHOD, "unknown_id@" + providerId);

        try {
            ClientSessionModel clientSession = clientCode.getClientSession();
            ClientModel clientModel = clientSession.getClient();
            Response response = checkClientPermissions(clientModel, providerId);

            if (response != null) {
                return response;
            }

            IdentityProvider identityProvider = getIdentityProvider(realm, providerId);

            if (identityProvider == null) {
                event.error(Errors.IDENTITY_PROVIDER_NOT_FOUND);
                return Flows.forms(session, realm, null, uriInfo).setError("Identity Provider not found").createErrorPage();
            }

            AuthenticationResponse authenticationResponse = identityProvider.handleRequest(createAuthenticationRequest(providerId, code, realm,
                    clientSession));

            response = authenticationResponse.getResponse();

            if (response != null) {
                event.success();
                return response;
            }
        } catch (Exception e) {
            logger.error("Could not send authentication request to identity provider " + providerId, e);
            String message = "Could not send authentication request to identity provider";
            event.error(message);
            return redirectToErrorPage(realm, message);
        }

        String message = "Could not proceed with authentication request to identity provider.";

        event.error(message);

        return redirectToErrorPage(realm, message);
    }

    @GET
    @Path("{realm}/{provider_id}")
    public Response handleResponseGet(@PathParam("realm") final String realmName,
            @PathParam("provider_id") String providerId) {
        return handleResponse(realmName, providerId);
    }

    @POST
    @Path("{realm}/{provider_id}")
    public Response handleResponsePost(@PathParam("realm") final String realmName,
            @PathParam("provider_id") String providerId) {
        return handleResponse(realmName, providerId);
    }

    @Path("{realm}/{provider_id}/token")
    @OPTIONS
    public Response retrieveTokenPreflight() {
        return Cors.add(this.request, Response.ok()).auth().preflight().build();
    }

    @GET
    @Path("{realm}/{provider_id}/token")
    public Response retrieveToken(@PathParam("realm") final String realmName, @PathParam("provider_id") String providerId) {
        return getToken(realmName, providerId, false);
    }

    private Response getToken(String realmName, String providerId, boolean forceRetrieval) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(realmName);
        AppAuthManager authManager = new AppAuthManager();
        AuthResult authResult = authManager.authenticateBearerToken(session, realm, uriInfo, clientConnection, request.getHttpHeaders());

        if (authResult != null) {
            String audience = authResult.getToken().getAudience();
            ClientModel clientModel = realm.findClient(audience);

            if (clientModel == null) {
                return corsResponse(Flows.errors().error("Invalid client.", Response.Status.FORBIDDEN), clientModel);
            }

            if (!clientModel.hasIdentityProvider(providerId)) {
                return corsResponse(Flows.errors().error("Client [" + audience + "] not authorized.", Response.Status.FORBIDDEN), clientModel);
            }

            if (OAuthClientModel.class.isInstance(clientModel) && !forceRetrieval) {
                return corsResponse(Flows.forms(session, realm, clientModel, uriInfo)
                        .setClientSessionCode(authManager.extractAuthorizationHeaderToken(request.getHttpHeaders()))
                        .setAccessRequest("Your information from " + providerId + " identity provider.")
                        .setClient(clientModel)
                        .setUriInfo(this.uriInfo)
                        .setActionUri(this.uriInfo.getRequestUri())
                        .createOAuthGrant(), clientModel);
            }

            IdentityProvider identityProvider = getIdentityProvider(realm, providerId);
            IdentityProviderModel identityProviderConfig = getIdentityProviderConfig(realm, providerId);

            if (identityProviderConfig.isStoreToken()) {
                FederatedIdentityModel identity = this.session.users().getFederatedIdentity(authResult.getUser(), providerId, realm);

                if (identity == null) {
                    return corsResponse(Flows.errors().error("User [" + authResult.getUser().getId() + "] is not associated with identity provider [" + providerId + "].", Response.Status.FORBIDDEN), clientModel);
                }

                return corsResponse(identityProvider.retrieveToken(identity), clientModel);
            }

            return corsResponse(Flows.errors().error("Identity Provider [" + providerId + "] does not support this operation.", Response.Status.FORBIDDEN), clientModel);
        }

        return Flows.errors().error("Invalid code.", Response.Status.FORBIDDEN);
    }

    @POST
    @Path("{realm}/{provider_id}/token")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response consentTokenRetrieval(@PathParam("realm") final String realmName, @PathParam("provider_id") String providerId,
                                          final MultivaluedMap<String, String> formData) {
        if (formData.containsKey("cancel")) {
            return Flows.errors().error("Permission not approved.", Response.Status.FORBIDDEN);
        }

        return getToken(realmName, providerId, true);
    }

    private Response handleResponse(String realmName, String providerId) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(realmName);

        IdentityProviderModel identityProviderConfig = getIdentityProviderConfig(realm, providerId);

        try {
            IdentityProvider identityProvider = getIdentityProvider(realm, providerId);

            if (identityProvider == null) {
                return Flows.forms(session, realm, null, uriInfo).setError("Social identityProvider not found").createErrorPage();
            }

            String relayState = identityProvider.getRelayState(createAuthenticationRequest(providerId, null, realm, null));

            if (relayState == null) {
                return redirectToErrorPage(realm, "No relay state from identity identityProvider.");
            }

            ClientSessionCode clientCode = isValidAuthorizationCode(relayState, realm);

            if (clientCode == null) {
                return redirectToErrorPage(realm, "Invalid authorization code, please login again through your application.");
            }

            ClientSessionModel clientSession = clientCode.getClientSession();
            ClientModel clientModel = clientSession.getClient();
            Response response = checkClientPermissions(clientModel, providerId);

            if (response != null) {
                return response;
            }

            AuthenticationResponse authenticationResponse = identityProvider.handleResponse(createAuthenticationRequest(providerId, null, realm, clientSession));

            response = authenticationResponse.getResponse();

            if (response != null) {
                return response;
            }

            FederatedIdentity identity = authenticationResponse.getUser();

            if (!identityProviderConfig.isStoreToken()) {
                identity.setToken(null);
            }

            return performLocalAuthentication(realm, providerId, identity, clientCode);
        } catch (Exception e) {
            logger.error("Could not authenticate user against provider " + providerId, e);
            if (session.getTransaction().isActive()) {
                session.getTransaction().rollback();
            }

            return Flows.forms(session, realm, null, uriInfo).setError("Authentication failed. Could not authenticate against Identity Provider [" + identityProviderConfig.getName() + "].").createErrorPage();
        } finally {
            if (session.getTransaction().isActive()) {
                session.getTransaction().commit();
            }
        }
    }

    private Response performLocalAuthentication(RealmModel realm, String providerId, FederatedIdentity updatedIdentity, ClientSessionCode clientCode) {
        ClientSessionModel clientSession = clientCode.getClientSession();
        FederatedIdentityModel federatedIdentityModel = new FederatedIdentityModel(providerId, updatedIdentity.getId(),
                updatedIdentity.getUsername(), updatedIdentity.getToken());
        UserModel federatedUser = session.users().getUserByFederatedIdentity(federatedIdentityModel, realm);
        IdentityProviderModel identityProviderConfig = getIdentityProviderConfig(realm, providerId);

        String authMethod = federatedIdentityModel.getUserId() + "@" + identityProviderConfig.getId();
        EventBuilder event = new EventsManager(realm, session, clientConnection).createEventBuilder()
                .event(EventType.LOGIN)
                .client(clientSession.getClient())
                .detail(Details.REDIRECT_URI, clientSession.getRedirectUri())
                .detail(Details.AUTH_METHOD, authMethod);

        event.detail(Details.USERNAME, authMethod);

        // Check if federatedUser is already authenticated (this means linking social into existing federatedUser account)
        if (clientSession.getUserSession() != null) {
            UserModel authenticatedUser = clientSession.getUserSession().getUser();

            if (federatedUser != null) {
                String message = "The updatedIdentity returned by the Identity Provider [" + identityProviderConfig.getName() + "] is already linked to other user";
                event.error(message);
                return redirectToErrorPage(realm, message);
            }

            if (!authenticatedUser.isEnabled()) {
                event.error(Errors.USER_DISABLED);
                return redirectToErrorPage(realm, "User is disabled");
            }

            if (!authenticatedUser.hasRole(realm.getApplicationByName(ACCOUNT_MANAGEMENT_APP).getRole(MANAGE_ACCOUNT))) {
                event.error(Errors.NOT_ALLOWED);
                return redirectToErrorPage(realm, "Insufficient permissions to link updatedIdentity");
            }

            session.users().addFederatedIdentity(realm, authenticatedUser, federatedIdentityModel);

            event.success();

            return Response.status(302).location(UriBuilder.fromUri(clientSession.getRedirectUri()).build()).build();
        }

        if (federatedUser == null) {

            String errorMessage = null;

            // Check if no user already exists with this username or email
            UserModel existingUser = session.users().getUserByEmail(updatedIdentity.getEmail(), realm);
            if (existingUser != null) {
                event.error(Errors.FEDERATED_IDENTITY_EMAIL_EXISTS);
                errorMessage = "federatedIdentityEmailExists";
            } else {
                existingUser = session.users().getUserByUsername(updatedIdentity.getUsername(), realm);
                if (existingUser != null) {
                    event.error(Errors.FEDERATED_IDENTITY_USERNAME_EXISTS);
                    errorMessage = "federatedIdentityUsernameExists";
                }
            }

            // Check if realm registration is allowed
            if (!realm.isRegistrationAllowed()) {
                event.error(Errors.FEDERATED_IDENTITY_DISABLED_REGISTRATION);
                errorMessage = "federatedIdentityDisabledRegistration";
            }

            if (errorMessage == null) {
                logger.debug("Creating user " + updatedIdentity.getUsername() + " and linking to federation provider " + providerId);
                federatedUser = session.users().addUser(realm, updatedIdentity.getUsername());
                federatedUser.setEnabled(true);
                federatedUser.setFirstName(updatedIdentity.getFirstName());
                federatedUser.setLastName(updatedIdentity.getLastName());
                federatedUser.setEmail(updatedIdentity.getEmail());

                session.users().addFederatedIdentity(realm, federatedUser, federatedIdentityModel);

                event.clone().user(federatedUser).event(EventType.REGISTER)
                        .detail(Details.REGISTER_METHOD, authMethod)
                        .detail(Details.EMAIL, federatedUser.getEmail())
                        .removeDetail("auth_method")
                        .success();

                if (identityProviderConfig.isUpdateProfileFirstLogin()) {
                    federatedUser.addRequiredAction(UPDATE_PROFILE);
                }
            } else {
                return Flows.forms(session, realm, clientSession.getClient(), uriInfo)
                        .setClientSessionCode(clientCode.getCode())
                        .setError(errorMessage)
                        .createLogin();
            }
        }

        federatedIdentityModel = this.session.users().getFederatedIdentity(federatedUser, providerId, realm);

        federatedIdentityModel.setToken(updatedIdentity.getToken());

        this.session.users().updateFederatedIdentity(realm, federatedUser, federatedIdentityModel);

        event.user(federatedUser);

        String username = federatedIdentityModel.getUserId() + "@" + identityProviderConfig.getName();

        UserSessionModel userSession = session.sessions()
                .createUserSession(realm, federatedUser, username, clientConnection.getRemoteAddr(), "broker", false);

        event.session(userSession);

        TokenManager.attachClientSession(userSession, clientSession);

        AuthenticationManager authManager = new AuthenticationManager();

        return authManager.nextActionAfterAuthentication(session, userSession, clientSession, clientConnection, request,
                uriInfo, event);
    }

    private ClientSessionCode isValidAuthorizationCode(String code, RealmModel realm) {
        ClientSessionCode clientCode = ClientSessionCode.parse(code, this.session, realm);

        if (clientCode != null && clientCode.isValid(AUTHENTICATE)) {
            return clientCode;
        }

        return null;
    }

    private AuthenticationRequest createAuthenticationRequest(String providerId, String code, RealmModel realm, ClientSessionModel clientSession) {
        return new AuthenticationRequest(realm, clientSession, this.request, this.uriInfo, code, getRedirectUri(providerId, realm));
    }

    private String getRedirectUri(String providerId, RealmModel realm) {
        return UriBuilder.fromUri(this.uriInfo.getBaseUri())
                .path(AuthenticationBrokerResource.class)
                .path(AuthenticationBrokerResource.class, "handleResponseGet")
                .build(realm.getName(), providerId)
                .toString();
    }

    private Response redirectToErrorPage(RealmModel realm, String message) {
        return Flows.forwardToSecurityFailurePage(this.session, realm, uriInfo, message);
    }

    private IdentityProvider getIdentityProvider(RealmModel realm, String providerId) {
        IdentityProviderModel identityProviderModel = realm.getIdentityProviderById(providerId);

        if (identityProviderModel != null) {
            IdentityProviderFactory providerFactory = getIdentityProviderFactory(identityProviderModel);

            if (providerFactory == null) {
                throw new RuntimeException("Could not find provider factory for identity provider [" + providerId + "].");
            }

            return providerFactory.create(identityProviderModel);
        }

        return null;
    }

    private IdentityProviderFactory getIdentityProviderFactory(IdentityProviderModel model) {
        Map<String, IdentityProviderFactory> availableProviders = new HashMap<String, IdentityProviderFactory>();
        List<ProviderFactory> allProviders = new ArrayList<ProviderFactory>();

        allProviders.addAll(this.session.getKeycloakSessionFactory().getProviderFactories(IdentityProvider.class));
        allProviders.addAll(this.session.getKeycloakSessionFactory().getProviderFactories(SocialIdentityProvider.class));

        for (ProviderFactory providerFactory : allProviders) {
            availableProviders.put(providerFactory.getId(), (IdentityProviderFactory) providerFactory);
        }

        return availableProviders.get(model.getProviderId());
    }

    private IdentityProviderModel getIdentityProviderConfig(RealmModel realm, String providerId) {
        for (IdentityProviderModel model : realm.getIdentityProviders()) {
            if (model.getId().equals(providerId)) {
                return model;
            }
        }

        return null;
    }

    private Response checkClientPermissions(ClientModel clientModel, String providerId) {
        if (clientModel == null) {
            return Flows.errors().error("Invalid client.", Response.Status.FORBIDDEN);
        }

        if (!clientModel.hasIdentityProvider(providerId)) {
            return Flows.errors().error("Client [" + clientModel.getClientId() + "] not authorized.", Response.Status.FORBIDDEN);
        }

        return null;
    }

    private Response corsResponse(Response response, ClientModel clientModel) {
        return Cors.add(request, Response.fromResponse(response)).auth().allowedOrigins(clientModel).build();
    }
}

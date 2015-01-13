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
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.managers.EventsManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.flows.Flows;
import org.keycloak.social.SocialIdentityProvider;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
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
            IdentityProvider identityProvider = getIdentityProvider(realm, providerId);

            if (identityProvider == null) {
                event.error(Errors.IDENTITY_PROVIDER_NOT_FOUND);
                return Flows.forms(session, realm, null, uriInfo).setError("Identity Provider not found").createErrorPage();
            }

            AuthenticationResponse authenticationResponse = identityProvider.handleRequest(createAuthenticationRequest(providerId, code, realm,
                    clientSession));
            Response response = authenticationResponse.getResponse();

            if (response != null) {
                event.success();
                return response;
            }
        } catch (Exception e) {
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

    private Response handleResponse(String realmName, String providerId) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(realmName);

        try {
            IdentityProvider provider = getIdentityProvider(realm, providerId);

            if (provider == null) {
                return Flows.forms(session, realm, null, uriInfo).setError("Social provider not found").createErrorPage();
            }

            String relayState = provider.getRelayState(createAuthenticationRequest(providerId, null, realm, null));

            if (relayState == null) {
                return redirectToErrorPage(realm, "No authorization code provided.");
            }

            ClientSessionCode clientCode = isValidAuthorizationCode(relayState, realm);

            if (clientCode == null) {
                return redirectToErrorPage(realm, "Invalid authorization code, please login again through your application.");
            }

            ClientSessionModel clientSession = clientCode.getClientSession();

            AuthenticationResponse authenticationResponse = provider.handleResponse(createAuthenticationRequest(providerId, null, realm, clientSession));
            Response response = authenticationResponse.getResponse();

            if (response != null) {
                return response;
            }

            FederatedIdentity socialUser = authenticationResponse.getUser();

            return performLocalAuthentication(realm, providerId, socialUser, clientCode);
        } catch (Exception e) {
            if (session.getTransaction().isActive()) {
                session.getTransaction().rollback();
            }

            IdentityProviderModel identityProviderConfig = getIdentityProviderConfig(realm, providerId);

            return Flows.forms(session, realm, null, uriInfo).setError("Authentication failed. Could not authenticate against Identity Provider [" + identityProviderConfig.getName() + "].").createErrorPage();
        } finally {
            if (session.getTransaction().isActive()) {
                session.getTransaction().commit();
            }
        }
    }

    private Response performLocalAuthentication(RealmModel realm, String providerId, FederatedIdentity socialUser, ClientSessionCode clientCode) {
        ClientSessionModel clientSession = clientCode.getClientSession();
        FederatedIdentityModel socialLink = new FederatedIdentityModel(providerId, socialUser.getId(),
                socialUser.getUsername());
        UserModel federatedUser = session.users().getUserByFederatedIdentity(socialLink, realm);
        IdentityProviderModel identityProviderConfig = getIdentityProviderConfig(realm, providerId);

        String authMethod = socialLink.getUserId() + "@" + identityProviderConfig.getId();
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
                String message = "The identity returned by the Identity Provider [" + identityProviderConfig.getName() + "] is already linked to other user";
                event.error(message);
                return redirectToErrorPage(realm, message);
            }

            if (!authenticatedUser.isEnabled()) {
                event.error(Errors.USER_DISABLED);
                return redirectToErrorPage(realm, "User is disabled");
            }

            if (!authenticatedUser.hasRole(realm.getApplicationByName(ACCOUNT_MANAGEMENT_APP).getRole(MANAGE_ACCOUNT))) {
                event.error(Errors.NOT_ALLOWED);
                return redirectToErrorPage(realm, "Insufficient permissions to link identity");
            }

            session.users().addFederatedIdentity(realm, authenticatedUser, socialLink);

            event.success();

            return Response.status(302).location(UriBuilder.fromUri(clientSession.getRedirectUri()).build()).build();
        }

        UserModel user = session.users().getUserByEmail(socialUser.getEmail(), realm);
        String errorMessage = "federatedIdentityEmailExists";

        if (user == null) {
            user = session.users().getUserByUsername(socialUser.getUsername(), realm);
            errorMessage = "federatedIdentityUsernameExists";
        }

        if (user == null) {
            federatedUser = session.users().addUser(realm, socialUser.getUsername());
            federatedUser.setEnabled(true);
            federatedUser.setFirstName(socialUser.getFirstName());
            federatedUser.setLastName(socialUser.getLastName());
            federatedUser.setEmail(socialUser.getEmail());

            session.users().addFederatedIdentity(realm, federatedUser, socialLink);

            event.clone().user(federatedUser).event(EventType.REGISTER)
                    .detail(Details.REGISTER_METHOD, authMethod)
                    .detail(Details.EMAIL, federatedUser.getEmail())
                    .removeDetail("auth_method")
                    .success();

            if (identityProviderConfig.isUpdateProfileFirstLogin()) {
                federatedUser.addRequiredAction(UPDATE_PROFILE);
            }
        } else {
            if (federatedUser == null) {
                return Flows.forms(session, realm, clientSession.getClient(), uriInfo)
                        .setClientSessionCode(clientCode.getCode())
                        .setError(errorMessage)
                        .createLogin();
            }
        }

        event.user(federatedUser);

        String username = socialLink.getUserId() + "@" + identityProviderConfig.getName();

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
        for (IdentityProviderModel model : realm.getIdentityProviders()) {
            if (model.getId().equals(providerId)) {
                IdentityProviderFactory providerFactory = getIdentityProviderFactory(model);

                if (providerFactory == null) {
                    throw new RuntimeException("Could not find provider factory for identity provider [" + providerId + "].");
                }

                return providerFactory.create(model);
            }
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

}

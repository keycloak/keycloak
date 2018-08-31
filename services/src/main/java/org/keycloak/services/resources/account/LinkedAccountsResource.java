/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.services.resources.account;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.common.util.Base64Url;
import org.keycloak.credential.CredentialModel;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.account.AccountLinkUriRepresentation;
import org.keycloak.representations.account.LinkedAccountsRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.Auth;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.Cors;
import org.keycloak.services.validation.Validation;

/**
 * API for linking/unlinking social login accounts
 *
 * @author Stan Silvert
 */
public class LinkedAccountsResource {
    private static final Logger logger = Logger.getLogger(LinkedAccountsResource.class);
    
    private final KeycloakSession session;
    private final HttpRequest request;
    private final ClientModel client;
    private final EventBuilder event;
    private final UserModel user;
    private final RealmModel realm;
    private final Auth auth;

    public LinkedAccountsResource(KeycloakSession session, 
                                  HttpRequest request, 
                                  ClientModel client,
                                  Auth auth, 
                                  EventBuilder event, 
                                  UserModel user) {
        this.session = session;
        this.request = request;
        this.client = client;
        this.auth = auth;
        this.event = event;
        this.user = user;
        realm = session.getContext().getRealm();
    }
    
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response linkedAccounts() {
        LinkedAccountsBean linkedAccounts = new LinkedAccountsBean(this.session, this.realm, this.user);
        LinkedAccountsRepresentation rep = new LinkedAccountsRepresentation();
        rep.setHasAlternativeAuthentication(linkedAccounts.getHasAlternativeAuthentication());
        rep.setLinkedAccounts(linkedAccounts.getLinkedAccounts());
        return Cors.add(request, Response.ok(rep)).auth().allowedOrigins(auth.getToken()).build();
    }
    
    @GET
    @Path("/{providerId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response buildLinkedAccountURI(@PathParam("providerId") String providerId, 
                                     @QueryParam("redirectUri") String redirectUri) {
        if (redirectUri == null) {
            ErrorResponse.error(Messages.INVALID_REDIRECT_URI, Response.Status.BAD_REQUEST);
        }
        
        String errorMessage = checkCommonPreconditions(providerId);
        if (errorMessage != null) {
            return ErrorResponse.error(errorMessage, Response.Status.BAD_REQUEST);
        }
        
        try {
            String nonce = UUID.randomUUID().toString();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String input = nonce + auth.getSession().getId() +  client.getClientId() + providerId;
            byte[] check = md.digest(input.getBytes(StandardCharsets.UTF_8));
            String hash = Base64Url.encode(check);
            URI linkUri = Urls.identityProviderLinkRequest(this.session.getContext().getUri().getBaseUri(), providerId, realm.getName());
            linkUri = UriBuilder.fromUri(linkUri)
                    .queryParam("nonce", nonce)
                    .queryParam("hash", hash)
                    .queryParam("client_id", client.getClientId())
                    .queryParam("redirect_uri", redirectUri)
                    .build();
            
            AccountLinkUriRepresentation rep = new AccountLinkUriRepresentation();
            rep.setAccountLinkUri(linkUri);
            rep.setHash(hash);
            rep.setNonce(nonce);
            
            return Cors.add(request, Response.ok(rep)).auth().allowedOrigins(auth.getToken()).build();
        } catch (Exception spe) {
            spe.printStackTrace();
            return ErrorResponse.error(Messages.FAILED_TO_PROCESS_RESPONSE, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
    
    @DELETE
    @Path("/{providerId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeLinkedAccount(@PathParam("providerId") String providerId) {
        String errorMessage = checkCommonPreconditions(providerId);
        if (errorMessage != null) {
            return ErrorResponse.error(errorMessage, Response.Status.BAD_REQUEST);
        }
        
        FederatedIdentityModel link = session.users().getFederatedIdentity(user, providerId, realm);
        if (link == null) {
            return ErrorResponse.error(Messages.FEDERATED_IDENTITY_NOT_ACTIVE, Response.Status.BAD_REQUEST);
        }

        // Removing last social provider is not possible if you don't have other possibility to authenticate
        if (!(session.users().getFederatedIdentities(user, realm).size() > 1 || user.getFederationLink() != null || isPasswordSet())) {
            return ErrorResponse.error(Messages.FEDERATED_IDENTITY_REMOVING_LAST_PROVIDER, Response.Status.BAD_REQUEST);
        }
        
        session.users().removeFederatedIdentity(realm, user, providerId);

        logger.debugv("Social provider {0} removed successfully from user {1}", providerId, user.getUsername());

        event.event(EventType.REMOVE_FEDERATED_IDENTITY).client(auth.getClient()).user(auth.getUser())
                .detail(Details.USERNAME, auth.getUser().getUsername())
                .detail(Details.IDENTITY_PROVIDER, link.getIdentityProvider())
                .detail(Details.IDENTITY_PROVIDER_USERNAME, link.getUserName())
                .success();

        return Cors.add(request, Response.ok()).auth().allowedOrigins(auth.getToken()).build();
    }
    
    private String checkCommonPreconditions(String providerId) {
        auth.require(AccountRoles.MANAGE_ACCOUNT);
        
        if (Validation.isEmpty(providerId)) {
            return Messages.MISSING_IDENTITY_PROVIDER;
        }
        
        if (!isValidProvider(providerId)) {
            return Messages.IDENTITY_PROVIDER_NOT_FOUND;
        }
        
        if (!user.isEnabled()) {
            return Messages.ACCOUNT_DISABLED;
        }
        
        return null;
    }
    
    private boolean isPasswordSet() {
        return session.userCredentialManager().isConfiguredFor(realm, user, CredentialModel.PASSWORD);
    }
    
    private boolean isValidProvider(String providerId) {
        for (IdentityProviderModel model : realm.getIdentityProviders()) {
            if (model.getAlias().equals(providerId)) {
                return true;
            }
        }
        
        return false;
    }
}

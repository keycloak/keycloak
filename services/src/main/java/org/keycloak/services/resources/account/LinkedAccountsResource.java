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
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.common.util.Base64Url;
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
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.account.AccountLinkUriRepresentation;
import org.keycloak.representations.account.LinkedAccountRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.Auth;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.Cors;
import org.keycloak.services.validation.Validation;

import static org.keycloak.models.Constants.ACCOUNT_CONSOLE_CLIENT_ID;

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
        auth.requireOneOf(AccountRoles.MANAGE_ACCOUNT, AccountRoles.VIEW_PROFILE);
        SortedSet<LinkedAccountRepresentation> linkedAccounts = getLinkedAccounts(this.session, this.realm, this.user);
        return Cors.add(request, Response.ok(linkedAccounts)).auth().allowedOrigins(auth.getToken()).build();
    }
    
    private Set<String> findSocialIds() {
       return session.getKeycloakSessionFactory().getProviderFactoriesStream(SocialIdentityProvider.class)
               .map(ProviderFactory::getId)
               .collect(Collectors.toSet());
    }

    public SortedSet<LinkedAccountRepresentation> getLinkedAccounts(KeycloakSession session, RealmModel realm, UserModel user) {
        Set<String> socialIds = findSocialIds();
        return realm.getIdentityProvidersStream().filter(IdentityProviderModel::isEnabled)
                .map(provider -> toLinkedAccountRepresentation(provider, socialIds, session.users().getFederatedIdentitiesStream(realm, user)))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private LinkedAccountRepresentation toLinkedAccountRepresentation(IdentityProviderModel provider, Set<String> socialIds,
                                                                      Stream<FederatedIdentityModel> identities) {
        String providerId = provider.getAlias();

        FederatedIdentityModel identity = getIdentity(identities, providerId);

        String displayName = KeycloakModelUtils.getIdentityProviderDisplayName(session, provider);
        String guiOrder = provider.getConfig() != null ? provider.getConfig().get("guiOrder") : null;

        LinkedAccountRepresentation rep = new LinkedAccountRepresentation();
        rep.setConnected(identity != null);
        rep.setSocial(socialIds.contains(provider.getProviderId()));
        rep.setProviderAlias(providerId);
        rep.setDisplayName(displayName);
        rep.setGuiOrder(guiOrder);
        rep.setProviderName(provider.getAlias());
        if (identity != null) {
            rep.setLinkedUsername(identity.getUserName());
        }
        return rep;
    }

    private FederatedIdentityModel getIdentity(Stream<FederatedIdentityModel> identities, String providerId) {
        return identities.filter(model -> Objects.equals(model.getIdentityProvider(), providerId))
                .findFirst().orElse(null);
    }
    
    @GET
    @Path("/{providerId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Deprecated
    public Response buildLinkedAccountURI(@PathParam("providerId") String providerId, 
                                     @QueryParam("redirectUri") String redirectUri) {
        auth.require(AccountRoles.MANAGE_ACCOUNT);
        
        if (redirectUri == null) {
            ErrorResponse.error(Messages.INVALID_REDIRECT_URI, Response.Status.BAD_REQUEST);
        }
        
        String errorMessage = checkCommonPreconditions(providerId);
        if (errorMessage != null) {
            return ErrorResponse.error(errorMessage, Response.Status.BAD_REQUEST);
        }
        if (auth.getSession() == null) {
            return ErrorResponse.error(Messages.SESSION_NOT_ACTIVE, Response.Status.BAD_REQUEST);
        }
        
        try {
            String nonce = UUID.randomUUID().toString();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String input = nonce + auth.getSession().getId() +  ACCOUNT_CONSOLE_CLIENT_ID + providerId;
            byte[] check = md.digest(input.getBytes(StandardCharsets.UTF_8));
            String hash = Base64Url.encode(check);
            URI linkUri = Urls.identityProviderLinkRequest(this.session.getContext().getUri().getBaseUri(), providerId, realm.getName());
            linkUri = UriBuilder.fromUri(linkUri)
                    .queryParam("nonce", nonce)
                    .queryParam("hash", hash)
                    // need to use "account-console" client because IdentityBrokerService authenticates user using cookies
                    // the regular "account" client is used only for REST calls therefore cookies authentication cannot be used
                    .queryParam("client_id", ACCOUNT_CONSOLE_CLIENT_ID)
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
        auth.require(AccountRoles.MANAGE_ACCOUNT);
        
        String errorMessage = checkCommonPreconditions(providerId);
        if (errorMessage != null) {
            return ErrorResponse.error(errorMessage, Response.Status.BAD_REQUEST);
        }
        
        FederatedIdentityModel link = session.users().getFederatedIdentity(realm, user, providerId);
        if (link == null) {
            return ErrorResponse.error(Messages.FEDERATED_IDENTITY_NOT_ACTIVE, Response.Status.BAD_REQUEST);
        }

        // Removing last social provider is not possible if you don't have other possibility to authenticate
        if (!(session.users().getFederatedIdentitiesStream(realm, user).count() > 1 || user.getFederationLink() != null || isPasswordSet())) {
            return ErrorResponse.error(Messages.FEDERATED_IDENTITY_REMOVING_LAST_PROVIDER, Response.Status.BAD_REQUEST);
        }
        
        session.users().removeFederatedIdentity(realm, user, providerId);

        logger.debugv("Social provider {0} removed successfully from user {1}", providerId, user.getUsername());

        event.event(EventType.REMOVE_FEDERATED_IDENTITY).client(auth.getClient()).user(auth.getUser())
                .detail(Details.USERNAME, auth.getUser().getUsername())
                .detail(Details.IDENTITY_PROVIDER, link.getIdentityProvider())
                .detail(Details.IDENTITY_PROVIDER_USERNAME, link.getUserName())
                .success();

        return Cors.add(request, Response.noContent()).auth().allowedOrigins(auth.getToken()).build();
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
        return user.credentialManager().isConfiguredFor(PasswordCredentialModel.TYPE);
    }
    
    private boolean isValidProvider(String providerId) {
        return realm.getIdentityProvidersStream().anyMatch(model -> Objects.equals(model.getAlias(), providerId));
    }
}

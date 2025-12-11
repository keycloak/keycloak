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
package org.keycloak.broker.provider;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.util.Booleans;

import org.jboss.logging.Logger;

/**
 * @author Pedro Igor
 */
public abstract class AbstractIdentityProvider<C extends IdentityProviderModel> implements UserAuthenticationIdentityProvider<C> {

    protected static final Logger logger = Logger.getLogger(AbstractIdentityProvider.class);

    // The clientSession note flag to indicate that email or username provided by identityProvider was changed on updateProfile page
    public static final String UPDATE_PROFILE_EMAIL_CHANGED = "UPDATE_PROFILE_EMAIL_CHANGED";
    public static final String UPDATE_PROFILE_USERNAME_CHANGED = "UPDATE_PROFILE_USERNAME_CHANGED";

    // clientSession.note flag specifies if we imported new user to keycloak (true) or we just linked to an existing keycloak user (false)
    public static final String BROKER_REGISTERED_NEW_USER = "BROKER_REGISTERED_NEW_USER";

    public static final String ACCOUNT_LINK_URL = "account-link-url";
    protected final KeycloakSession session;
    private final C config;

    public AbstractIdentityProvider(KeycloakSession session, C config) {
        this.session = session;
        this.config = config;
    }

    public C getConfig() {
        return this.config;
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public Object callback(RealmModel realm, AuthenticationCallback callback, EventBuilder event) {
        return null;
    }

    @Override
    public Response performLogin(AuthenticationRequest request) {
        return null;
    }

    @Override
    public Response keycloakInitiatedBrowserLogout(KeycloakSession session, UserSessionModel userSession, UriInfo uriInfo, RealmModel realm) {
        return null;
    }

    @Override
    public void backchannelLogout(KeycloakSession session, UserSessionModel userSession, UriInfo uriInfo, RealmModel realm) {

    }

    public Response exchangeNotSupported() {
        Map<String, String> error = new HashMap<>();
        error.put("error", "invalid_target");
        error.put("error_description", "target_exchange_unsupported");
        return  Response.status(400).entity(error).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    public Response exchangeNotLinked(UriInfo uriInfo, ClientModel authorizedClient, UserSessionModel tokenUserSession, UserModel tokenSubject) {
        return exchangeErrorResponse(uriInfo, authorizedClient, tokenUserSession, "not_linked", "identity provider is not linked");
    }

    public Response exchangeNotLinkedNoStore(UriInfo uriInfo, ClientModel authorizedClient, UserSessionModel tokenUserSession, UserModel tokenSubject) {
        return exchangeErrorResponse(uriInfo, authorizedClient, tokenUserSession, "not_linked", "identity provider is not linked, can only link to current user session");
    }

    protected Response exchangeErrorResponse(UriInfo uriInfo, ClientModel authorizedClient, UserSessionModel tokenUserSession, String errorCode, String reason) {
        Map<String, String> error = new HashMap<>();
        error.put("error", errorCode);
        error.put("error_description", reason);
        String accountLinkUrl = getLinkingUrl(uriInfo, authorizedClient, tokenUserSession);
        if (accountLinkUrl != null) error.put(ACCOUNT_LINK_URL, accountLinkUrl);
        return Response.status(400).entity(error).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    protected String getLinkingUrl(UriInfo uriInfo, ClientModel authorizedClient, UserSessionModel tokenUserSession) {
        String provider = getConfig().getAlias();
        String clientId = authorizedClient.getClientId();
        String nonce = UUID.randomUUID().toString();
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        String input = nonce + tokenUserSession.getId() + clientId + provider;
        byte[] check = md.digest(input.getBytes(StandardCharsets.UTF_8));
        String hash = Base64Url.encode(check);
        return KeycloakUriBuilder.fromUri(uriInfo.getBaseUri())
                .path("/realms/{realm}/broker/{provider}/link")
                .queryParam("nonce", nonce)
                .queryParam("hash", hash)
                .queryParam("client_id", clientId)
                .build(authorizedClient.getRealm().getName(), provider)
                .toString();
    }

    public Response exchangeTokenExpired(UriInfo uriInfo, ClientModel authorizedClient, UserSessionModel tokenUserSession, UserModel tokenSubject) {
        return exchangeErrorResponse(uriInfo, authorizedClient, tokenUserSession, "token_expired", "linked token is expired");
    }

    public Response exchangeUnsupportedRequiredType() {
        Map<String, String> error = new HashMap<>();
        error.put("error", "invalid_target");
        error.put("error_description", "response_token_type_unsupported");
        return Response.status(400).entity(error).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    @Override
    public void authenticationFinished(AuthenticationSessionModel authSession, BrokeredIdentityContext context) {

    }

    @Override
    public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm, BrokeredIdentityContext context) {

    }

    @Override
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user, BrokeredIdentityContext context) {

    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, BrokeredIdentityContext context) {
        updateEmail(user, context);
    }

    protected void updateEmail(UserModel user, BrokeredIdentityContext context) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();

        // Could be the case during external-internal token exchange
        if (authSession == null) {
            return;
        }

        String email = context.getEmail();

        if (email == null) {
            // do not set email if not provided by the IdP
            return;
        }

        boolean isNewUser = Boolean.parseBoolean(authSession.getAuthNote(BROKER_REGISTERED_NEW_USER));

        if (isNewUser || IdentityProviderSyncMode.FORCE.equals(getConfig().getSyncMode())) {
            if (Boolean.parseBoolean(authSession.getAuthNote(UPDATE_PROFILE_EMAIL_CHANGED))) {
                // user updated the email and needs verification
                user.setEmailVerified(false);
                return;
            }

            setEmailVerified(user, context);
            user.setEmail(email);
        }
    }

    protected void setEmailVerified(UserModel user, BrokeredIdentityContext context) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        boolean isNewUser = Boolean.parseBoolean(authSession.getAuthNote(BROKER_REGISTERED_NEW_USER));
        String federatedEmail = context.getEmail();
        String localEmail = user.getEmail();

        if (isNewUser || federatedEmail != null && !federatedEmail.equalsIgnoreCase(localEmail)) {
            IdentityProviderModel config = context.getIdpConfig();
            boolean trustEmail = Booleans.isTrue(config.isTrustEmail());

            if (logger.isTraceEnabled()) {
                logger.tracef("Email %s verified automatically after updating user '%s' through Identity provider '%s' ", trustEmail ? "" : "not", user.getUsername(), config.getAlias());
            }

            user.setEmailVerified(trustEmail);
        }
    }

    @Override
    public IdentityProviderDataMarshaller getMarshaller() {
        return new DefaultDataMarshaller();
    }

}

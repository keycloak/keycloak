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

package org.keycloak.services.clientregistration;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.Config;
import org.keycloak.OAuthErrorException;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientInitialAccessModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.utils.AuthorizeClientUtil;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.DynamicClientRegisterContext;
import org.keycloak.services.clientpolicy.context.DynamicClientUnregisterContext;
import org.keycloak.services.clientpolicy.context.DynamicClientUpdateContext;
import org.keycloak.services.clientpolicy.context.DynamicClientViewContext;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyException;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyManager;
import org.keycloak.services.clientregistration.policy.RegistrationAuth;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.util.UserSessionUtil;
import org.keycloak.util.TokenUtil;

import static org.keycloak.models.utils.KeycloakModelUtils.removeTransientAdminRoles;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientRegistrationAuth {

    private final KeycloakSession session;
    private final ClientRegistrationProvider provider;
    private final EventBuilder event;

    private RealmModel realm;
    private AccessToken jwt;
    private ClientInitialAccessModel initialAccessModel;
    private String kid;
    private String token;
    private String endpoint;
    private boolean initialized;

    public ClientRegistrationAuth(KeycloakSession session, ClientRegistrationProvider provider, EventBuilder event, String endpoint) {
        this.session = session;
        this.provider = provider;
        this.event = event;
        this.endpoint = endpoint;
    }

    void init() {
        if (initialized) {
            return;
        }
        realm = session.getContext().getRealm();

        HttpHeaders headers = session.getContext().getRequestHeaders();
        AppAuthManager.AuthHeader authHeader = AppAuthManager.extractAuthorizationHeaderTokenOrReturnNull(headers);
        if (authHeader == null) {
            String authorization = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
            if (authorization != null && !("install".equals(endpoint) && authorization.regionMatches(true, 0, "Basic ", 0, 6))) {
                throw unauthorized("Invalid authorization header");
            }
            initialized = true;
            return;
        }
        token = authHeader.getToken();

        ClientRegistrationTokenUtils.TokenVerification tokenVerification = ClientRegistrationTokenUtils.verifyToken(session, realm, token, authHeader.getScheme());
        if (tokenVerification.getError() != null) {
            String description = tokenVerification.getError().getMessage();
            if (TokenUtil.TOKEN_TYPE_DPOP.equalsIgnoreCase(authHeader.getScheme())) {
                event.detail(Details.REASON, description).error(Errors.INVALID_TOKEN);
                throw new ErrorResponseException(Response.status(Response.Status.UNAUTHORIZED)
                        .header(HttpHeaders.WWW_AUTHENTICATE, "DPoP error=\"invalid_token\"")
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .entity(new OAuth2ErrorRepresentation(OAuthErrorException.INVALID_TOKEN, description)).build());
            }
            throw unauthorized(description);
        }
        kid = tokenVerification.getKid();
        jwt = tokenVerification.getJwt();

        if (!ClientRegistrationTokenUtils.isAccessToken(jwt) && !TokenUtil.TOKEN_TYPE_BEARER.equalsIgnoreCase(authHeader.getScheme())) {
            throw unauthorized("Invalid authorization scheme");
        }

        if (isInitialAccessToken()) {
            initialAccessModel = session.realms().getClientInitialAccessModel(session.getContext().getRealm(), jwt.getId());
            if (initialAccessModel == null) {
                throw unauthorized("Initial Access Token not found");
            }
        }
        initialized = true;
    }

    public String getToken() {
        return token;
    }

    public String getKid() {
        return kid;
    }

    public AccessToken getJwt() {
        return jwt;
    }

    boolean isAccessToken() {
        return jwt != null && ClientRegistrationTokenUtils.isAccessToken(jwt);
    }

    public boolean isInitialAccessToken() {
        return jwt != null && ClientRegistrationTokenUtils.TYPE_INITIAL_ACCESS_TOKEN.equals(jwt.getType());
    }

    public boolean isRegistrationAccessToken() {
        return jwt != null && ClientRegistrationTokenUtils.TYPE_REGISTRATION_ACCESS_TOKEN.equals(jwt.getType());
    }

    public boolean isViewOnly() {
        return isAccessToken()
                && hasRole(AdminRoles.VIEW_CLIENTS)
                && !hasRole(AdminRoles.MANAGE_CLIENTS);
    }

    public RegistrationAuth requireCreate(ClientRegistrationContext context) {
        init();

        RegistrationAuth registrationAuth = RegistrationAuth.ANONYMOUS;

        if (isAccessToken()) {

            if (hasRole(AdminRoles.MANAGE_CLIENTS, AdminRoles.CREATE_CLIENT)) {
                registrationAuth = RegistrationAuth.AUTHENTICATED;
            } else {
                throw forbidden();
            }
        } else if (isInitialAccessToken()) {
            if (initialAccessModel.getRemainingCount() > 0) {
                if (initialAccessModel.getExpiration() == 0 || (initialAccessModel.getTimestamp() + initialAccessModel.getExpiration()) > Time.currentTime()) {
                    registrationAuth = RegistrationAuth.AUTHENTICATED;
                } else {
                    throw unauthorized("Expired initial access token");
                }
            } else {
                throw unauthorized("No remaining count on initial access token");
            }
        }

        try {
            session.clientPolicy().triggerOnEvent(new DynamicClientRegisterContext(context, jwt, realm));
            ClientRegistrationPolicyManager.triggerBeforeRegister(context, registrationAuth);
        } catch (ClientRegistrationPolicyException crpe) {
            throw forbidden(crpe.getMessage());
        } catch (ClientPolicyException cpe) {
            event.detail(Details.REASON, Details.CLIENT_POLICY_ERROR);
            event.detail(Details.CLIENT_POLICY_ERROR, cpe.getError());
            event.detail(Details.CLIENT_POLICY_ERROR_DETAIL, cpe.getErrorDetail());
            event.error(cpe.getError());
            throw forbidden(cpe.getMessage());
        }

        return registrationAuth;
    }

    public void requireView(ClientModel client) {
        requireView(client, false);
    }

    public void requireView(ClientModel client, boolean allowPublicClient) {
        RegistrationAuth authType = null;
        boolean authenticated = false;

        init();

        if (isAccessToken()) {
            checkClientProtocol();

            if (hasRole(AdminRoles.MANAGE_CLIENTS, AdminRoles.VIEW_CLIENTS)) {
                if (client == null) {
                    throw notFound();
                }

                authenticated = true;
                authType = RegistrationAuth.AUTHENTICATED;
            } else {
                throw forbidden();
            }
        } else if (isRegistrationAccessToken()) {
            if (client != null && client.isEnabled() && client.getRegistrationToken() != null && client.getRegistrationToken().equals(jwt.getId())) {
                checkClientProtocol(client);
                authenticated = true;
                authType = getRegistrationAuth();
            }
        } else if (isInitialAccessToken()) {
            throw unauthorized("Not initial access token allowed");
        } else if (allowPublicClient && authenticatePublicClient(client)) {
            authenticated = true;
            authType = RegistrationAuth.AUTHENTICATED;
        }

        if (authenticated) {
            try {
                session.clientPolicy().triggerOnEvent(new DynamicClientViewContext(session, client, jwt, realm));
                ClientRegistrationPolicyManager.triggerBeforeView(session, provider, authType, client);
            } catch (ClientRegistrationPolicyException crpe) {
                throw forbidden(crpe.getMessage());
            } catch (ClientPolicyException cpe) {
                event.detail(Details.REASON, Details.CLIENT_POLICY_ERROR);
                event.detail(Details.CLIENT_POLICY_ERROR, cpe.getError());
                event.detail(Details.CLIENT_POLICY_ERROR_DETAIL, cpe.getErrorDetail());
                event.error(cpe.getError());
                throw forbidden(cpe.getMessage());
            }
        } else {
            throw unauthorized("Not authorized to view client. Not valid token or client credentials provided.");
        }
    }

    public RegistrationAuth getRegistrationAuth() {
        String str = (String) jwt.getOtherClaims().get(RegistrationAccessToken.REGISTRATION_AUTH);
        return RegistrationAuth.fromString(str);
    }

    public RegistrationAuth resolveRegistrationAuth() {
        init();
        if (jwt == null) {
            return RegistrationAuth.ANONYMOUS;
        }
        if (isRegistrationAccessToken()) {
            return getRegistrationAuth();
        }
        return RegistrationAuth.AUTHENTICATED;
    }

    public RegistrationAuth requireUpdate(ClientRegistrationContext context, ClientModel client) {
        RegistrationAuth regAuth = requireUpdateAuth(client);

        try {
            session.clientPolicy().triggerOnEvent(new DynamicClientUpdateContext(context, client, jwt, realm));
            ClientRegistrationPolicyManager.triggerBeforeUpdate(context, regAuth, client);
        } catch (ClientRegistrationPolicyException crpe) {
            throw forbidden(crpe.getMessage());
        } catch (ClientPolicyException cpe) {
            event.detail(Details.REASON, Details.CLIENT_POLICY_ERROR);
            event.detail(Details.CLIENT_POLICY_ERROR, cpe.getError());
            event.detail(Details.CLIENT_POLICY_ERROR_DETAIL, cpe.getErrorDetail());
            event.error(cpe.getError());
            throw forbidden(cpe.getMessage());
        }

        return regAuth;
    }

    public void requireDelete(ClientModel client) {
        RegistrationAuth chainType = requireUpdateAuth(client);

        try {
            session.clientPolicy().triggerOnEvent(new DynamicClientUnregisterContext(session, client, jwt, realm));
            ClientRegistrationPolicyManager.triggerBeforeRemove(session, provider, chainType, client);
        } catch (ClientRegistrationPolicyException crpe) {
            throw forbidden(crpe.getMessage());
        } catch (ClientPolicyException cpe) {
            event.detail(Details.REASON, Details.CLIENT_POLICY_ERROR);
            event.detail(Details.CLIENT_POLICY_ERROR, cpe.getError());
            event.detail(Details.CLIENT_POLICY_ERROR_DETAIL, cpe.getErrorDetail());
            event.error(cpe.getError());
            throw forbidden(cpe.getMessage());
        }
    }

    private void checkClientProtocol() {
        ClientModel client = session.getContext().getRealm().getClientByClientId(jwt.getIssuedFor());

        checkClientProtocol(client);
    }

    private void checkClientProtocol(ClientModel client) {
        if (endpoint.equals("openid-connect")) {
            if (client != null && !endpoint.contains(client.getProtocol())) {
                throw new ErrorResponseException(Errors.INVALID_CLIENT, "Wrong client protocol.", Response.Status.BAD_REQUEST);
            }
        }
    }

    private RegistrationAuth requireUpdateAuth(ClientModel client) {
        init();

        if (isAccessToken()) {
            checkClientProtocol();

            if (hasRole(AdminRoles.MANAGE_CLIENTS)) {
                if (client == null) {
                    throw notFound();
                }

                return RegistrationAuth.AUTHENTICATED;
            } else {
                throw forbidden();
            }
        } else if (isRegistrationAccessToken()) {
            if (client != null && client.isEnabled() && client.getRegistrationToken() != null && client.getRegistrationToken().equals(jwt.getId())) {
                checkClientProtocol(client);
                return getRegistrationAuth();
            }
        }

        throw unauthorized("Not authorized to update client. Maybe missing token or bad token type.");
    }

    public ClientInitialAccessModel getInitialAccessModel() {
        return initialAccessModel;
    }

    private boolean hasRole(String... roles) {
        try {
            boolean lightweight = AuthenticationManager.resolveLightweightAccessTokenRoles(session, jwt, session.getContext().getRealm());

            if (!lightweight) {
                // For lightweight access token, the roles are already considered just for those, which are present on UserModel
                if (isAccessToken()) {
                    String clientId = getMgmtClientId();
                    AccessToken.Access mgmtClientAccess = jwt.getResourceAccess(clientId);
                    if (mgmtClientAccess != null) {
                        ClientModel client = realm.getClientByClientId(jwt.getIssuedFor());
                        if (client == null) return false;
                        UserSessionModel userSession = UserSessionUtil.findValidSessionForAccessToken(session,realm, jwt, client, (invalidUserSession -> {})).getUserSession();
                        if (userSession == null) return false;

                        removeTransientAdminRoles(realm, clientId, userSession.getUser(), mgmtClientAccess);
                    }
                }
            }

            return hasRoleInToken(roles);
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean hasRoleInToken(String[] role) {
        Map<String, AccessToken.Access> resourceAccess = jwt.getResourceAccess();
        if (resourceAccess == null) {
            return false;
        }

        String clientId = getMgmtClientId();

        Set<String> roles = Optional.ofNullable(resourceAccess.get(clientId))
                .map(AccessToken.Access::getRoles)
                .orElse(Collections.emptySet());

        return Arrays.stream(role).anyMatch(roles::contains);
    }

    private String getMgmtClientId() {
        return realm.getName().equals(Config.getAdminRealm())
                ? realm.getMasterAdminClient().getClientId()
                : Constants.REALM_MANAGEMENT_CLIENT_ID;
    }

    private boolean authenticatePublicClient(ClientModel client) {
        if (client == null) {
            return false;
        }

        if (client.isPublicClient()) {
            return true;
        }

        AuthenticationProcessor processor = AuthorizeClientUtil.getAuthenticationProcessor(session, event);

        Response response = processor.authenticateClient();
        if (response != null) {
            event.client(client.getClientId()).error(Errors.NOT_ALLOWED);
            throw unauthorized("Failed to authenticate client");
        }

        ClientModel authClient = processor.getClient();
        if (authClient == null) {
            event.client(client.getClientId()).error(Errors.NOT_ALLOWED);
            throw unauthorized("No client authenticated");
        }

        if (!authClient.getClientId().equals(client.getClientId())) {
            event.client(client.getClientId()).error(Errors.NOT_ALLOWED);
            throw unauthorized("Different client authenticated");
        }

        checkClientProtocol(authClient);

        return true;
    }

    private WebApplicationException unauthorized(String errorDescription) {
        event.detail(Details.REASON, errorDescription).error(Errors.INVALID_TOKEN);
        throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, errorDescription, Response.Status.UNAUTHORIZED);
    }

    private WebApplicationException forbidden() {
        return forbidden("Forbidden");
    }

    private WebApplicationException forbidden(String errorDescription) {
        event.error(Errors.NOT_ALLOWED);
        throw new ErrorResponseException(OAuthErrorException.INSUFFICIENT_SCOPE, errorDescription, Response.Status.FORBIDDEN);
    }

    private WebApplicationException notFound() {
        event.error(Errors.CLIENT_NOT_FOUND);
        throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Client not found", Response.Status.NOT_FOUND);
    }

}

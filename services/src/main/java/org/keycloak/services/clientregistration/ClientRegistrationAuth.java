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

import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.NotFoundException;
import org.jboss.resteasy.spi.UnauthorizedException;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.common.util.Time;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientInitialAccessModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.utils.AuthorizeClientUtil;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.ForbiddenException;
import org.keycloak.util.TokenUtil;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientRegistrationAuth {

    private KeycloakSession session;
    private EventBuilder event;

    private RealmModel realm;
    private JsonWebToken jwt;
    private ClientInitialAccessModel initialAccessModel;

    public ClientRegistrationAuth(KeycloakSession session, EventBuilder event) {
        this.session = session;
        this.event = event;
    }

    private void init() {
        realm = session.getContext().getRealm();
        UriInfo uri = session.getContext().getUri();

        String authorizationHeader = session.getContext().getRequestHeaders().getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader == null) {
            return;
        }

        String[] split = authorizationHeader.split(" ");
        if (!split[0].equalsIgnoreCase("bearer")) {
            return;
        }

        jwt = ClientRegistrationTokenUtils.verifyToken(realm, uri, split[1]);
        if (jwt == null) {
            throw unauthorized();
        }

        if (isInitialAccessToken()) {
            initialAccessModel = session.sessions().getClientInitialAccessModel(session.getContext().getRealm(), jwt.getId());
            if (initialAccessModel == null) {
                throw unauthorized();
            }
        }
    }

    private boolean isBearerToken() {
        return jwt != null && TokenUtil.TOKEN_TYPE_BEARER.equals(jwt.getType());
    }

    public boolean isInitialAccessToken() {
        return jwt != null && ClientRegistrationTokenUtils.TYPE_INITIAL_ACCESS_TOKEN.equals(jwt.getType());
    }

    public boolean isRegistrationAccessToken() {
        return jwt != null && ClientRegistrationTokenUtils.TYPE_REGISTRATION_ACCESS_TOKEN.equals(jwt.getType());
    }

    public void requireCreate() {
        init();

        if (isBearerToken()) {
            if (hasRole(AdminRoles.MANAGE_CLIENTS, AdminRoles.CREATE_CLIENT)) {
                return;
            } else {
                throw forbidden();
            }
        } else if (isInitialAccessToken()) {
            if (initialAccessModel.getRemainingCount() > 0) {
                if (initialAccessModel.getExpiration() == 0 || (initialAccessModel.getTimestamp() + initialAccessModel.getExpiration()) > Time.currentTime()) {
                    return;
                }
            }
        }

        throw unauthorized();
    }

    public void requireView(ClientModel client) {
        init();

        if (isBearerToken()) {
            if (hasRole(AdminRoles.MANAGE_CLIENTS, AdminRoles.VIEW_CLIENTS)) {
                if (client == null) {
                    throw notFound();
                }
                return;
            } else {
                throw forbidden();
            }
        } else if (isRegistrationAccessToken()) {
            if (client.getRegistrationToken() != null && client != null && client.getRegistrationToken().equals(jwt.getId())) {
                return;
            }
        } else if (isInitialAccessToken()) {
            throw unauthorized();
        } else {
            if (authenticateClient(client)) {
                return;
            }
        }

        throw unauthorized();
    }

    public void requireUpdate(ClientModel client) {
        init();

        if (isBearerToken()) {
            if (hasRole(AdminRoles.MANAGE_CLIENTS)) {
                if (client == null) {
                    throw notFound();
                }
                return;
            } else {
                throw forbidden();
            }
        } else if (isRegistrationAccessToken()) {
            if (client.getRegistrationToken() != null && client != null && client.getRegistrationToken().equals(jwt.getId())) {
                return;
            }
        }

        throw unauthorized();
    }

    public ClientInitialAccessModel getInitialAccessModel() {
        return initialAccessModel;
    }

    private boolean hasRole(String... role) {
        try {
            Map<String, Object> otherClaims = jwt.getOtherClaims();
            if (otherClaims != null) {
                Map<String, Map<String, List<String>>> resourceAccess = (Map<String, Map<String, List<String>>>) jwt.getOtherClaims().get("resource_access");
                if (resourceAccess == null) {
                    return false;
                }

                List<String> roles = null;

                Map<String, List<String>> map;
                if (realm.getName().equals(Config.getAdminRealm())) {
                    map = resourceAccess.get(realm.getMasterAdminClient().getClientId());
                } else {
                    map = resourceAccess.get(Constants.REALM_MANAGEMENT_CLIENT_ID);
                }

                if (map != null) {
                    roles = map.get("roles");
                }

                if (roles == null) {
                    return false;
                }

                for (String r : role) {
                    if (roles.contains(r)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean authenticateClient(ClientModel client) {
        if (client.isPublicClient()) {
            return true;
        }

        AuthenticationProcessor processor = AuthorizeClientUtil.getAuthenticationProcessor(session, event);

        Response response = processor.authenticateClient();
        if (response != null) {
            event.client(client.getClientId()).error(Errors.NOT_ALLOWED);
            throw unauthorized();
        }

        ClientModel authClient = processor.getClient();
        if (client == null) {
            event.client(client.getClientId()).error(Errors.NOT_ALLOWED);
            throw unauthorized();
        }

        if (!authClient.getClientId().equals(client.getClientId())) {
            event.client(client.getClientId()).error(Errors.NOT_ALLOWED);
            throw unauthorized();
        }

        return true;
    }

    private Failure unauthorized() {
        event.error(Errors.NOT_ALLOWED);
        return new UnauthorizedException();
    }

    private Failure forbidden() {
        event.error(Errors.NOT_ALLOWED);
        return new ForbiddenException();
    }

    private Failure notFound() {
        event.error(Errors.NOT_ALLOWED);
        return new NotFoundException("Client not found");
    }

}

package org.keycloak.services.clientregistration;

import org.jboss.resteasy.spi.UnauthorizedException;
import org.keycloak.common.util.Time;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.*;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.ForbiddenException;
import org.keycloak.util.TokenUtil;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientRegistrationAuth {

    private KeycloakSession session;
    private EventBuilder event;

    private JsonWebToken jwt;
    private ClientInitialAccessModel initialAccessModel;

    public ClientRegistrationAuth(KeycloakSession session, EventBuilder event) {
        this.session = session;
        this.event = event;

        init();
    }

    private void init() {
        RealmModel realm = session.getContext().getRealm();
        UriInfo uri = session.getContext().getUri();

        String authorizationHeader = session.getContext().getRequestHeaders().getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader == null) {
            return;
        }

        String[] split = authorizationHeader.split(" ");
        if (!split[0].equalsIgnoreCase("bearer")) {
            return;
        }

        jwt = ClientRegistrationTokenUtils.parseToken(realm, uri, split[1]);

        if (isInitialAccessToken()) {
            initialAccessModel = session.sessions().getClientInitialAccessModel(session.getContext().getRealm(), jwt.getId());
            if (initialAccessModel == null) {
                throw new ForbiddenException();
            }
        }
    }

    public boolean isAuthenticated() {
        return jwt != null;
    }

    public boolean isBearerToken() {
        return TokenUtil.TOKEN_TYPE_BEARER.equals(jwt.getType());
    }

    public boolean isInitialAccessToken() {
        return ClientRegistrationTokenUtils.TYPE_INITIAL_ACCESS_TOKEN.equals(jwt.getType());
    }

    public boolean isRegistrationAccessToken() {
        return ClientRegistrationTokenUtils.TYPE_REGISTRATION_ACCESS_TOKEN.equals(jwt.getType());
    }

    public void requireCreate() {
        if (!isAuthenticated()) {
            event.error(Errors.NOT_ALLOWED);
            throw new UnauthorizedException();
        }

        if (isBearerToken()) {
            if (hasRole(AdminRoles.MANAGE_CLIENTS, AdminRoles.CREATE_CLIENT)) {
                return;
            }
        } else if (isInitialAccessToken()) {
            if (initialAccessModel.getRemainingCount() > 0) {
                if (initialAccessModel.getExpiration() == 0 || (initialAccessModel.getTimestamp() + initialAccessModel.getExpiration()) > Time.currentTime()) {
                    return;
                }
            }
        }

        event.error(Errors.NOT_ALLOWED);
        throw new ForbiddenException();
    }

    public void requireView(ClientModel client) {
        if (!isAuthenticated()) {
            event.error(Errors.NOT_ALLOWED);
            throw new UnauthorizedException();
        }

        if (client == null) {
            event.error(Errors.NOT_ALLOWED);
            throw new ForbiddenException();
        }

        if (isBearerToken()) {
            if (hasRole(AdminRoles.MANAGE_CLIENTS, AdminRoles.VIEW_CLIENTS)) {
                return;
            }
        } else if (isRegistrationAccessToken()) {
            if (client.getRegistrationToken() != null && client.getRegistrationToken().equals(jwt.getId())) {
                return;
            }
        }

        event.error(Errors.NOT_ALLOWED);
        throw new ForbiddenException();
    }

    public void requireUpdate(ClientModel client) {
        if (!isAuthenticated()) {
            event.error(Errors.NOT_ALLOWED);
            throw new UnauthorizedException();
        }

        if (client == null) {
            event.error(Errors.NOT_ALLOWED);
            throw new ForbiddenException();
        }

        if (isBearerToken()) {
            if (hasRole(AdminRoles.MANAGE_CLIENTS)) {
                return;
            }
        } else if (isRegistrationAccessToken()) {
            if (client.getRegistrationToken() != null && client.getRegistrationToken().equals(jwt.getId())) {
                return;
            }
        }

        event.error(Errors.NOT_ALLOWED);
        throw new ForbiddenException();
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

                Map<String, List<String>> realmManagement = resourceAccess.get(Constants.REALM_MANAGEMENT_CLIENT_ID);
                if (realmManagement == null) {
                    return false;
                }

                List<String> resources = realmManagement.get("roles");
                if (resources == null) {
                    return false;
                }

                for (String r : role) {
                    if (resources.contains(r)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

}

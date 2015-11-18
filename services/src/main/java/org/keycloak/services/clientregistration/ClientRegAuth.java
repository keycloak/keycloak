package org.keycloak.services.clientregistration;

import org.jboss.resteasy.spi.UnauthorizedException;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.*;
import org.keycloak.protocol.oidc.utils.AuthorizeClientUtil;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;

import javax.ws.rs.core.HttpHeaders;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientRegAuth {

    private KeycloakSession session;
    private EventBuilder event;

    private String token;
    private AccessToken.Access bearerRealmAccess;

    private boolean authenticated = false;
    private boolean registrationAccessToken = false;

    public ClientRegAuth(KeycloakSession session, EventBuilder event) {
        this.session = session;
        this.event = event;

        init();
    }

    private void init() {
        RealmModel realm = session.getContext().getRealm();

        String authorizationHeader = session.getContext().getRequestHeaders().getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader == null) {
            return;
        }

        String[] split = authorizationHeader.split(" ");
        if (!split[0].equalsIgnoreCase("bearer")) {
            return;
        }

        if (split[1].indexOf('.') == -1) {
            token = split[1];
            authenticated = true;
            registrationAccessToken = true;
        } else {
            AuthenticationManager.AuthResult authResult = new AppAuthManager().authenticateBearerToken(session, realm);
            bearerRealmAccess = authResult.getToken().getResourceAccess(Constants.REALM_MANAGEMENT_CLIENT_ID);
            authenticated = true;
        }
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public boolean isRegistrationAccessToken() {
        return registrationAccessToken;
    }

    public void requireCreate() {
        if (!authenticated) {
            event.error(Errors.NOT_ALLOWED);
            throw new UnauthorizedException();
        }

        if (bearerRealmAccess != null) {
            if (bearerRealmAccess.isUserInRole(AdminRoles.MANAGE_CLIENTS) || bearerRealmAccess.isUserInRole(AdminRoles.CREATE_CLIENT)) {
                return;
            }
        }

        event.error(Errors.NOT_ALLOWED);
        throw new ForbiddenException();
    }

    public void requireView(ClientModel client) {
        if (!authenticated) {
            event.error(Errors.NOT_ALLOWED);
            throw new UnauthorizedException();
        }

        if (client == null) {
            event.error(Errors.NOT_ALLOWED);
            throw new ForbiddenException();
        }

        if (bearerRealmAccess != null) {
            if (bearerRealmAccess.isUserInRole(AdminRoles.MANAGE_CLIENTS) || bearerRealmAccess.isUserInRole(AdminRoles.VIEW_CLIENTS)) {
                return;
            }
        } else if (token != null) {
            if (client.getRegistrationSecret() != null && client.getRegistrationSecret().equals(token)) {
                return;
            }
        }

        event.error(Errors.NOT_ALLOWED);
        throw new ForbiddenException();
    }

    public void requireUpdate(ClientModel client) {
        if (!authenticated) {
            event.error(Errors.NOT_ALLOWED);
            throw new UnauthorizedException();
        }

        if (client == null) {
            event.error(Errors.NOT_ALLOWED);
            throw new ForbiddenException();
        }

        if (bearerRealmAccess != null) {
            if (bearerRealmAccess.isUserInRole(AdminRoles.MANAGE_CLIENTS) || bearerRealmAccess.isUserInRole(AdminRoles.VIEW_CLIENTS)) {
                return;
            }
        } else if (token != null) {
            if (client.getRegistrationSecret() != null && client.getRegistrationSecret().equals(token)) {
                return;
            }
        }

        event.error(Errors.NOT_ALLOWED);
        throw new ForbiddenException();
    }

}

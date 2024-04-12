package org.keycloak.admin.ui.rest;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.light.LightweightUserAdapter;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import jakarta.ws.rs.ForbiddenException;

public class UsersResource {
    private final KeycloakSession session;

    private final AdminPermissionEvaluator auth;

    public UsersResource(KeycloakSession session, AdminPermissionEvaluator auth) {
        this.session = session;
        this.auth = auth;
    }

    @Path("{id}")
    public UserResource getUser(@PathParam("id") String id) {
        RealmModel realm = session.getContext().getRealm();
        UserModel user = null;
        if (LightweightUserAdapter.isLightweightUser(id)) {
            UserSessionModel userSession = session.sessions().getUserSession(realm, LightweightUserAdapter.getLightweightUserId(id));
            if (userSession != null) {
                user = userSession.getUser();
            }
        } else {
            user = session.users().getUserById(realm, id);
        }

        if (user == null) {
            // we do this to make sure somebody can't phish ids
            if (auth.users().canQuery()) throw new NotFoundException("User not found");
            else throw new ForbiddenException();
        }

        return new UserResource(session, user);
    }
}

package org.keycloak.admin.ui.rest;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

public class UsersResource {
    private final KeycloakSession session;

    public UsersResource(KeycloakSession session) {
        this.session = session;
    }

    @Path("{id}")
    public UserResource getUser(@PathParam("id") String id) {
        RealmModel realm = session.getContext().getRealm();
        UserModel user = session.users().getUserById(realm, id);

        if (user == null) {
            throw new NotFoundException();
        }

        return new UserResource(session, user);
    }
}

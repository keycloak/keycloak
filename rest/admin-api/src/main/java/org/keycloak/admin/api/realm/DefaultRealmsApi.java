package org.keycloak.admin.api.realm;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import org.keycloak.admin.api.ChosenBySpi;
import org.keycloak.models.KeycloakSession;

import java.util.Optional;

@RequestScoped
@ChosenBySpi
public class DefaultRealmsApi implements RealmsApi {

    @Inject
    RealmApi realmApi;

    @Context
    KeycloakSession session;

    @Path("{name}")
    @Override
    public RealmApi realm(@PathParam("name") String name) {
        var realm = Optional.ofNullable(session.realms().getRealmByName(name))
                .orElseThrow(() -> new NotFoundException("Realm cannot be found"));
        session.getContext().setRealm(realm);
        return realmApi;
    }

    @Override
    public void close() {

    }
}

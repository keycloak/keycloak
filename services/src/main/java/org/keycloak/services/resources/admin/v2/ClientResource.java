package org.keycloak.services.resources.admin.v2;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.NoCache;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.admin.v2.ClientRepresentation;

import static java.lang.String.format;

public class ClientResource {
    public final KeycloakSession session;
    public final RealmModel realm;
    public final String clientId;

    public ClientResource(KeycloakSession session, RealmModel realm, String clientId) {
        this.session = session;
        this.realm = realm;
        this.clientId = clientId;
    }

    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public ClientRepresentation getClient() {
        return session.getServices()
                .clients()
                .getClient(realm, clientId)
                .orElseThrow(() -> new NotFoundException(format("Cannot find client with client ID: '%s'", clientId)));
    }
}

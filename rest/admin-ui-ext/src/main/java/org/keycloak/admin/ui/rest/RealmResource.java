package org.keycloak.admin.ui.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.ForbiddenException;

import java.util.Objects;
import java.util.stream.Stream;

import static org.keycloak.utils.StreamsUtil.throwIfEmpty;

public class RealmResource {
    private final KeycloakSession session;

    public RealmResource(KeycloakSession session) {
        this.session = session;
    }

    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Lists only the names of the realms",
            description = "Returns a list of realm names based on what the caller is allowed to view"
    )
    @APIResponse(
            responseCode = "200",
            description = "",
            content = {@Content(
                    schema = @Schema(
                            implementation = String.class,
                            type = SchemaType.ARRAY
                    )
            )}
    )
    public Stream<String> realmList() {
        Stream<String> realms = session.realms().getRealmsStream().filter(Objects::nonNull).map(RealmModel::getName);
        return throwIfEmpty(realms, new ForbiddenException());
    }
}

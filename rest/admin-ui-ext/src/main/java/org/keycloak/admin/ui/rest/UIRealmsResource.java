package org.keycloak.admin.ui.rest;

import static org.keycloak.utils.StreamsUtil.throwIfEmpty;

import java.util.Objects;
import java.util.stream.Stream;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.reactive.NoCache;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.RealmsPermissionEvaluator;

public class UIRealmsResource {

    private final KeycloakSession session;
    private final AdminPermissionEvaluator auth;

    public UIRealmsResource(KeycloakSession session, AdminPermissionEvaluator auth) {
        this.session = session;
        this.auth = auth;
    }

    @GET
    @Path("names")
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
    public Stream<String> getRealmNames() {
        Stream<String> realms = session.realms().getRealmsStream()
                                .filter(realm -> {
                                    RealmsPermissionEvaluator eval = AdminPermissions.realms(session, auth.adminAuth());
                                    return eval.canView(realm) || eval.isAdmin(realm);
                                  })
                                .map(RealmModel::getName);
        return throwIfEmpty(realms, new ForbiddenException());
    }
}

package org.keycloak.admin.api;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;



@Path("admin/api")
public interface AdminRootV2 {

    @Path("{realmName}")
    AdminApi adminApi(@PathParam("realmName") String realmName);

//    // TODO Fix preflights
//    @Path("{realmName}/{any:.*}")
//    @OPTIONS
//    @Operation(hidden = true)
//    public Response preFlight() {
//        checkApiEnabled();
//        return new AdminCorsPreflightService().preflight();
//    }
//
//    private void checkApiEnabled() {
//        if (!isAdminApiV2Enabled()) {
//            throw new NotFoundException();
//        }
//    }
//
//    public static boolean isAdminApiV2Enabled() {
//        return Profile.isFeatureEnabled(Profile.Feature.CLIENT_ADMIN_API_V2); // There's currently only Client API for the new Admin API v2
//    }
}

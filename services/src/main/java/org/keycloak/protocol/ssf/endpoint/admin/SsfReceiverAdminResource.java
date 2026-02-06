package org.keycloak.protocol.ssf.endpoint.admin;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * SsfReceiverAdminResource provides access to SSF Receiver operations. SSS
 */
public class SsfReceiverAdminResource {

    protected final KeycloakSession session;
    protected final AdminPermissionEvaluator auth;

    public SsfReceiverAdminResource(KeycloakSession session, AdminPermissionEvaluator auth) {
        this.session = session;
        this.auth = auth;
    }

    /**
     * Exposes the {@link SsfVerificationResource} to verify the stream and event delivery setup for a SSF Receiver as a custom endpoint.
     * <p>
     * The endpoint is available via {@code $KC_ADMIN_URL/admin/realms/{realm}/ssf/receivers/{receiverAlias}/verify}
     *
     * @param alias
     * @return
     */
    @Tag(name = KeycloakOpenAPI.Admin.Tags.SSF_STREAM_VERIFICATION)
    @Operation(summary = "Trigger SSF Stream Verification for the given receiver in this realm.")
    @APIResponses(value = {
            @APIResponse(responseCode = "202", description = "Accepted"),
            @APIResponse(responseCode = "400", description = "Bad Request"),
    })
    @Path("/{receiverAlias}/verify")
    public SsfVerificationResource verificationEndpoint(@PathParam("receiverAlias") String alias) {
        return new SsfVerificationResource(session, alias);
    }
}

package org.keycloak.ssf.receiver.resources;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.ssf.receiver.SsfReceiver;
import org.keycloak.ssf.receiver.registration.SsfReceiverRegistrationProviderFactory;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

/**
 * SsfReceiverAdminResource provides access to SSF Receiver operations.
 */
public class SsfReceiverAdminResource {

    protected static final Logger LOG = Logger.getLogger(SsfReceiverAdminResource.class);

    protected final KeycloakSession session;

    protected final AdminPermissionEvaluator auth;

    public SsfReceiverAdminResource(KeycloakSession session, AdminPermissionEvaluator auth) {
        this.session = session;
        this.auth = auth;
    }

    /**
     * This calls the verification_endpoint provided by the associated SSF Transmitter.
     * <p>
     * Note that the verification_endpoint is called with the current stream_id and the transmitter access token.
     *
     * <p>
     * The endpoint is available via {@code $KC_ADMIN_URL/admin/realms/{realm}/ssf/receivers/{receiverAlias}/verify}
     *
     * @param alias
     * @return
     */
    @Tag(name = KeycloakOpenAPI.Admin.Tags.SSF_STREAM_VERIFICATION)
    @Operation(summary = "Trigger SSF Stream Verification for the given receiver in this realm.")
    @APIResponses(value = {
            @APIResponse(responseCode = "204", description = "No Content"),
            @APIResponse(responseCode = "400", description = "Bad Request"),
            @APIResponse(responseCode = "404", description = "Not Found"),
    })
    @POST
    @Path("/{receiverAlias}/verify")
    public Response verificationEndpoint(@PathParam("receiverAlias") String receiverAlias) {

        RealmModel realm = session.getContext().getRealm();
        SsfReceiver receiver = SsfReceiverRegistrationProviderFactory.getSsfReceiver(session, realm, receiverAlias);
        if (receiver == null) {
            return Response.status(Response.Status.NOT_FOUND).type(MediaType.APPLICATION_JSON_TYPE).build();
        }

        // TODO handle pending verifications

        try {
            receiver.requestVerification();
        } catch (Exception e) {
            LOG.warn("Failed to trigger stream verification for receiver: " + receiverAlias, e);
            throw SsfSetPushDeliveryFailureResponse.newFailureResponse(Response.Status.INTERNAL_SERVER_ERROR, SsfSetPushDeliveryFailureResponse.ERROR_INTERNAL_ERROR, "Stream verification failed");
        }

        return Response.noContent().type(MediaType.APPLICATION_JSON).build();
    }
}

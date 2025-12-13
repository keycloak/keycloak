package org.keycloak.protocol.ssf.endpoint.admin;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ssf.endpoint.SsfSetPushDeliveryFailureResponse;
import org.keycloak.protocol.ssf.receiver.SsfReceiver;
import org.keycloak.protocol.ssf.receiver.registration.SsfReceiverRegistrationProviderFactory;

/**
 * SsfVerificationResource is used to verify the stream and event delivery setup for a SSF Receiver
 */
public class SsfVerificationResource {

    protected final KeycloakSession session;

    protected final String receiverAlias;

    public SsfVerificationResource(KeycloakSession session, String receiverAlias) {
        this.session = session;
        this.receiverAlias = receiverAlias;
    }

    /**
     * This calls the verification_endpoint provided by the associated SSF Transmitter.
     * <p>
     * Note that the verification_endpoint is called with the current stream_id and the transmitter access token.
     *
     * @return
     */
    @POST
    public Response triggerVerification() {

        RealmModel realm = session.getContext().getRealm();
        SsfReceiver receiver = SsfReceiverRegistrationProviderFactory.getSsfReceiver(session, realm, receiverAlias);
        if (receiver == null) {
            return Response.status(Response.Status.NOT_FOUND).type(MediaType.APPLICATION_JSON_TYPE).build();
        }

        // TODO handle pending verifications

        try {
            receiver.requestVerification();
        } catch (Exception e) {
            throw SsfSetPushDeliveryFailureResponse.newFailureResponse(Response.Status.INTERNAL_SERVER_ERROR, SsfSetPushDeliveryFailureResponse.ERROR_INTERNAL_ERROR, e.getMessage());
        }

        return Response.noContent().type(MediaType.APPLICATION_JSON).build();
    }
}

package org.keycloak.protocol.ssf.endpoint.admin;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ssf.endpoint.SsfSetPushDeliveryFailureResponse;
import org.keycloak.protocol.ssf.receiver.SsfReceiverProviderFactory;
import org.keycloak.protocol.ssf.receiver.SsfReceiver;

import static org.keycloak.protocol.ssf.endpoint.SsfSetPushDeliveryResponseUtil.newSsfSetPushDeliveryFailureResponse;

public class SsfVerificationResource {

    protected static final Logger log = Logger.getLogger(SsfVerificationResource.class);

    protected final KeycloakSession session;

    protected final String receiverAlias;

    public SsfVerificationResource(KeycloakSession session, String receiverAlias) {
        this.session = session;
        this.receiverAlias = receiverAlias;
    }

    @POST
    public Response triggerVerification() {

        RealmModel realm = session.getContext().getRealm();
        SsfReceiver receiver = SsfReceiverProviderFactory.getSsfReceiver(session, realm, receiverAlias);
        if (receiver == null) {
            return Response.status(Response.Status.NOT_FOUND).type(MediaType.APPLICATION_JSON_TYPE).build();
        }
        // TODO reject pending verification

        try {
            receiver.requestVerification();
        } catch (Exception e) {
            throw newSsfSetPushDeliveryFailureResponse(Response.Status.INTERNAL_SERVER_ERROR, SsfSetPushDeliveryFailureResponse.ERROR_INTERNAL_ERROR, e.getMessage());
        }

        return Response.noContent().type(MediaType.APPLICATION_JSON).build();

    }
}

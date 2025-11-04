package org.keycloak.protocol.ssf.receiver.management;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.ssf.receiver.SsfReceiverModel;
import org.keycloak.protocol.ssf.receiver.spi.SsfReceiver;
import org.keycloak.protocol.ssf.support.SsfSetPushDeliveryFailureResponse;

import static org.keycloak.protocol.ssf.support.SsfSetPushDeliveryResponseUtil.newSsfSetPushDeliveryFailureResponse;

public class SsfVerificationEndpoint {

    protected static final Logger log = Logger.getLogger(SsfVerificationEndpoint.class);

    protected final KeycloakSession session;

    protected final SsfReceiverManager receiverManager;

    protected final String receiverAlias;

    public SsfVerificationEndpoint(KeycloakSession session, SsfReceiverManager receiverManager, String receiverAlias) {
        this.session = session;
        this.receiverManager = receiverManager;
        this.receiverAlias = receiverAlias;
    }

    @POST
    public Response triggerVerification() {

        KeycloakContext context = session.getContext();
        SsfReceiverModel receiverModel = receiverManager.getReceiverModel(context, receiverAlias);
        if (receiverModel == null) {
            return Response.status(Response.Status.NOT_FOUND).type(MediaType.APPLICATION_JSON_TYPE).build();
        }

        SsfReceiver receiver = receiverManager.loadReceiverFromModel(receiverModel);

        // TODO reject pending verification

        try {
            receiver.requestVerification();
        } catch (Exception e) {
            throw newSsfSetPushDeliveryFailureResponse(Response.Status.INTERNAL_SERVER_ERROR, SsfSetPushDeliveryFailureResponse.ERROR_INTERNAL_ERROR, e.getMessage());
        }

        return Response.noContent().type(MediaType.APPLICATION_JSON).build();

    }
}

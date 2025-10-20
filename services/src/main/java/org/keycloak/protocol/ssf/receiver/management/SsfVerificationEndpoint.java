package org.keycloak.protocol.ssf.receiver.management;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.ssf.receiver.ReceiverModel;
import org.keycloak.protocol.ssf.receiver.SsfReceiver;
import org.keycloak.protocol.ssf.support.SsfFailureResponse;

import static org.keycloak.protocol.ssf.support.SsfResponseUtil.newSharedSignalFailureResponse;

public class SsfVerificationEndpoint {

    protected static final Logger log = Logger.getLogger(SsfVerificationEndpoint.class);

    protected final KeycloakSession session;

    protected final ReceiverManager receiverManager;

    protected final String receiverAlias;

    public SsfVerificationEndpoint(KeycloakSession session, ReceiverManager receiverManager, String receiverAlias) {
        this.session = session;
        this.receiverManager = receiverManager;
        this.receiverAlias = receiverAlias;
    }

    @POST
    public Response triggerVerification() {

        KeycloakContext context = session.getContext();
        ReceiverModel receiverModel = receiverManager.getReceiverModel(context, receiverAlias);
        if (receiverModel == null) {
            return Response.status(Response.Status.NOT_FOUND).type(MediaType.APPLICATION_JSON_TYPE).build();
        }

        SsfReceiver receiver = receiverManager.lookupReceiver(receiverModel);

        // TODO reject pending verification

        try {
            receiver.requestVerification();
        } catch (Exception e) {
            throw newSharedSignalFailureResponse(Response.Status.INTERNAL_SERVER_ERROR, SsfFailureResponse.ERROR_INTERNAL_ERROR, e.getMessage());
        }

        return Response.noContent().type(MediaType.APPLICATION_JSON).build();

    }
}

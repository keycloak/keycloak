package org.keycloak.protocol.ssf.receiver.management;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.ssf.receiver.SsfReceiverConfig;
import org.keycloak.protocol.ssf.receiver.SsfReceiverModel;

import java.util.List;
import java.util.Map;

import static org.keycloak.protocol.ssf.support.SsfSetPushDeliveryResponseUtil.newSsfSetPushDeliveryFailureResponse;

public class SsfReceiverManagementEndpoint {

    protected static final Logger log = Logger.getLogger(SsfReceiverManagementEndpoint.class);

    private final KeycloakSession session;

    private final SsfReceiverManager receiverManager;

    public SsfReceiverManagementEndpoint(KeycloakSession session, SsfReceiverManager receiverManager) {
        this.session = session;
        this.receiverManager = receiverManager;
    }

    /**
     * @param alias
     * @param config
     * @return
     */
    @PUT
    @Path("/receivers/{receiverAlias}")
    public Response updateReceiverConfig(@PathParam("receiverAlias") String alias, SsfReceiverConfig config) {

        SsfReceiverModel receiverModel;
        try {
            receiverModel = receiverManager.createOrUpdateReceiver(session.getContext(), alias, config);
        } catch (SsfStreamException sse) {
            throw newSsfSetPushDeliveryFailureResponse(sse.getStatus(), sse.getStatus().getReasonPhrase(), "Could not update receiver config: " + sse.getMessage());
        } catch (Exception e) {
            throw newSsfSetPushDeliveryFailureResponse(Response.Status.INTERNAL_SERVER_ERROR, Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase(), "Could not update receiver config: " + e.getMessage());
        }

        return Response.ok().type(MediaType.APPLICATION_JSON_TYPE).entity(modelToRep(receiverModel)).build();
    }

    @POST
    @Path("/receivers/{receiverAlias}/refresh")
    public Response refreshReceiver(@PathParam("receiverAlias") String alias) {

        KeycloakContext context = session.getContext();
        SsfReceiverModel receiverModel = receiverManager.getReceiverModel(context, alias);
        if (receiverModel == null) {
            return Response.status(Response.Status.NOT_FOUND).type(MediaType.APPLICATION_JSON_TYPE).build();
        }

        receiverManager.refreshReceiver(context, receiverModel);

        return Response.ok().type(MediaType.APPLICATION_JSON_TYPE).entity(Map.of("status", "refreshed")).build();
    }

    @Path("/receivers/{receiverAlias}/verify")
    public SsfVerificationEndpoint verificationEndpoint(@PathParam("receiverAlias") String alias) {
        return new SsfVerificationEndpoint(session, receiverManager, alias);
    }

    @DELETE
    @Path("/receivers/{receiverAlias}")
    public Response deleteReceiverConfig(@PathParam("receiverAlias") String alias) {

        KeycloakContext context = session.getContext();
        SsfReceiverModel receiverModel = receiverManager.getReceiverModel(context, alias);
        if (receiverModel == null) {
            return Response.status(Response.Status.NOT_FOUND).type(MediaType.APPLICATION_JSON_TYPE).build();
        }

        receiverManager.removeReceiver(context, receiverModel);

        return Response.noContent().type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    @GET
    @Path("/receivers")
    public Response listReceivers() {

        List<SsfReceiverModel> receiverModels = receiverManager.listReceivers(session.getContext());
        List<SsfReceiverRepresentation> reps = receiverModels.stream().map(this::modelToRep).toList();
        return Response.ok().entity(reps).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    @GET
    @Path("/receivers/{receiverAlias}")
    public Response getReceiver(@PathParam("receiverAlias") String alias) {

        SsfReceiverModel receiverModel = receiverManager.getReceiverModel(session.getContext(), alias);
        if (receiverModel == null) {
            return Response.status(Response.Status.NOT_FOUND).type(MediaType.APPLICATION_JSON_TYPE).build();
        }

        return Response.ok().entity(modelToRep(receiverModel)).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    protected SsfReceiverRepresentation modelToRep(SsfReceiverModel model) {
        SsfReceiverRepresentation rep = new SsfReceiverRepresentation();

        rep.setComponentId(model.getId());
        rep.setAlias(model.getAlias());
        rep.setDescription(model.getDescription());
        rep.setAudience(model.getAudience());
        rep.setManagedStream(model.getManagedStream());
        rep.setEventsDelivered(model.getEventsDelivered());
        rep.setPollIntervalSeconds(model.getPollIntervalSeconds());
        rep.setPushAuthorizationToken(model.getPushAuthorizationHeader());
        rep.setTransmitterUrl(model.getTransmitterUrl());
        rep.setTransmitterPollUrl(model.getTransmitterPollUrl());
        rep.setReceiverPushUrl(model.getReceiverPushUrl());
        rep.setDeliveryMethod(model.getDeliveryMethod().name());
        rep.setStreamId(model.getStreamId());
        rep.setModifiedAt(model.getModifiedAt());
        rep.setConfigHash(model.getConfigHash());
        rep.setMaxEvents(model.getMaxEvents());
        rep.setAcknowledgeImmediately(model.isAcknowledgeImmediately());
        return rep;
    }
}

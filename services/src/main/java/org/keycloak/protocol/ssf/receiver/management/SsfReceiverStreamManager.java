package org.keycloak.protocol.ssf.receiver.management;

import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakContext;
import org.keycloak.protocol.ssf.receiver.SsfReceiverModel;
import org.keycloak.protocol.ssf.receiver.streamclient.SsfStreamClient;
import org.keycloak.protocol.ssf.receiver.transmitterclient.SsfTransmitterClient;
import org.keycloak.protocol.ssf.spi.SsfProvider;
import org.keycloak.protocol.ssf.stream.CreateStreamRequest;
import org.keycloak.protocol.ssf.stream.PollSetDeliveryMethodRepresentation;
import org.keycloak.protocol.ssf.stream.PushDeliveryMethodRepresentation;
import org.keycloak.protocol.ssf.stream.SsfStreamRepresentation;
import org.keycloak.protocol.ssf.transmitter.SsfTransmitterMetadata;
import org.keycloak.services.Urls;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.net.URI;

public class SsfReceiverStreamManager {

    protected static final Logger log = Logger.getLogger(SsfReceiverManager.class);

    protected final SsfStreamClient streamClient;

    protected final SsfTransmitterClient ssfTransmitterClient;

    public SsfReceiverStreamManager(SsfProvider ssfProvider) {
        this.streamClient = ssfProvider.streamClient();
        this.ssfTransmitterClient = ssfProvider.transmitterClient();
    }

    public SsfStreamRepresentation createReceiverStream(KeycloakContext context, SsfReceiverModel model) {

        SsfTransmitterMetadata transmitterMetadata = ssfTransmitterClient.loadTransmitterMetadata(model);
        CreateStreamRequest createStreamRequest = createCreateStreamRequest(context, model);
        SsfStreamRepresentation streamRep = streamClient.createStream(transmitterMetadata, model.getTransmitterAccessToken(), createStreamRequest);

        try {
            log.infof("Created stream rep: %s", JsonSerialization.writeValueAsPrettyString(streamRep));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // update streamId
        model.setStreamId(streamRep.getId());
        context.getRealm().updateComponent(model);

        return streamRep;
    }

    protected CreateStreamRequest createCreateStreamRequest(KeycloakContext context, SsfReceiverModel model) {

        CreateStreamRequest createStreamRequest = new CreateStreamRequest();
        createStreamRequest.setDescription(model.getDescription());
        createStreamRequest.setEventsRequested(model.getEventsRequested());
        switch (model.getDeliveryMethod()) {
            case POLL -> {
                // endpoint URL determined by transmitter
                var delivery = new PollSetDeliveryMethodRepresentation(null);
                createStreamRequest.setDelivery(delivery);
            }
            case PUSH -> {
                String pushUrl = createPushUrl(context, model);
                try {
                    URI.create(pushUrl);
                } catch (IllegalArgumentException use) {
                    throw new SsfStreamException("Invalid push url: " + pushUrl, use, Response.Status.BAD_REQUEST);
                }
                var delivery = new PushDeliveryMethodRepresentation(pushUrl, model.getPushAuthorizationHeader());
                createStreamRequest.setDelivery(delivery);
            }
        }

        return createStreamRequest;
    }

    public String createPushUrl(KeycloakContext context, SsfReceiverModel model) {

        String issuer = Urls.realmIssuer(context.getUri().getBaseUri(), context.getRealm().getName());
        String pushUrl = issuer + "/ssf/push/" + model.getAlias();
        return pushUrl;
    }

    public void deleteReceiverStream(SsfReceiverModel model) {

        SsfTransmitterMetadata transmitterMetadata = ssfTransmitterClient.loadTransmitterMetadata(model);
        streamClient.deleteStream(transmitterMetadata, model.getTransmitterAccessToken(), model.getStreamId());
    }

    public SsfStreamRepresentation getStream(SsfReceiverModel model) {

        SsfTransmitterMetadata transmitterMetadata = ssfTransmitterClient.loadTransmitterMetadata(model);
        SsfStreamRepresentation streamRep = streamClient.getStream(transmitterMetadata, model.getTransmitterAccessToken(), model.getStreamId());
        return streamRep;
    }

}

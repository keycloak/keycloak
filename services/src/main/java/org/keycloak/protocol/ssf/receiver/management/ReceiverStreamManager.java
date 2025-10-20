package org.keycloak.protocol.ssf.receiver.management;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakContext;
import org.keycloak.protocol.ssf.receiver.ReceiverModel;
import org.keycloak.protocol.ssf.receiver.streamclient.SsfStreamClient;
import org.keycloak.protocol.ssf.receiver.transmitterclient.SsfTransmitterClient;
import org.keycloak.protocol.ssf.spi.SsfProvider;
import org.keycloak.protocol.ssf.stream.CreateStreamRequest;
import org.keycloak.protocol.ssf.stream.PollDeliveryMethodRepresentation;
import org.keycloak.protocol.ssf.stream.PushDeliveryMethodRepresentation;
import org.keycloak.protocol.ssf.stream.SsfStreamRepresentation;
import org.keycloak.protocol.ssf.transmitter.SsfTransmitterMetadata;
import org.keycloak.services.Urls;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.net.URI;

public class ReceiverStreamManager {

    protected static final Logger log = Logger.getLogger(ReceiverManager.class);

    protected final SsfStreamClient streamClient;

    protected final SsfTransmitterClient ssfTransmitterClient;

    public ReceiverStreamManager(SsfProvider ssfProvider) {
        this.streamClient = ssfProvider.streamClient();
        this.ssfTransmitterClient = ssfProvider.transmitterClient();
    }

    public SsfStreamRepresentation createReceiverStream(KeycloakContext context, ReceiverModel model) {

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

    protected CreateStreamRequest createCreateStreamRequest(KeycloakContext context, ReceiverModel model) {

        CreateStreamRequest createStreamRequest = new CreateStreamRequest();
        createStreamRequest.setDescription(model.getDescription());
        createStreamRequest.setEventsRequested(model.getEventsRequested());
        switch(model.getDeliveryMethod()) {
            case POLL -> createStreamRequest.setDelivery(new PollDeliveryMethodRepresentation(null));
            case PUSH -> {
                String pushUrl = createPushUrl(context, model);
                createStreamRequest.setDelivery(new PushDeliveryMethodRepresentation(URI.create(pushUrl), model.getPushAuthorizationToken()));
            }
        }

        return createStreamRequest;
    }

    public String createPushUrl(KeycloakContext context, ReceiverModel model) {
        String issuer = Urls.realmIssuer(context.getUri().getBaseUri(), context.getRealm().getName());
        String pushUrl = issuer + "/ssf/push/" + model.getAlias();
        return pushUrl;
    }

    public void deleteReceiverStream(ReceiverModel model) {

        SsfTransmitterMetadata transmitterMetadata = ssfTransmitterClient.loadTransmitterMetadata(model);
        streamClient.deleteStream(transmitterMetadata, model.getTransmitterAccessToken(), model.getStreamId());
    }

    public SsfStreamRepresentation getStream(ReceiverModel model) {

        SsfTransmitterMetadata transmitterMetadata = ssfTransmitterClient.loadTransmitterMetadata(model);
        SsfStreamRepresentation streamRep = streamClient.getStream(transmitterMetadata, model.getTransmitterAccessToken(), model.getStreamId());
        return streamRep;
    }

}

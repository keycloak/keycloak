package org.keycloak.protocol.ssf.receiver.spi;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.keys.KeyProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ssf.event.delivery.DeliveryMethod;
import org.keycloak.protocol.ssf.keys.SsfTransmitterKeyManager;
import org.keycloak.protocol.ssf.receiver.SsfReceiverKeyModel;
import org.keycloak.protocol.ssf.receiver.SsfReceiverModel;
import org.keycloak.protocol.ssf.receiver.streamclient.DefaultSsfStreamClient;
import org.keycloak.protocol.ssf.receiver.transmitterclient.SsfTransmitterClient;
import org.keycloak.protocol.ssf.receiver.verification.SsfStreamVerificationState;
import org.keycloak.protocol.ssf.receiver.verification.SsfStreamVerificationStore;
import org.keycloak.protocol.ssf.spi.SsfProvider;
import org.keycloak.protocol.ssf.stream.PollSetDeliveryMethodRepresentation;
import org.keycloak.protocol.ssf.stream.PushDeliveryMethodRepresentation;
import org.keycloak.protocol.ssf.stream.SsfStreamRepresentation;
import org.keycloak.protocol.ssf.stream.StreamStatus;
import org.keycloak.protocol.ssf.transmitter.SsfTransmitterMetadata;

import java.net.URI;
import java.security.PublicKey;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DefaultSsfReceiver implements SsfReceiver {

    protected static final Logger log = Logger.getLogger(DefaultSsfStreamClient.class);

    protected final KeycloakSession session;

    protected final SsfProvider ssfProvider;

    protected final SsfReceiverModel receiverModel;

    public DefaultSsfReceiver(KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.ssfProvider = session.getProvider(SsfProvider.class);
        if (model instanceof SsfReceiverModel rm) {
            this.receiverModel = rm;
        } else {
            this.receiverModel = new SsfReceiverModel(model);
        }
    }

    public DefaultSsfReceiver(KeycloakSession session) {
        this(session, new ComponentModel());
    }

    @Override
    public SsfReceiverModel getReceiverModel() {
        return receiverModel;
    }

    @Override
    public void close() {
        // NOOP
    }

    @Override
    public Stream<KeyWrapper> getKeys() {

        RealmModel realm = session.getContext().getRealm();

        return realm.getComponentsStream(receiverModel.getId(), KeyProvider.class.getName()).map(SsfReceiverKeyModel::new).map(receiverKey -> {
            String encodedPublicKey = receiverKey.getPublicKey();
            PublicKey publicKey = SsfTransmitterKeyManager.decodePublicKey(encodedPublicKey, receiverKey.getType(), receiverKey.getAlgorithm());
            KeyWrapper key = new KeyWrapper();
            key.setKid(receiverKey.getKid());
            key.setAlgorithm(receiverKey.getAlgorithm());
            key.setUse(receiverKey.getKeyUse());
            key.setType(receiverKey.getType());
            key.setPublicKey(publicKey);
            return key;
        });
    }

    @Override
    public SsfTransmitterMetadata refreshTransmitterMetadata() {

        SsfTransmitterClient ssfTransmitterClient = ssfProvider.transmitterClient();

        RealmModel realm = session.getContext().getRealm();
        boolean cleared = ssfTransmitterClient.clearTransmitterMetadata(receiverModel);
        if (cleared) {
            log.debugf("Cleared Transmitter metadata. realm=%s receiver=%s", realm.getName(), receiverModel.getAlias());
        }

        SsfTransmitterMetadata transmitterMetadata = ssfTransmitterClient.loadTransmitterMetadata(receiverModel);

        log.debugf("Refreshed Transmitter metadata. realm=%s receiver=%s", realm.getName(), receiverModel.getAlias());

        return transmitterMetadata;
    }

    @Override
    public void unregisterStream() {
        try {
            if (Boolean.TRUE.equals(receiverModel.getManagedStream())) {
                RealmModel realm = session.getContext().getRealm();
                ssfProvider.receiverStreamManager().deleteReceiverStream(receiverModel);
                log.debugf("Removed managed stream for receiver component with id %s. realm=%s alias=%s stream_id=%s", realm.getName(), receiverModel.getId(), receiverModel.getAlias(), receiverModel.getStreamId());
            }
        } catch (Exception e) {
            log.errorf("Could not delete receiver stream with id %s. alias=%s", receiverModel.getId(), receiverModel.getAlias());
        }
    }

    @Override
    public SsfReceiverModel registerStream() {

        SsfStreamRepresentation streamRep = ssfProvider.receiverStreamManager().createReceiverStream(session.getContext(), receiverModel);
        updateReceiverModelFromStreamRepresentation(streamRep);

        return receiverModel;
    }

    @Override
    public SsfReceiverModel importStream() {

        SsfStreamRepresentation streamRep = ssfProvider.receiverStreamManager().getStream(receiverModel);
        updateReceiverModelFromStreamRepresentation(streamRep);

        return receiverModel;
    }

    protected void updateReceiverModelFromStreamRepresentation(SsfStreamRepresentation streamRep) {

        receiverModel.setStreamId(streamRep.getId());
        receiverModel.setIssuer(streamRep.getIssuer().toString());

        Object audience = streamRep.getAudience();
        if (audience != null) {
            if (audience instanceof String audienceString) {
                receiverModel.setAudience(Set.of(audienceString));
            } else if (audience instanceof Collection<?> audienceColl) {
                receiverModel.setAudience(Set.copyOf((Collection<String>) audienceColl));
            }
        }

        DeliveryMethod deliveryMethod = streamRep.getDelivery().getMethod();
        receiverModel.setDeliveryMethod(deliveryMethod);
        switch(deliveryMethod) {
            case PUSH -> {
                var pushDelivery = (PushDeliveryMethodRepresentation)streamRep.getDelivery();
                receiverModel.setPushAuthorizationHeader(pushDelivery.getAuthorizationHeader());
                receiverModel.setReceiverPushUrl(pushDelivery.getEndpointUrl());
            }
            case POLL -> {
                var pollDelivery = (PollSetDeliveryMethodRepresentation)streamRep.getDelivery();
                receiverModel.setTransmitterPollUrl(pollDelivery.getEndpointUrl());
            }
        }

        receiverModel.setEventsDelivered(streamRep.getEventsDelivered().stream().map(URI::toString).collect(Collectors.toSet()));
        if (receiverModel.getDescription() == null) {
            receiverModel.setDescription(streamRep.getDescription());
        }
    }

    @Override
    public void requestVerification() {

        SsfStreamVerificationStore storage = ssfProvider.verificationStore();

        // store current verification state
        RealmModel realm = session.getContext().getRealm();
        SsfStreamVerificationState verificationState = storage.getVerificationState(realm, receiverModel);
        if (verificationState != null) {
            log.debugf("Resetting pending verification state for stream. %s", verificationState);
            storage.clearVerificationState(realm, receiverModel);
        }

        SsfTransmitterClient ssfTransmitterClient = ssfProvider.transmitterClient();
        SsfTransmitterMetadata transmitterMetadata = ssfTransmitterClient.loadTransmitterMetadata(receiverModel);
        String state = UUID.randomUUID().toString();

        // store current verification state
        storage.setVerificationState(realm, receiverModel, state);

        ssfProvider.verificationClient().requestVerification(receiverModel, transmitterMetadata, state);
    }

    @Override
    public void updateStreamStatus(StreamStatus newStatus) {
        StreamStatus oldStatus = receiverModel.getStreamStatus();
        receiverModel.setStreamStatus(newStatus);
        log.debugf("Changed stream status from %s to %s", oldStatus, newStatus);
    }
}

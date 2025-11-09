package org.keycloak.protocol.ssf.receiver;

import org.jboss.logging.Logger;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ssf.receiver.transmitter.SsfTransmitterClient;
import org.keycloak.protocol.ssf.receiver.verification.SsfStreamVerificationState;
import org.keycloak.protocol.ssf.receiver.verification.SsfStreamVerificationStore;
import org.keycloak.protocol.ssf.spi.SsfProvider;
import org.keycloak.protocol.ssf.receiver.transmitter.SsfTransmitterMetadata;

import java.util.UUID;

public class SsfReceiverProvider implements IdentityProvider<SsfReceiverProviderConfig> {

    protected static final Logger log = Logger.getLogger(SsfReceiverProvider.class);

    private final KeycloakSession session;

    private final SsfReceiverProviderConfig model;

    public SsfReceiverProvider(KeycloakSession session, SsfReceiverProviderConfig model) {
        this.session = session;
        this.model = model;
    }

    @Override
    public SsfReceiverProviderConfig getConfig() {
        return new SsfReceiverProviderConfig(model);
    }

    public void requestVerification() {

        var ssfProvider = session.getProvider(SsfProvider.class);
        SsfStreamVerificationStore storage = ssfProvider.verificationStore();

        // store current verification state
        RealmModel realm = session.getContext().getRealm();
        SsfStreamVerificationState verificationState = storage.getVerificationState(realm, model.getAlias(), model.getStreamId());
        if (verificationState != null) {
            log.debugf("Resetting pending verification state for stream. %s", verificationState);
            storage.clearVerificationState(realm, model.getAlias(), model.getStreamId());
        }

        SsfReceiver ssfReceiver = SsfReceiverProviderFactory.getSsfReceiver(session, realm, model.getAlias());

        SsfTransmitterClient ssfTransmitterClient = ssfProvider.transmitterClient();
        SsfTransmitterMetadata transmitterMetadata = ssfTransmitterClient.loadTransmitterMetadata(ssfReceiver);
        String state = UUID.randomUUID().toString();

        // store current verification state
        storage.setVerificationState(realm, model.getAlias(), model.getStreamId(), state);

        ssfProvider.verificationClient().requestVerification(ssfReceiver, transmitterMetadata, state);
    }

    @Override
    public void close() {
        // NOOP
    }
}

package org.keycloak.protocol.ssf.receiver;

import java.util.UUID;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ssf.receiver.registration.SsfReceiverRegistrationProviderConfig;
import org.keycloak.protocol.ssf.receiver.spi.SsfReceiverProvider;
import org.keycloak.protocol.ssf.receiver.transmitter.SsfTransmitterClient;
import org.keycloak.protocol.ssf.receiver.transmitter.SsfTransmitterMetadata;
import org.keycloak.protocol.ssf.receiver.verification.SsfStreamVerificationState;
import org.keycloak.protocol.ssf.receiver.verification.SsfStreamVerificationStore;

import org.jboss.logging.Logger;

public class DefaultSsfReceiver implements SsfReceiver {

    protected static final Logger LOG = Logger.getLogger(DefaultSsfReceiver.class);

    protected final KeycloakSession session;

    protected final SsfReceiverProvider ssfReceiverProvider;

    protected SsfReceiverRegistrationProviderConfig receiverProviderConfig;

    public DefaultSsfReceiver(KeycloakSession session, SsfReceiverRegistrationProviderConfig receiverProviderConfig) {
        this.session = session;
        this.ssfReceiverProvider = session.getProvider(SsfReceiverProvider.class);
        this.receiverProviderConfig = receiverProviderConfig;
    }

    @Override
    public SsfReceiverRegistrationProviderConfig getConfig() {
        return receiverProviderConfig;
    }

    @Override
    public void close() {
        // NOOP
    }

    @Override
    public SsfTransmitterMetadata refreshTransmitterMetadata() {

        SsfTransmitterClient ssfTransmitterClient = ssfReceiverProvider.transmitterClient();

        RealmModel realm = session.getContext().getRealm();
        boolean cleared = ssfTransmitterClient.clearTransmitterMetadata(this);
        if (cleared) {
            LOG.debugf("Cleared Transmitter metadata. realm=%s receiver=%s", realm.getName(), receiverProviderConfig.getAlias());
        }

        SsfTransmitterMetadata transmitterMetadata = ssfTransmitterClient.loadTransmitterMetadata(this);

        LOG.debugf("Refreshed Transmitter metadata. realm=%s receiver=%s", realm.getName(), receiverProviderConfig.getAlias());

        return transmitterMetadata;
    }

    @Override
    public String getTransmitterConfigUrl() {

        // TODO do we need a providerConfig.getTransmitterConfigUrl() override?
        String transmitterConfigUrl = null;
        if (transmitterConfigUrl == null) {
            String configUrl = receiverProviderConfig.getIssuer();
            if (!configUrl.endsWith("/")) {
                configUrl+="/";
            }
            configUrl = configUrl + ".well-known/ssf-configuration";
            transmitterConfigUrl = configUrl;
        }

        return transmitterConfigUrl;
    }

    @Override
    public SsfTransmitterMetadata getTransmitterMetadata() {

        SsfTransmitterClient ssfTransmitterClient = ssfReceiverProvider.transmitterClient();
        SsfTransmitterMetadata transmitterMetadata = ssfTransmitterClient.loadTransmitterMetadata(this);
        return transmitterMetadata;
    }

    @Override
    public void requestVerification() {

        SsfStreamVerificationStore storage = ssfReceiverProvider.verificationStore();

        // store current verification state
        RealmModel realm = session.getContext().getRealm();
        SsfStreamVerificationState verificationState = storage.getVerificationState(realm, receiverProviderConfig.getAlias(), receiverProviderConfig.getStreamId());
        if (verificationState != null) {
            LOG.debugf("Resetting pending verification state for stream. %s", verificationState);
            storage.clearVerificationState(realm, receiverProviderConfig.getAlias(), receiverProviderConfig.getStreamId());
        }

        SsfTransmitterClient ssfTransmitterClient = ssfReceiverProvider.transmitterClient();
        SsfTransmitterMetadata transmitterMetadata = ssfTransmitterClient.loadTransmitterMetadata(this);
        String state = UUID.randomUUID().toString();

        // store current verification state
        storage.setVerificationState(realm, receiverProviderConfig.getAlias(), receiverProviderConfig.getStreamId(), state);

        ssfReceiverProvider.verificationClient().requestVerification(this, transmitterMetadata, state);
    }
}

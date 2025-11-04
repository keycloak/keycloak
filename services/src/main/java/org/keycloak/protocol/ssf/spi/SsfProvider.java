package org.keycloak.protocol.ssf.spi;

import org.keycloak.protocol.ssf.event.SecurityEventToken;
import org.keycloak.protocol.ssf.event.delivery.push.PushEndpoint;
import org.keycloak.protocol.ssf.event.processor.SsfSecurityEventContext;
import org.keycloak.protocol.ssf.receiver.SsfReceiverModel;
import org.keycloak.protocol.ssf.receiver.management.SsfReceiverManagementEndpoint;
import org.keycloak.protocol.ssf.receiver.management.SsfReceiverManager;
import org.keycloak.protocol.ssf.receiver.management.SsfReceiverStreamManager;
import org.keycloak.protocol.ssf.receiver.streamclient.SsfStreamClient;
import org.keycloak.protocol.ssf.receiver.transmitterclient.SsfTransmitterClient;
import org.keycloak.protocol.ssf.receiver.verification.SsfVerificationClient;
import org.keycloak.protocol.ssf.receiver.verification.SsfStreamVerificationStore;
import org.keycloak.provider.Provider;

import static org.keycloak.utils.KeycloakSessionUtil.getKeycloakSession;

public interface SsfProvider extends Provider {

    @Override
    default void close() {
        // NOOP
    }

    SecurityEventToken parseSecurityEventToken(String encodedSecurityEventToken, SsfSecurityEventContext securityEventContext);

    void processSecurityEvents(SsfSecurityEventContext ssfSecurityEventContext);

    SsfSecurityEventContext createSecurityEventContext(SecurityEventToken securityEventToken, SsfReceiverModel receiverModel);

    // SSF Receiver Support
    PushEndpoint pushEndpoint();

    SsfReceiverManagementEndpoint receiverManagementEndpoint();

    SsfReceiverStreamManager receiverStreamManager();

    SsfStreamVerificationStore verificationStore();

    SsfVerificationClient verificationClient();

    SsfStreamClient streamClient();

    SsfTransmitterClient transmitterClient();

    SsfReceiverManager receiverManager();

    static SsfProvider current() {
        return getKeycloakSession().getProvider(SsfProvider.class);
    }
}

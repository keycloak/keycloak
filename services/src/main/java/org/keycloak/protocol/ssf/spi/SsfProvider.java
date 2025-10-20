package org.keycloak.protocol.ssf.spi;


import org.keycloak.protocol.ssf.event.SecurityEventToken;
import org.keycloak.protocol.ssf.event.delivery.push.PushEndpoint;
import org.keycloak.protocol.ssf.event.processor.SsfEventContext;
import org.keycloak.protocol.ssf.receiver.management.ReceiverManagementEndpoint;
import org.keycloak.protocol.ssf.receiver.management.ReceiverManager;
import org.keycloak.protocol.ssf.receiver.management.ReceiverStreamManager;
import org.keycloak.protocol.ssf.receiver.streamclient.SsfStreamClient;
import org.keycloak.protocol.ssf.receiver.transmitterclient.SsfTransmitterClient;
import org.keycloak.protocol.ssf.receiver.verification.SsfVerificationClient;
import org.keycloak.protocol.ssf.receiver.verification.VerificationStore;
import org.keycloak.provider.Provider;

import static org.keycloak.utils.KeycloakSessionUtil.getKeycloakSession;

public interface SsfProvider extends Provider {

    @Override
    default void close() {
        // NOOP
    }

    SecurityEventToken parseSecurityEventToken(String encodedSecurityEventToken, SsfEventContext processingContext);

    void processSecurityEvents(SsfEventContext ssfEventContext);

    SsfEventContext createSecurityEventProcessingContext(SecurityEventToken securityEventToken, String receiverAlias);

    // SSF Receiver Support
    PushEndpoint pushEndpoint();

    ReceiverManagementEndpoint receiverManagementEndpoint();

    ReceiverStreamManager receiverStreamManager();

    VerificationStore verificationStore();

    SsfVerificationClient verificationClient();

    SsfStreamClient streamClient();

    SsfTransmitterClient transmitterClient();

    ReceiverManager receiverManager();

    static SsfProvider current() {
        return getKeycloakSession().getProvider(SsfProvider.class);
    }
}

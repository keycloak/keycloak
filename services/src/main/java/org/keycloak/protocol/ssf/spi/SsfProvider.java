package org.keycloak.protocol.ssf.spi;

import org.keycloak.protocol.ssf.endpoint.SsfPushDeliveryResource;
import org.keycloak.protocol.ssf.event.SecurityEventToken;
import org.keycloak.protocol.ssf.event.processor.SsfSecurityEventContext;
import org.keycloak.protocol.ssf.receiver.SsfReceiver;
import org.keycloak.protocol.ssf.receiver.transmitter.SsfTransmitterClient;
import org.keycloak.protocol.ssf.receiver.verification.SsfStreamVerificationStore;
import org.keycloak.protocol.ssf.receiver.verification.SsfVerificationClient;
import org.keycloak.provider.Provider;

/**
 * SsfProvider exposes the SSF infrastructure components.
 */
public interface SsfProvider extends Provider {

    @Override
    default void close() {
        // NOOP
    }

    SecurityEventToken parseSecurityEventToken(String encodedSecurityEventToken, SsfSecurityEventContext securityEventContext);

    void processSecurityEvents(SsfSecurityEventContext securityEventContext);

    SsfSecurityEventContext createSecurityEventContext(SecurityEventToken securityEventToken, SsfReceiver receiver);

    SsfPushDeliveryResource pushDeliveryEndpoint();

    SsfStreamVerificationStore verificationStore();

    SsfVerificationClient verificationClient();

    SsfTransmitterClient transmitterClient();

}

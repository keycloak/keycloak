package org.keycloak.protocol.ssf.spi;

import org.keycloak.protocol.ssf.endpoint.SsfPushDeliveryResource;
import org.keycloak.protocol.ssf.event.SecurityEventToken;
import org.keycloak.protocol.ssf.event.processor.SsfEventContext;
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

    SecurityEventToken parseSecurityEventToken(String encodedSecurityEventToken, SsfEventContext eventContext);

    SsfEventContext createEventContext(SecurityEventToken securityEventToken, SsfReceiver receiver);

    void processEvents(SecurityEventToken securityEventToken, SsfEventContext eventContext);

    SsfPushDeliveryResource pushDeliveryEndpoint();

    SsfStreamVerificationStore verificationStore();

    SsfVerificationClient verificationClient();

    SsfTransmitterClient transmitterClient();

}

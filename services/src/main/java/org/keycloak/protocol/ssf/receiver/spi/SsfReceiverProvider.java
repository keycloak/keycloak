package org.keycloak.protocol.ssf.receiver.spi;

import org.keycloak.protocol.ssf.event.token.SsfSecurityEventToken;
import org.keycloak.protocol.ssf.receiver.SsfReceiver;
import org.keycloak.protocol.ssf.receiver.event.processor.SsfEventContext;
import org.keycloak.protocol.ssf.receiver.transmitter.SsfTransmitterClient;
import org.keycloak.protocol.ssf.receiver.verification.SsfStreamVerificationStore;
import org.keycloak.protocol.ssf.receiver.verification.SsfVerificationClient;
import org.keycloak.provider.Provider;

/**
 * SsfProvider exposes the SSF Receiver infrastructure components.
 */
public interface SsfReceiverProvider extends Provider {

    @Override
    default void close() {
        // NOOP
    }

    SsfSecurityEventToken parseSecurityEventToken(String encodedSecurityEventToken, SsfEventContext eventContext);

    SsfEventContext createEventContext(SsfSecurityEventToken securityEventToken, SsfReceiver receiver);

    void processEvents(SsfSecurityEventToken securityEventToken, SsfEventContext eventContext);

    SsfStreamVerificationStore verificationStore();

    SsfVerificationClient verificationClient();

    SsfTransmitterClient transmitterClient();

}

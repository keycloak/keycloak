package org.keycloak.ssf.receiver;

import org.keycloak.ssf.event.token.SsfSecurityEventToken;
import org.keycloak.ssf.receiver.event.processor.SsfEventContext;
import org.keycloak.ssf.receiver.transmitter.SsfTransmitterClient;
import org.keycloak.ssf.receiver.verification.SsfStreamVerificationStore;
import org.keycloak.ssf.receiver.verification.SsfVerificationClient;
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

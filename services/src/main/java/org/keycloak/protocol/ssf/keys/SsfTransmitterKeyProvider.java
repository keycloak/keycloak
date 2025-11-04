package org.keycloak.protocol.ssf.keys;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.keys.KeyProvider;
import org.keycloak.models.KeycloakSession;

import java.util.stream.Stream;

/**
 * Dummy class used in combination with ReceiverKey ComponentModels
 */
public class SsfTransmitterKeyProvider implements KeyProvider {

    protected static final Logger log = Logger.getLogger(SsfTransmitterKeyProvider.class);

    public SsfTransmitterKeyProvider(KeycloakSession session, ComponentModel model) {
    }

    @Override
    public Stream<KeyWrapper> getKeysStream() {
        return Stream.empty();
    }

}

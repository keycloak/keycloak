package org.keycloak.protocol.ssf.receiver.spi;

import org.keycloak.component.ComponentFactory;

public interface SsfReceiverFactory extends ComponentFactory<SsfReceiver, SsfReceiver> {

    @Override
    default void close() {

    }
}

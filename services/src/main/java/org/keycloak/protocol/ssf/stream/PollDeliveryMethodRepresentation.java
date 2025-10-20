package org.keycloak.protocol.ssf.stream;

import org.keycloak.protocol.ssf.event.delivery.DeliveryMethod;

import java.net.URI;

public class PollDeliveryMethodRepresentation extends AbstractDeliveryMethodRepresentation {

    public PollDeliveryMethodRepresentation(URI endpointUrl) {
        super(DeliveryMethod.POLL, endpointUrl);
    }
}

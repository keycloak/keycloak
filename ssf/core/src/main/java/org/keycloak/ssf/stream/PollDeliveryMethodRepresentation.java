package org.keycloak.ssf.stream;

import java.net.URI;

public class PollDeliveryMethodRepresentation extends AbstractDeliveryMethodRepresentation {

    public PollDeliveryMethodRepresentation(URI endpointUrl) {
        super(DeliveryMethod.POLL, endpointUrl);
    }
}

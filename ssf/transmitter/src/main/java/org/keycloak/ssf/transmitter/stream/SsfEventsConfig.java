package org.keycloak.ssf.transmitter.stream;

import java.util.Set;

/**
 *
 * @param eventsSupported
 * @param eventsDelivered
 */
public record SsfEventsConfig(Set<String> eventsSupported, Set<String> eventsRequested, Set<String> eventsDelivered) {
}

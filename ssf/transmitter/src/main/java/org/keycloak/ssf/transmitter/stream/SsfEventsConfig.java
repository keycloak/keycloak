package org.keycloak.ssf.transmitter.stream;

import java.util.Set;

/**
 * The three event-type sets of a stream, as advertised in the stream
 * configuration (SSF §7.1.1).
 *
 * @param eventsSupported event types the transmitter can deliver on this stream
 * @param eventsRequested event types the receiver asked for
 * @param eventsDelivered event types the transmitter will actually deliver
 *                        (the intersection of supported and requested)
 */
public record SsfEventsConfig(Set<String> eventsSupported, Set<String> eventsRequested, Set<String> eventsDelivered) {
}

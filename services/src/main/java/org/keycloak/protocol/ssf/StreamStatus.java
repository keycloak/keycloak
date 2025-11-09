package org.keycloak.protocol.ssf;

public enum StreamStatus {

    /**
     * The Transmitter MUST transmit events over the stream, according to the stream's configured delivery method.
     */
    enabled,

    /**
     * The Transmitter MUST NOT transmit events over the stream. The Transmitter will hold any events it would have transmitted while paused, and SHOULD transmit them when the stream's status becomes "enabled". If a Transmitter holds successive events that affect the same Subject Principal, then the Transmitter MUST make sure that those events are transmitted in the order of time that they were generated OR the Transmitter MUST send only the last events that do not require the previous events affecting the same Subject Principal to be processed by the Receiver, because the previous events are either cancelled by the later events or the previous events are outdated.
     */
    paused,

    /**
     * The Transmitter MUST NOT transmit events over the stream and will not hold any events for later transmission.
     */
    disabled
}

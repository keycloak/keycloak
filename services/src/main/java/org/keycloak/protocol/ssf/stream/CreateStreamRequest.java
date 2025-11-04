package org.keycloak.protocol.ssf.stream;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public class CreateStreamRequest {

        /**
         * Receiver-Supplied, OPTIONAL. An array of URIs identifying the set of events that the Receiver requested. A Receiver SHOULD request only the events that it understands and it can act on. This is configurable by the Receiver. A Transmitter MUST ignore any array values that it does not understand. This array SHOULD NOT be empty.
         */
        @JsonProperty("events_requested")
        private Set<String> eventsRequested;

        /**
         * Receiver-Supplied, OPTIONAL. A JSON object containing a set of name/value pairs specifying configuration parameters for the SET delivery method. The actual delivery method is identified by the special key "method" with the value being a URI as defined in Section 10.3.1. The value of the "delivery" field contains two sub-fields:
         */
        @JsonProperty("delivery")
        private AbstractSetDeliveryMethodRepresentation delivery;

        /**
         * Receiver-Supplied, OPTIONAL. A string that describes the properties of the stream. This is useful in multi-stream systems to identify the stream for human actors. The transmitter MAY truncate the string beyond an allowed max length.
         */
        @JsonProperty("description")
        private String description;

        public Set<String> getEventsRequested() {
            return eventsRequested;
        }

        public void setEventsRequested(Set<String> eventsRequested) {
            this.eventsRequested = eventsRequested;
        }

        public AbstractSetDeliveryMethodRepresentation getDelivery() {
            return delivery;
        }

        public void setDelivery(AbstractSetDeliveryMethodRepresentation delivery) {
            this.delivery = delivery;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

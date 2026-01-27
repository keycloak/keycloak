package org.keycloak.protocol.ssf.event.types;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.protocol.ssf.event.SsfStandardEvents;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Custom deserializer for Security Events.
 * <pre>
 *      "events" (Security Events) Claim
 *       This claim contains a set of event statements that each provide
 *       information describing a single logical event that has occurred
 *       about a security subject (e.g., a state change to the subject).
 *       Multiple event identifiers with the same value MUST NOT be used.
 *       The "events" claim MUST NOT be used to express multiple
 *       independent logical events.
 *
 *       The value of the "events" claim is a JSON object whose members are
 *       name/value pairs whose names are URIs identifying the event
 *       statements being expressed.  Event identifiers SHOULD be stable
 *       values (e.g., a permanent URL for an event specification).  For
 *       each name present, the corresponding value MUST be a JSON object.
 *       The JSON object MAY be an empty object ("{}"), or it MAY be a JSON
 *       object containing data described by the profiling specification.
 * </pre>
 * See: https://datatracker.ietf.org/doc/html/rfc8417#section-2.2
 */
public class SsfEventMapJsonDeserializer extends JsonDeserializer<Map<String, SsfEvent>> {

    @Override
    public Map<String, SsfEvent> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        JsonNode node = mapper.readTree(p);

        Map<String, SsfEvent> eventsMap = new HashMap<>();

        for (Map.Entry<String, JsonNode> entry : node.properties()) {
            String eventType = entry.getKey();  // Extracts event type key
            JsonNode eventData = entry.getValue(); // Extracts event data

            Class<? extends SsfEvent> eventClass = SsfStandardEvents.getSecurityEventType(eventType);

            if (eventClass == null) {
                throw new IOException("Unknown event type: " + eventType);
            }

            SsfEvent event = mapper.treeToValue(eventData, eventClass);
            event.setEventType(eventType);  // Manually set event type since it's not in JSON
            eventsMap.put(eventType, event);
        }

        return eventsMap;
    }
}

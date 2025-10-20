package org.keycloak.protocol.ssf.event.types;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.protocol.ssf.event.SecurityEvents;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SecurityEventMapJsonDeserializer extends JsonDeserializer<Map<String, SsfEvent>> {

    @Override
    public Map<String, SsfEvent> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        JsonNode node = mapper.readTree(p);

        Map<String, SsfEvent> eventsMap = new HashMap<>();

        for (Map.Entry<String, JsonNode> entry : node.properties()) {
            String eventType = entry.getKey();  // Extracts event type key
            JsonNode eventData = entry.getValue(); // Extracts event data

            Class<? extends SsfEvent> eventClass = SecurityEvents.getSecurityEventType(eventType);

            if (eventClass == null) {
                throw new IOException("Unknown event type: " + eventType);
            }

            SsfEvent event = mapper.treeToValue(eventData, eventClass);
            event.eventType = eventType;  // Manually set event type since it's not in JSON
            eventsMap.put(eventType, event);
        }

        return eventsMap;
    }
}

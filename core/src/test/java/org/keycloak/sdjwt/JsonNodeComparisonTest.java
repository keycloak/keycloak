package org.keycloak.sdjwt;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */
public class JsonNodeComparisonTest {
    @Test
    public void testJsonNodeEquality() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        JsonNode node1 = mapper.readTree("{\"name\":\"John\", \"age\":30}");
        JsonNode node2 = mapper.readTree("{\"age\":30, \"name\":\"John\"}");

        assertEquals("JsonNode objects should be equal", node1, node2);
    }
}

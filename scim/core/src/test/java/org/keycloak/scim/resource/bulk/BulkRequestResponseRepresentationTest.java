package org.keycloak.scim.resource.bulk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class BulkRequestResponseRepresentationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesBulkRequestFixture() throws IOException {
        BulkRequest request = readFixture("bulk/bulk-request-create-user-group.json", BulkRequest.class);

        assertEquals(BulkRequest.SCHEMA, request.getSchemas().iterator().next());
        assertEquals(1, request.getFailOnErrors());
        assertEquals(2, request.getOperations().size());
        assertEquals("POST", request.getOperations().get(0).getMethod());
        assertEquals("/Users", request.getOperations().get(0).getPath());
        assertEquals("Engineering", request.getOperations().get(1).getData().get("displayName").asText());
    }

    @Test
    void serializesBulkResponseFixtureWithMixedStatuses() throws IOException {
        BulkResponse response = readFixture("bulk/bulk-response-mixed-results.json", BulkResponse.class);

        assertEquals(2, response.getOperations().size());
        assertEquals("201", response.getOperations().get(0).getStatus());
        assertEquals("400", response.getOperations().get(1).getStatus());

        String json = objectMapper.writeValueAsString(response);
        JsonNode serialized = objectMapper.readTree(json);
        assertEquals(BulkResponse.SCHEMA, serialized.get("schemas").get(0).asText());
        assertEquals("201", serialized.get("Operations").get(0).get("status").asText());
        assertEquals("400", serialized.get("Operations").get(1).get("status").asText());
        assertNotNull(serialized.get("Operations").get(1).get("response").get("detail"));
    }

    @Test
    void preservesAbsentOptionalRequestFields() throws JsonProcessingException {
        BulkRequest request = objectMapper.readValue("{\"Operations\":[{\"method\":\"DELETE\",\"path\":\"/Users/123\"}]}", BulkRequest.class);

        assertEquals(null, request.getFailOnErrors());
        assertEquals(null, request.getOperations().get(0).getData());
        assertEquals(null, request.getOperations().get(0).getBulkId());
    }

    @Test
    void rejectsMalformedJson() {
        assertThrows(JsonProcessingException.class, () -> objectMapper.readValue("{\"Operations\":[", BulkRequest.class));
    }

    private <T> T readFixture(String name, Class<T> type) throws IOException {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(name)) {
            assertNotNull(stream, "Missing fixture: " + name);
            return objectMapper.readValue(stream, type);
        }
    }
}

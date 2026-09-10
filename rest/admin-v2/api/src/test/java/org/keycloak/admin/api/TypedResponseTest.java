package org.keycloak.admin.api;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypedResponseTest {

    @Test
    void getResponseExposesRawResponse() {
        TrackingResponse response = new TrackingResponse("payload", 201);
        TypedResponse<String> typedResponse = new TypedResponse<>(response, String.class);

        assertSame(response, typedResponse.getResponse());
    }

    @Test
    void readEntityUsesConfiguredType() {
        TestEntity entity = new TestEntity("client-123");
        TrackingResponse response = new TrackingResponse(entity, 201);
        TypedResponse<TestEntity> typedResponse = new TypedResponse<>(response, TestEntity.class);

        assertEquals(entity, typedResponse.readEntity());
        assertEquals(TestEntity.class, response.lastEntityType);
    }

    @Test
    void closeDelegatesToWrappedResponse() {
        TrackingResponse response = new TrackingResponse("payload", 200);
        TypedResponse<String> typedResponse = new TypedResponse<>(response, String.class);

        typedResponse.close();

        assertTrue(response.closed);
    }

    @Test
    void nullArgumentsAreRejected() {
        TrackingResponse response = new TrackingResponse("payload", 200);

        assertThrows(NullPointerException.class, () -> new TypedResponse<String>(null, String.class));
        assertThrows(NullPointerException.class, () -> new TypedResponse<>(response, null));
    }

    private static final class TestEntity {
        private final String clientId;

        private TestEntity(String clientId) {
            this.clientId = clientId;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof TestEntity)) {
                return false;
            }
            return clientId.equals(((TestEntity) other).clientId);
        }

        @Override
        public int hashCode() {
            return clientId.hashCode();
        }
    }

    private static final class TrackingResponse extends Response {

        private final Object entity;
        private final int status;
        private final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        private boolean closed;
        private Class<?> lastEntityType;

        private TrackingResponse(Object entity, int status) {
            this.entity = entity;
            this.status = status;
        }

        @Override
        public int getStatus() {
            return status;
        }

        @Override
        public StatusType getStatusInfo() {
            return Status.fromStatusCode(status);
        }

        @Override
        public Object getEntity() {
            return entity;
        }

        @Override
        public <T> T readEntity(Class<T> entityType) {
            lastEntityType = entityType;
            return entityType.cast(entity);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T readEntity(GenericType<T> entityType) {
            Class<?> rawType = entityType.getRawType();
            lastEntityType = rawType;
            return (T) rawType.cast(entity);
        }

        @Override
        public <T> T readEntity(Class<T> entityType, Annotation[] annotations) {
            return readEntity(entityType);
        }

        @Override
        public <T> T readEntity(GenericType<T> entityType, Annotation[] annotations) {
            return readEntity(entityType);
        }

        @Override
        public boolean hasEntity() {
            return entity != null;
        }

        @Override
        public boolean bufferEntity() {
            return false;
        }

        @Override
        public void close() {
            closed = true;
        }

        @Override
        public MediaType getMediaType() {
            return null;
        }

        @Override
        public Locale getLanguage() {
            return null;
        }

        @Override
        public int getLength() {
            return -1;
        }

        @Override
        public Set<String> getAllowedMethods() {
            return Set.of();
        }

        @Override
        public Map<String, NewCookie> getCookies() {
            return Map.of();
        }

        @Override
        public EntityTag getEntityTag() {
            return null;
        }

        @Override
        public Date getDate() {
            return null;
        }

        @Override
        public Date getLastModified() {
            return null;
        }

        @Override
        public URI getLocation() {
            return null;
        }

        @Override
        public Set<Link> getLinks() {
            return Set.of();
        }

        @Override
        public boolean hasLink(String relation) {
            return false;
        }

        @Override
        public Link getLink(String relation) {
            return null;
        }

        @Override
        public Link.Builder getLinkBuilder(String relation) {
            return Link.fromUri(URI.create("about:blank")).rel(relation);
        }

        @Override
        public MultivaluedMap<String, Object> getMetadata() {
            return headers;
        }

        @Override
        public MultivaluedMap<String, Object> getHeaders() {
            return headers;
        }

        @Override
        public MultivaluedMap<String, String> getStringHeaders() {
            MultivaluedMap<String, String> converted = new MultivaluedHashMap<>();
            headers.forEach((name, values) -> converted.put(name, values.stream()
                    .map(value -> value == null ? null : value.toString())
                    .collect(Collectors.toList())));
            return converted;
        }

        @Override
        public String getHeaderString(String name) {
            List<Object> values = headers.get(name);
            if (values == null || values.isEmpty()) {
                return null;
            }
            return values.stream().map(value -> value == null ? "" : value.toString()).reduce((left, right) -> left + "," + right).orElse(null);
        }
    }
}

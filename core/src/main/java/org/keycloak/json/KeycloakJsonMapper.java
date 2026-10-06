package org.keycloak.json;

import java.io.IOException;
import java.io.InputStream;

/**
 * Jackson-version-agnostic JSON mapper SPI. Each Jackson module provides an implementation
 * backed by its own ObjectMapper/JsonMapper. Discovered via {@link java.util.ServiceLoader}
 * and cached statically in {@link KeycloakJsonMapperFactory}.
 */
public interface KeycloakJsonMapper {

    default int getPriority() {
        return 0;
    }

    <T> T convertValue(Object fromValue, Class<T> toValueType);

    <T> T readValue(byte[] src, Class<T> valueType) throws IOException;

    <T> T readValue(String src, Class<T> valueType) throws IOException;

    <T> T readValue(InputStream src, Class<T> valueType) throws IOException;

    byte[] writeValueAsBytes(Object value) throws IOException;

    String writeValueAsString(Object value) throws IOException;
}

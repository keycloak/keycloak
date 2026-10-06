package org.keycloak.json;

import java.io.IOException;
import java.io.InputStream;

import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Jackson 2 implementation of {@link KeycloakJsonMapper} backed by the
 * {@link ObjectMapper} from {@link JsonSerialization}.
 */
public final class Jackson2JsonMapper implements KeycloakJsonMapper {

    private final ObjectMapper delegate = JsonSerialization.mapper;

    @Override
    public <T> T convertValue(Object fromValue, Class<T> toValueType) {
        return delegate.convertValue(fromValue, toValueType);
    }

    @Override
    public <T> T readValue(byte[] src, Class<T> valueType) throws IOException {
        return delegate.readValue(src, valueType);
    }

    @Override
    public <T> T readValue(String src, Class<T> valueType) throws IOException {
        return delegate.readValue(src, valueType);
    }

    @Override
    public <T> T readValue(InputStream src, Class<T> valueType) throws IOException {
        return delegate.readValue(src, valueType);
    }

    @Override
    public byte[] writeValueAsBytes(Object value) throws IOException {
        return delegate.writeValueAsBytes(value);
    }

    @Override
    public String writeValueAsString(Object value) throws IOException {
        return delegate.writeValueAsString(value);
    }
}

package org.keycloak.admin.client.jackson3;

import java.io.InputStream;

import org.keycloak.json.KeycloakJsonMapper;

import tools.jackson.databind.json.JsonMapper;

public final class Jackson3JsonMapper implements KeycloakJsonMapper {

    private static final JsonMapper MAPPER = Jackson3MapperHolder.MAPPER;

    @Override
    public int getPriority() {
        // Jackson 3 must have priority as our test framework brings Jackson 2
        return 1;
    }

    @Override
    public <T> T convertValue(Object fromValue, Class<T> toValueType) {
        return MAPPER.convertValue(fromValue, toValueType);
    }

    @Override
    public <T> T readValue(byte[] src, Class<T> valueType) {
        return MAPPER.readValue(src, valueType);
    }

    @Override
    public <T> T readValue(String src, Class<T> valueType) {
        return MAPPER.readValue(src, valueType);
    }

    @Override
    public <T> T readValue(InputStream src, Class<T> valueType) {
        return MAPPER.readValue(src, valueType);
    }

    @Override
    public byte[] writeValueAsBytes(Object value) {
        return MAPPER.writeValueAsBytes(value);
    }

    @Override
    public String writeValueAsString(Object value) {
        return MAPPER.writeValueAsString(value);
    }
}

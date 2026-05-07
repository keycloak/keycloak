package org.keycloak.admin.client;

import java.util.stream.Stream;

import jakarta.ws.rs.core.MediaType;

import org.keycloak.admin.client.deserializer.StreamDeserializer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;

public class JacksonProvider extends ResteasyJackson2Provider {
    private static final StreamDeserializer STREAM_DESERIALIZER = new StreamDeserializer();
    private static final SimpleModule STREAM_MODULE = new SimpleModule().addDeserializer(Stream.class, STREAM_DESERIALIZER);

    @Override
    public ObjectMapper locateMapper(Class<?> type, MediaType mediaType) {
        ObjectMapper objectMapper = super.locateMapper(type, mediaType);

        // Same like JSONSerialization class. Makes it possible to use admin-client against older versions of Keycloak server where the properties on representations might be different
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // The client must work with the newer versions of Keycloak server, which might contain the JSON fields not yet known by the client. So unknown fields will be ignored.
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        objectMapper.registerModule(STREAM_MODULE);
        return objectMapper;
    }
}

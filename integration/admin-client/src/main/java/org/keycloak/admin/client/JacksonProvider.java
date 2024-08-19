package org.keycloak.admin.client;

import jakarta.ws.rs.core.MediaType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;

public class JacksonProvider extends ResteasyJackson2Provider {

    @Override
    public ObjectMapper locateMapper(Class<?> type, MediaType mediaType) {
        ObjectMapper objectMapper = super.locateMapper(type, mediaType);

        // Same like JSONSerialization class. Makes it possible to use admin-client against older versions of Keycloak server where the properties on representations might be different
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        
        return objectMapper;
    }
}

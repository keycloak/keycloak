package org.keycloak.testframework.realm;

import com.fasterxml.jackson.databind.ObjectMapper;

public class RepresentationUtils {

    private static ObjectMapper objectMapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    public static <T> T clone(T t) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(t);
            return (T) objectMapper.readValue(bytes, t.getClass());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

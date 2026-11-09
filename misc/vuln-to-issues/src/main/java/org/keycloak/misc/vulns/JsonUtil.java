package org.keycloak.misc.vulns;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtil {

    private static final ObjectMapper om = new ObjectMapper();

    public static <T> T read(File src, Class<T> valueType) throws IOException {
        return om.readValue(src, valueType);
    }

    public static <T> T read(InputStream src, Class<T> valueType) throws IOException {
        return om.readValue(src, valueType);
    }

    public static JsonNode read(InputStream src) throws IOException {
        return om.readTree(src);
    }

    public static JsonNode read(byte[] src) throws IOException {
        return om.readTree(src);
    }

    public static byte[] write(Object value) throws IOException {
        return om.writeValueAsBytes(value);
    }

}

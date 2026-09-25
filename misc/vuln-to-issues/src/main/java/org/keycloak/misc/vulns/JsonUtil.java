package org.keycloak.misc.vulns;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class JsonUtil {

    private static final ObjectMapper om = new ObjectMapper();

    public static <T> T read(File src, Class<T> valueType) throws IOException {
        return om.readValue(src, valueType);
    }

    public static <T> T read(InputStream src, Class<T> valueType) throws IOException {
        return om.readValue(src, valueType);
    }

}

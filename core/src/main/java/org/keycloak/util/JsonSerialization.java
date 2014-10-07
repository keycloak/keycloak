package org.keycloak.util;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utility class to handle simple JSON serializable for Keycloak.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JsonSerialization {
    public static final ObjectMapper mapper = new ObjectMapper();
    public static final ObjectMapper prettyMapper = new ObjectMapper();
    public static final ObjectMapper sysPropertiesAwareMapper = new ObjectMapper(new SystemPropertiesJsonParserFactory());

    static {
        mapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
        prettyMapper.enable(SerializationConfig.Feature.INDENT_OUTPUT);
        prettyMapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
    }

    public static void writeValueToStream(OutputStream os, Object obj) throws IOException {
        mapper.writeValue(os, obj);

    }

    public static String writeValueAsString(Object obj) throws IOException {
        return mapper.writeValueAsString(obj);
    }

    public static byte[] writeValueAsBytes(Object obj) throws IOException {
        return mapper.writeValueAsBytes(obj);
    }

    public static <T> T readValue(byte[] bytes, Class<T> type) throws IOException {
        return mapper.readValue(bytes, type);
    }

    public static <T> T readValue(String bytes, Class<T> type) throws IOException {
        return mapper.readValue(bytes, type);
    }

    public static <T> T readValue(InputStream bytes, Class<T> type) throws IOException {
        return readValue(bytes, type, false);
    }

    public static <T> T readValue(InputStream bytes, Class<T> type, boolean replaceSystemProperties) throws IOException {
        if (replaceSystemProperties) {
            return sysPropertiesAwareMapper.readValue(bytes, type);
        } else {
            return mapper.readValue(bytes, type);
        }
    }



}

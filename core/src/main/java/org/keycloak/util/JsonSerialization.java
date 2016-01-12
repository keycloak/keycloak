package org.keycloak.util;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.type.TypeReference;

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

    public static void writeValuePrettyToStream(OutputStream os, Object obj) throws IOException {
        prettyMapper.writeValue(os, obj);
    }

    public static String writeValueAsPrettyString(Object obj) throws IOException {
        return prettyMapper.writeValueAsString(obj);
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

    public static <T> T readValue(InputStream bytes, TypeReference<T> type) throws IOException {
        return mapper.readValue(bytes, type);
    }

    public static <T> T readValue(InputStream bytes, Class<T> type, boolean replaceSystemProperties) throws IOException {
        if (replaceSystemProperties) {
            return sysPropertiesAwareMapper.readValue(bytes, type);
        } else {
            return mapper.readValue(bytes, type);
        }
    }

    /**
     * Creates an {@link ObjectNode} based on the given {@code pojo}, copying all its properties to the resulting {@link ObjectNode}.
     *
     * @param pojo a pojo which properties will be populates into the resulting a {@link ObjectNode}
     * @return a {@link ObjectNode} with all the properties from the given pojo
     * @throws IOException if the resulting a {@link ObjectNode} can not be created
     */
    public static ObjectNode createObjectNode(Object pojo) throws IOException {
        if (pojo == null) {
            throw new IllegalArgumentException("Pojo can not be null.");
        }

        ObjectNode objectNode = createObjectNode();
        JsonParser jsonParser = mapper.getJsonFactory().createJsonParser(writeValueAsBytes(pojo));
        JsonNode jsonNode = jsonParser.readValueAsTree();

        if (!jsonNode.isObject()) {
            throw new RuntimeException("JsonNode [" + jsonNode + "] is not a object.");
        }

        objectNode.putAll((ObjectNode) jsonNode);

        return objectNode;
    }

    public static ObjectNode createObjectNode() {
        return mapper.createObjectNode();
    }

}

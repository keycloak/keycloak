/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        prettyMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        prettyMapper.enable(SerializationFeature.INDENT_OUTPUT);
        prettyMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
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

    public static <T> T readValue(String string, TypeReference<T> type) throws IOException {
        return mapper.readValue(string, type);
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
        JsonParser jsonParser = mapper.getFactory().createParser(writeValueAsBytes(pojo));
        JsonNode jsonNode = jsonParser.readValueAsTree();

        if (!jsonNode.isObject()) {
            throw new RuntimeException("JsonNode [" + jsonNode + "] is not a object.");
        }

        objectNode.setAll((ObjectNode) jsonNode);

        return objectNode;
    }

    public static ObjectNode createObjectNode() {
        return mapper.createObjectNode();
    }

}

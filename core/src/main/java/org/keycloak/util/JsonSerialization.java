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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Utility class to handle simple JSON serializable for Keycloak.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JsonSerialization {
    /**
     * The mapper with the Keycloak defaults: no null values are written, empty beans are allowed and the
     * JDK 8 and Java time modules are registered.
     * <p>
     * Shared and must not be reconfigured.
     */
    public static final ObjectMapper mapper = createObjectMapperWithDefaults();

    /**
     * A mapper with indented output that, like {@link #mapper}, does not write null values and allows empty beans.
     * Unlike {@link #mapper} it does not register the JDK 8 and Java time modules.
     * <p>
     * Shared and must not be reconfigured.
     */
    public static final ObjectMapper prettyMapper = createPrettyObjectMapperWithDefaults();

    /**
     * An {@link ObjectMapper} with the plain Jackson defaults, and no Keycloak specific configuration or modules.
     * <p>
     * Use this instead of creating a new {@code ObjectMapper} when the exact Jackson default behavior is required,
     * for example when the serialized form is hashed or signed, or when {@link #mapper} would change the result by
     * omitting null values. Prefer {@link #mapper} otherwise.
     * <p>
     * Shared and must not be reconfigured, as that would affect every other user. To derive a variation use
     * {@link ObjectMapper#writer(com.fasterxml.jackson.core.PrettyPrinter)}, {@link ObjectMapper#readerFor(Class)},
     * {@link ObjectMapper#writer(SerializationFeature)} or {@link ObjectMapper#copy()}. If a different configuration
     * is needed, create a dedicated mapper instead.
     */
    public static final ObjectMapper plainMapper = new ObjectMapper();

    public static ObjectMapper createObjectMapperWithDefaults() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }

    public static ObjectMapper createPrettyObjectMapperWithDefaults() {
        ObjectMapper prettyMapper = new ObjectMapper();
        prettyMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        prettyMapper.enable(SerializationFeature.INDENT_OUTPUT);
        prettyMapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
        return prettyMapper;
    }

    public static String valueAsString(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static String valueAsPrettyString(Object obj) {
        try {
            return prettyMapper.writeValueAsString(obj);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static <T> T valueFromString(String string, Class<T> type) {
        try {
            return mapper.readValue(string, type);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
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

    public static JsonNode writeValueAsNode(Object obj) {
        return mapper.valueToTree(obj);
    }

    public static <T> T readValue(byte[] bytes, Class<T> type) throws IOException {
        return mapper.readValue(bytes, type);
    }

    public static <T> T readValue(String bytes, Class<T> type) throws IOException {
        return mapper.readValue(bytes, type);
    }

    public static <T> T readValue(InputStream bytes, Class<T> type) throws IOException {
        return mapper.readValue(bytes, type);
    }

    public static <T> T readValue(byte[] bytes, TypeReference<T> type) throws IOException {
        return mapper.readValue(bytes, type);
    }

    public static <T> T readValue(String string, TypeReference<T> type) throws IOException {
        return mapper.readValue(string, type);
    }

    public static <T> T readValue(InputStream bytes, TypeReference<T> type) throws IOException {
        return mapper.readValue(bytes, type);
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
        JsonNode jsonNode;
        try (JsonParser jsonParser = mapper.getFactory().createParser(writeValueAsBytes(pojo))) {
            jsonNode = jsonParser.readValueAsTree();
        }

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

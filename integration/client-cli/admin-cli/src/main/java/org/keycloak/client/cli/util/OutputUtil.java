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
package org.keycloak.client.cli.util;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class OutputUtil {

    public static ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
        MAPPER.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public static JsonNode convertToJsonNode(Object object) throws IOException {
        if (object instanceof JsonNode) {
            return (JsonNode) object;
        }

        return MAPPER.convertValue(object, JsonNode.class);
    }

    public static void printAsCsv(Object object, ReturnFields fields, boolean unquoted) throws IOException {
        printAsCsv(object, fields, unquoted, IoUtil::printOut);
    }

    public static void printAsCsv(Object object, ReturnFields fields, boolean unquoted, Consumer<String> printer) throws IOException {

        JsonNode node = convertToJsonNode(object);
        if (!node.isArray()) {
            ArrayNode listNode = MAPPER.createArrayNode();
            listNode.add(node);
            node = listNode;
        }

        for (JsonNode item: node) {
            StringBuilder buffer = new StringBuilder();
            printObjectAsCsv(buffer, item, fields, unquoted);

            printer.accept(buffer.length() > 0 ? buffer.substring(1) : "");
        }
    }

    static void printObjectAsCsv(StringBuilder out, JsonNode node, boolean unquoted) {
        printObjectAsCsv(out, node, null, unquoted);
    }

    static void printObjectAsCsv(StringBuilder out, JsonNode node, ReturnFields fields, boolean unquoted) {

        if (node == null) {
            out.append(",");
        } else if (node.isObject()) {
            if (fields == null) {
                Iterator<Map.Entry<String, JsonNode>> it = node.fields();
                while (it.hasNext()) {
                    printObjectAsCsv(out, it.next().getValue(), unquoted);
                }
            } else {
                Iterator<String> it = fields.iterator();
                while (it.hasNext()) {
                    String field = it.next();
                    JsonNode attr = node.get(field);
                    printObjectAsCsv(out, attr, fields.child(field), unquoted);
                }
            }
        } else if (node.isArray()) {
            for (JsonNode item: node) {
                printObjectAsCsv(out, item, fields, unquoted);
            }
        } else {
            out.append(",");
            if (unquoted && node instanceof TextNode) {
                out.append(node.asText());
            } else {
                out.append(node.toString());
            }
        }
    }
}

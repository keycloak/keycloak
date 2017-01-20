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
package org.keycloak.client.admin.cli.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.keycloak.client.admin.cli.common.AttributeKey;
import org.keycloak.client.admin.cli.common.AttributeOperation;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.keycloak.client.admin.cli.common.AttributeOperation.Type.SET;
import static org.keycloak.client.admin.cli.util.OutputUtil.MAPPER;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class ReflectionUtil {

    public static void setAttributes(JsonNode client, List<AttributeOperation> attrs) {
        for (AttributeOperation item: attrs) {
            AttributeKey attr = item.getKey();
            JsonNode nested = client;

            List<AttributeKey.Component> cs = attr.getComponents();
            for (int i = 0; i < cs.size(); i++) {
                AttributeKey.Component c = cs.get(i);

                // if this is the last component of the name,
                //    then if SET we need to set value on nested:
                //             if value already set on nested, then overwrite, maybe remove node + add new node
                //         if DELETE we need to remove or nullify value (if isArray)
                // else get child and
                //    if exist set nested to child
                //    else if SET create new empty object or array - depending on c.isArray()
                //

                // if this is the last component of the name
                if (i == cs.size() - 1) {
                    String val = item.getValue();
                    ObjectNode obj = (ObjectNode) nested;

                    if (SET == item.getType()) {
                        JsonNode valNode = valueToJsonNode(val);
                        if (c.isArray() || attr.isAppend()) {
                            JsonNode list = obj.get(c.getName());
                            // child expected to be an array
                            if ( ! (list instanceof ArrayNode)) {
                                // replace with new array
                                list = MAPPER.createArrayNode();
                                obj.set(c.getName(), list);
                            }
                            setArrayItem((ArrayNode) list, c.getIndex(), valNode);
                        } else {
                            ((ObjectNode) nested).set(c.getName(), valNode);
                        }
                    } else {
                        // type == DELETE
                        if (c.isArray()) {
                            JsonNode list = obj.get(c.getName());
                            // child expected to be an array
                            if (list instanceof ArrayNode) {
                                removeArrayItem((ArrayNode) list, c.getIndex());
                            }
                        } else {
                            obj.remove(c.getName());
                        }
                    }
                } else {
                    // get child and
                    //    if exist set nested to child
                    //    else create new empty object or array - depending on c.isArray()
                    JsonNode node = nested.get(c.getName());
                    if (node == null) {
                        if (c.isArray()) {
                            node = MAPPER.createArrayNode();
                        } else {
                            node = MAPPER.createObjectNode();
                        }
                        ((ObjectNode) nested).set(c.getName(), node);
                    }
                    nested = node;
                }
            }
        }
    }

    private static void setArrayItem(ArrayNode list, int index, JsonNode valNode) {
        if (index == -1) {
            // append to end of array
            list.add(valNode);
            return;
        }
        // make sure items up to index exist
        for (int i = list.size(); i < index+1; i++) {
            list.add(NullNode.instance);
        }
        list.set(index, valNode);
    }

    private static void removeArrayItem(ArrayNode list, int index) {
        if (index == -1) {
            throw new IllegalArgumentException("Internal error - should never be called with index == -1");
        }
        list.remove(index);
    }

    private static JsonNode valueToJsonNode(String val) {
        // try get value as JSON object
        try {
            return MAPPER.readValue(val, ObjectNode.class);
        } catch (Exception ignored) {
        }

        // try get value as JSON array
        try {
            return MAPPER.readValue(val, ArrayNode.class);
        } catch (Exception ignored) {
        }

        if (isBoolean(val)) {
            return BooleanNode.valueOf(Boolean.valueOf(val));
        } else if (isInteger(val)) {
            return LongNode.valueOf(Long.valueOf(val));
        } else if (isNumber(val)) {
            return DoubleNode.valueOf(Double.valueOf(val));
        } else if (isQuoted(val)) {
            return TextNode.valueOf(unquote(val));
        }

        return TextNode.valueOf(val);
    }

    private static boolean isInteger(String val) {
        try {
            Long.valueOf(val);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean isNumber(String val) {
        try {
            Double.valueOf(val);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean isBoolean(String val) {
        return "false".equals(val) || "true".equals(val);
    }

    private static boolean isQuoted(String val) {
        return val.startsWith("'") || val.startsWith("\"");
    }

    private static String unquote(String val) {
        if (!(val.startsWith("'") || val.startsWith("\"")) || !(val.endsWith("'") || val.endsWith("\""))) {
            throw new RuntimeException("Invalid string value: " + val);
        }
        return val.substring(1, val.length()-1);
    }

    public static void merge(JsonNode source, ObjectNode dest) {
        // Iterate over source
        // For each child check if exists on the destination
        // if it does go deep
        // otherwise copy over
        // if it's last component, set it on destination

        if (!source.isObject()) {
            throw new RuntimeException("Not a JSON object: " + source);
        }

        Iterator<Map.Entry<String, JsonNode>> it = ((ObjectNode) source).fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> item = it.next();
            String name = item.getKey();
            JsonNode node = item.getValue();

            JsonNode destNode = dest.get(name);
            if (destNode != null) {
                if (destNode.isObject()) {
                    if (node.isObject()) {
                        merge(node, (ObjectNode) destNode);
                    } else {
                        throw new RuntimeException("Attribute is of incompatible type - " + name + ": " + node);
                    }
                } else if (destNode.isArray()) {
                    if (node.isArray()) {
                        dest.set(name, node);
                    } else {
                        throw new RuntimeException("Attribute is of incompatible type - " + name + ": " + node);
                    }
                } else {
                    dest.set(name, node);
                }
            } else {
                dest.set(name, node);
            }
        }
    }
}

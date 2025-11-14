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

package org.keycloak.json;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class StringListMapDeserializer extends JsonDeserializer<Object> {

    @Override
    public Object deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode jsonNode = jsonParser.readValueAsTree();
        Iterator<Map.Entry<String, JsonNode>> itr = jsonNode.fields();
        Map<String, List<String>> map = new HashMap<>();
        while (itr.hasNext()) {
            Map.Entry<String, JsonNode> e = itr.next();
            List<String> values = new LinkedList<>();
            if (!e.getValue().isArray()) {
                values.add((e.getValue().isNull()) ? null : e.getValue().asText());
            } else {
                ArrayNode a = (ArrayNode) e.getValue();
                Iterator<JsonNode> vitr = a.elements();
                while (vitr.hasNext()) {
                    JsonNode node = vitr.next();
                    values.add((node.isNull() ? null : node.asText()));
                }
            }
            map.put(e.getKey(), values);
        }
        return map;
    }

}

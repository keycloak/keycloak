/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.keycloak.common.util.ObjectUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * A {@link JsonDeserializer} that can deserialize either a {@link String} of
 * space-separated scopes or an array of "whitespace-free" {@link String}s
 * which represent each scope.
 *
 * This is required because some identity providers may utilize an array of
 * scopes in their {@link org.keycloak.representations.AccessTokenResponse} as
 * opposed to a {@link String} of space-separated scopes.
 *
 * Invariant: When deserializing an array of scopes, each element must not
 * contain any whitespace.
 */
public class AccessTokenResponseScopeDeserializer extends JsonDeserializer<Object> {

    @Override
    public Object deserialize(
        JsonParser jsonParser,
        DeserializationContext deserializationContext
    ) throws IOException {
        JsonNode jsonNode = jsonParser.readValueAsTree();
        if (jsonNode.isArray()) {
            ArrayList<String> a = new ArrayList<>(1);
            for (JsonNode node : jsonNode) {
                String nodeTextValue = node.textValue();
                if (ObjectUtil.isBlank(nodeTextValue)) {
                    throw deserializationContext.weirdStringException(
                        nodeTextValue,
                        String.class,
                        "Scope string contains whitespace."
                    );
                }
                a.add(nodeTextValue);
            }
            return a.stream().collect(Collectors.joining(" "));
        } else {
            return jsonNode.textValue();
        }
    }

}

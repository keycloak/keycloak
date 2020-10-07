/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.adapters.authorization.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class JsonUtils {

    public static List<String> getValues(JsonNode jsonNode, String path) {
        return getValues(jsonNode.at(path));
    }

    public static List<String> getValues(JsonNode jsonNode) {
        List<String> values = new ArrayList<>();

        if (jsonNode.isArray()) {

            for (JsonNode node : jsonNode) {
                String value;

                if (node.isObject()) {
                    try {
                        value = JsonSerialization.writeValueAsString(node);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    value = node.asText();
                }

                if (value != null) {
                    values.add(value);
                }
            }
        } else {
            String value = jsonNode.asText();

            if (value != null) {
                values.add(value);
            }
        }

        return values;
    }

}

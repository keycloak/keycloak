/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.utils;

import java.io.IOException;
import java.util.Map;

import org.keycloak.utils.JsonConfigProvider.JsonScope;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;

public class ScopeUtil {

    public static JsonScope createScope(Map<String, String> properties) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode config = mapper.readTree(json(properties));
            return new JsonConfigProvider(config).new JsonScope(config);
        } catch (IOException e) {
            Assert.fail("Could not parse json");
        }
        return null;
    }

    static String json(Map<String, String> properties) {
        String[] params = properties.entrySet().stream().map(e -> param(e.getKey(), e.getValue())).toArray(String[]::new);

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append(String.join(",", params));
        sb.append("}");

        return sb.toString();
    }

    static String param(String key, String value) {
        return "\"" + key + "\"" + " : " + "\"" + value + "\"";
    }

}

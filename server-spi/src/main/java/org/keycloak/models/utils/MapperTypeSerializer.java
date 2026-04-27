/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.models.utils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.type.TypeReference;

import static java.util.Collections.emptyMap;

/**
 * Serializer and deserializer for {@link org.keycloak.provider.ProviderConfigProperty#MAP_TYPE}
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MapperTypeSerializer {

    private static final TypeReference<List<StringPair>> MAP_TYPE_REPRESENTATION = new TypeReference<>() {
    };

    public static Map<String, List<String>> deserialize(String configString) {
        if (configString == null) {
            return emptyMap();
        }

        try {
            List<StringPair> map = JsonSerialization.readValue(configString, MAP_TYPE_REPRESENTATION);
            return map.stream().collect(
                    Collectors.collectingAndThen(
                            Collectors.groupingBy(StringPair::getKey,
                                    Collectors.mapping(StringPair::getValue, Collectors.toUnmodifiableList())),
                            Collections::unmodifiableMap));
        } catch (IOException e) {
            throw new RuntimeException("Could not deserialize json: " + configString, e);
        }
    }

    public static String serialize(Map<String, List<String>> config) {
        List<StringPair> pairs = config.entrySet()
                .stream()
                .flatMap(entry -> {
                    String key = entry.getKey();
                    List<String> values = entry.getValue();
                    return values
                            .stream()
                            .map(value -> new StringPair(key, value));
                })
                .toList();
        try {
            return JsonSerialization.writeValueAsString(pairs);
        } catch (IOException e) {
            throw new RuntimeException("Could not serialize json: " + config, e);
        }
    }

    static class StringPair {
        private String key;
        private String value;

        public StringPair() {
        }

        private StringPair(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}

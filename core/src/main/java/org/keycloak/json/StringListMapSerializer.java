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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class StringListMapSerializer extends JsonSerializer<Object> {
    @Override
    public void serialize(Object o, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();

        Map<String, List<String>> values = (Map<String, List<String>>) o;

        for (Map.Entry<String, List<String>> e : values.entrySet()) {
            if (!e.getValue().isEmpty()) {
                if (e.getValue().size() == 1) {
                    jsonGenerator.writeStringField(e.getKey(), e.getValue().get(0));
                } else {
                    jsonGenerator.writeArrayFieldStart(e.getKey());
                    for (String v : e.getValue()) {
                        jsonGenerator.writeString(v);
                    }
                    jsonGenerator.writeEndArray();
                }
            }
        }

        jsonGenerator.writeEndObject();
    }
}

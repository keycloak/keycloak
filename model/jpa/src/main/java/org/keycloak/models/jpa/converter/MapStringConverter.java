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

package org.keycloak.models.jpa.converter;

import java.io.IOException;
import java.util.Map;
import javax.persistence.AttributeConverter;
import org.jboss.logging.Logger;
import org.keycloak.util.JsonSerialization;

public class MapStringConverter implements AttributeConverter<Map<String, String>, String> {
    private static final Logger logger = Logger.getLogger(MapStringConverter.class);

    @Override
    public String convertToDatabaseColumn(Map<String, String> attribute) {
        try {
            return JsonSerialization.writeValueAsString(attribute);
        } catch (IOException e) {
            logger.error("Error while converting Map to JSON String: ", e);
            return null;
        }
    }

    @Override
    public Map<String, String> convertToEntityAttribute(String dbData) {
        try {
            return JsonSerialization.readValue(dbData, Map.class);
        } catch (IOException e) {
            logger.error("Error while converting JSON String to Map: ", e);
            return null;
        }
    }
}

/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.storage.attributes.mappers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.component.ComponentModelScope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.Map;

/**
 * @author Ben Cresitello-Dittmar
 *
 * Attribute mapper to map attributes from an external attribute store to attributes on a user
 */
public class UserAttributeMapper extends AbstractAttributeMapper {

    public UserAttributeMapper(ComponentModelScope config) {
        super(config);
    }

    @Override
    public void transform(KeycloakSession session, Map<String, Object> source, Map<String, String> dest) {
        String destKey = getDestAttributeKey();
        String value = getAttributeValue(source);
        dest.put(destKey, value);
    }

    /**
     * Helper function to get the configured value from the given source attributes
     * @param source the attribute received from the external attribute store
     * @return the attribute value to set on the user
     */
    private String getAttributeValue(Map<String, Object> source){
        String pointerExpr = config.get(UserAttributeMapperFactory.CONFIG_ATTRIBUTE_JSON_POINTER);
        JsonNode parsed = new ObjectMapper().convertValue(source, JsonNode.class);

        // extract the value from the source attributes based on the JSON pointer expression
        try {
            JsonNode extracted = parsed.at(pointerExpr);

            // return as string unless it is a nested type (ie array or object). prevents extraneous quote character for string values
            if (extracted.isValueNode()){
                return extracted.asText();
            } else {
                return JsonSerialization.writeValueAsString(extracted);
            }
        } catch (IOException e){
            // don't hard fail so other mappers can still execute
            logger.warnf("failed to execute mapper: %s", e);
        }

        return null;
    }

    private String getDestAttributeKey(){
        return config.get(UserAttributeMapperFactory.CONFIG_ATTRIBUTE_DEST);
    }
}

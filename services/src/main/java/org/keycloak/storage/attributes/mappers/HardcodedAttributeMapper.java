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

import org.keycloak.component.ComponentModelScope;
import org.keycloak.models.KeycloakSession;

import java.util.Map;

/**
 * @author Ben Cresitello-Dittmar
 *
 * Attribute mapper to specify hardcoded attribute values. This mapper ignores the attribute received from the attribute
 * store and sets a hardcoded value on the return attributes
 */
public class HardcodedAttributeMapper extends AbstractAttributeMapper {

    public HardcodedAttributeMapper(ComponentModelScope config){
        super(config);
    }

    @Override
    public void transform(KeycloakSession session, Map<String, Object> source, Map<String, String> dest) {
        String attrName = getAttributeName();
        String attrValue = getAttributeValue();

        dest.put(attrName, attrValue);
    }

    private String getAttributeName(){
        return config.get(HardcodedAttributeMapperFactory.CONFIG_ATTRIBUTE_NAME);
    }

    private String getAttributeValue(){
        return config.get(HardcodedAttributeMapperFactory.CONFIG_ATTRIBUTE_VALUE);
    }

}

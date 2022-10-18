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
package org.keycloak.storage.attributes.mappers;

import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentModelScope;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Ben Cresitello-Dittmar
 *
 * Factory for creating instances of {@link UserAttributeMapper}
 */
public class UserAttributeMapperFactory extends AbstractAttributeMapperFactory<UserAttributeMapper> {

    /**
     * The JSON pointer expression used to extract the attribute value from the attributes received from the external store
     */
    public static String CONFIG_ATTRIBUTE_JSON_POINTER = "attribute.pointer";

    /**
     * The name of the attribute to set on the user
     */
    public static String CONFIG_ATTRIBUTE_DEST = "attribute.dest";

    protected UserAttributeMapper createMapper(ComponentModelScope config){
        return new UserAttributeMapper(config);
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel model) throws ComponentValidationException {
        List<String> errors = new ArrayList<>();

        // ensure all config attributes are set
        Arrays.asList(
                CONFIG_ATTRIBUTE_DEST
        ).forEach(config -> { if (!isSet(config, model)) { errors.add(String.format("Attribute %s is not set", config)); } });

        if (!errors.isEmpty()) {
            throw new ComponentValidationException(String.format("Config validation failed when creating UserAttributeMapper. %s", String.join(". ", errors)));
        }
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(){
        return ProviderConfigurationBuilder.create()
                .property().name(CONFIG_ATTRIBUTE_JSON_POINTER)
                .label("JSON Pointer Expression")
                .type(ProviderConfigProperty.STRING_TYPE)
                .helpText("The JSON pointer expression used to extract attributes from the attribute store response (https://datatracker.ietf.org/doc/html/rfc6901)")
                .add()
                .property().name(CONFIG_ATTRIBUTE_DEST)
                .label("Destination Attribute Key")
                .type(ProviderConfigProperty.STRING_TYPE)
                .helpText("Name of the attribute to set on the user")
                .required(true)
                .add()
                .build();
    }

    @Override
    public String getId() {
        return "user-attribute-mapper";
    }
}

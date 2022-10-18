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

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentModelScope;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Ben Cresitello-Dittmar
 *
 * Factory for creating instances of {@link ScriptAttributeMapper}
 */
public class ScriptAttributeMapperFactory extends AbstractAttributeMapperFactory<ScriptAttributeMapper> implements EnvironmentDependentProviderFactory {

    /**
     * The key of the attribute to set on the user
     */
    public static String CONFIG_ATTRIBUTE_DEST = "attribute.dest";
    /**
     * The script source used to process the attributes from the external store
     */
    public static String CONFIG_SCRIPT_SOURCE = "script.source";

    public static String SCRIPT_HELP_TEXT = (
            "Script to compute the attribute value. \n" +
                    " Available variables: \n" +
                    " 'keycloakSession' - the current keycloakSession.\n\n" +
                    " 'realm' - the current realm.\n" +
                    " 'sourceAttributes' - the map of attributes received from the attribute query\n" +

                    "To use: the last statement is the value returned to Java as the attribute value. It should be a string.\n"
    );

    public static String SCRIPT_DEFAULT_VALUE = ("/**\n" +
            " * Available variables: \n" +
            " * keycloakSession - the current keycloakSession\n" +
            " * realm - the current realm\n" +
            " * sourceAttributes - the map of attributes received from the attribute query\n" +
            " */\n\n\n//insert your code here..."
    );

    protected ScriptAttributeMapper createMapper(ComponentModelScope config){
        return new ScriptAttributeMapper(config);
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel model) throws ComponentValidationException {
        List<String> errors = new ArrayList<>();

        Arrays.asList(
                CONFIG_ATTRIBUTE_DEST,
                CONFIG_SCRIPT_SOURCE
        ).forEach(config -> { if (!isSet(config, model)) { errors.add(String.format("Attribute %s is not set", config)); } });

        if (!errors.isEmpty()) {
            throw new ComponentValidationException(String.format("Config validation failed when creating ScriptAttributeMapper. %s", String.join(". ", errors)));
        }
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(){
        return ProviderConfigurationBuilder.create()
                .property().name(CONFIG_ATTRIBUTE_DEST)
                .label("Attribute Destination Key")
                .helpText("Name of the attribute to set on the user")
                .type(ProviderConfigProperty.STRING_TYPE)
                .required(true)
                .add()
                .property().name(CONFIG_SCRIPT_SOURCE)
                .label("Script Source Code")
                .type(ProviderConfigProperty.SCRIPT_TYPE)
                .defaultValue(SCRIPT_DEFAULT_VALUE)
                .helpText(SCRIPT_HELP_TEXT)
                .required(true)
                .add()
                .build();
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return super.isSupported(config) && Profile.isFeatureEnabled(Profile.Feature.SCRIPTS);
    }

    @Override
    public String getId() {
        return "script-attribute-mapper";
    }
}

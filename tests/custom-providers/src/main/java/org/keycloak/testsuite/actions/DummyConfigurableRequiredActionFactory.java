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

package org.keycloak.testsuite.actions;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RequiredActionConfigModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

public class DummyConfigurableRequiredActionFactory implements RequiredActionFactory {

    public static final String PROVIDER_ID = "configurable-test-action";

    public static final String SETTING_1 = "setting1";

    public static final String SETTING_2 = "setting2";

    @Override
    public String getDisplayText() {
        return "Configurable Test Action";
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return new RequiredActionProvider() {
            @Override
            public void evaluateTriggers(RequiredActionContext context) {

            }

            @Override
            public void requiredActionChallenge(RequiredActionContext context) {

                // users can access the given Required Action configuration via RequiredActionContext#getContext()
                RequiredActionConfigModel configModel = context.getConfig();
                Map<String, String> config = configModel.getConfig();

                String setting1Value = configModel.getConfigValue(SETTING_1);

                context.success();
            }

            @Override
            public void processAction(RequiredActionContext context) {
            }

            @Override
            public void close() {

            }
        };
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = ProviderConfigurationBuilder.create() //
            .property() //
            .name(SETTING_1) //
            .label("Setting 1") //
            .helpText("Setting 1 Help Text") //
            .type(ProviderConfigProperty.STRING_TYPE) //
            .defaultValue("setting1Default") //
            .add() //

            .property() //
            .name(SETTING_2) //
            .label("Setting 2") //
            .helpText("Setting 2 Help Text") //
            .type(ProviderConfigProperty.BOOLEAN_TYPE) //
            .defaultValue("true") //
            .add() //

            .build();

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return Stream.concat(
                List.copyOf(CONFIG_PROPERTIES).stream(),
                List.copyOf(RequiredActionFactory.super.getConfigMetadata()).stream()
        ).toList();
    }
}

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

package org.keycloak.testsuite.components;

import java.util.List;

import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ConfigurationValidationHelper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import static org.keycloak.provider.ProviderConfigProperty.STRING_TYPE;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class TestComponentImplProviderFactory implements TestComponentProviderFactory {

    private List<ProviderConfigProperty> config = ProviderConfigurationBuilder.create()
            .property("secret", "Secret", "A secret value", STRING_TYPE, null, null, true)
            .property("number", "Number", "A number value", STRING_TYPE, null, null, false)
            .property("required", "Required", "A required value", STRING_TYPE, null, null, false)
            .property("val1", "Value 1", "Some more values", STRING_TYPE, null, null, false)
            .property("val2", "Value 2", "Some more values", STRING_TYPE, null, null, false)
            .property("val3", "Value 3", "Some more values", STRING_TYPE, null, null, false)
            .build();

    @Override
    public Object create(KeycloakSession session, ComponentModel model) {
        return new TestComponentImplProvider(model);
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel model) throws ComponentValidationException {
        ConfigurationValidationHelper.check(model)
                .checkRequired("required", "Required")
                .checkInt("number", "Number", false);
    }

    @Override
    public String getHelpText() {
        return "Provider to test component storage";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return config;
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
        return "test-component";
    }

    public static class TestComponentImplProvider implements TestComponentProvider {

        private ComponentModel model;

        public TestComponentImplProvider(ComponentModel model) {
            this.model = model;
        }

        @Override
        public DetailsRepresentation getDetails() {
            DetailsRepresentation rep = new DetailsRepresentation();
            rep.setConfig(model.getConfig());
            return rep;
        }

        @Override
        public void close() {
        }

    }

}

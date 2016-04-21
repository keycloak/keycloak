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

package org.keycloak.testsuite.federation;

import java.util.Arrays;
import java.util.List;

import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DummyConfigurableUserFederationProviderFactory extends DummyUserFederationProviderFactory implements ConfiguredProvider {

    public static final String PROVIDER_NAME = "dummy-configurable";

    @Override
    public String getId() {
        return PROVIDER_NAME;
    }

    @Override
    public String getHelpText() {
        return "Dummy User Federation Provider Help Text";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {

        ProviderConfigProperty prop1 = new ProviderConfigProperty();
        prop1.setName("prop1");
        prop1.setLabel("Prop1");
        prop1.setDefaultValue("prop1Default");
        prop1.setHelpText("Prop1 HelpText");
        prop1.setType(ProviderConfigProperty.STRING_TYPE);

        ProviderConfigProperty prop2 = new ProviderConfigProperty();
        prop2.setName("prop2");
        prop2.setLabel("Prop2");
        prop2.setDefaultValue("true");
        prop2.setHelpText("Prop2 HelpText");
        prop2.setType(ProviderConfigProperty.BOOLEAN_TYPE);

        return Arrays.asList(prop1, prop2);
    }
}

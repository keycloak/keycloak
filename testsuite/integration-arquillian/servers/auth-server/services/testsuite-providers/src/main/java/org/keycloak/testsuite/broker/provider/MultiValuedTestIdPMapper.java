/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.broker.provider;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.broker.provider.AbstractIdentityProviderMapper;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * Testing IdP mapper with multivalued property
 *
 * @author Martin Bartos <mabartos@redhat.com>
 */
public class MultiValuedTestIdPMapper extends AbstractIdentityProviderMapper {
    public static final String[] COMPATIBLE_PROVIDERS = {ANY_PROVIDER};

    public static final String PROVIDER_ID = "multi-valued-test-idp-mapper";
    public static final String VALUES_ATTRIBUTE = "values";

    protected static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(VALUES_ATTRIBUTE);
        property.setLabel("Test values");
        property.setHelpText("Define test values");
        property.setType(ProviderConfigProperty.MULTIVALUED_STRING_TYPE);
        configProperties.add(property);
    }

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
    }

    @Override
    public String getDisplayCategory() {
        return "Test IdP Mapper";
    }

    @Override
    public String getDisplayType() {
        return "Test MultiValued Mapper";
    }

    @Override
    public String getHelpText() {
        return "This is testing IdP mapper with multivalued property";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}

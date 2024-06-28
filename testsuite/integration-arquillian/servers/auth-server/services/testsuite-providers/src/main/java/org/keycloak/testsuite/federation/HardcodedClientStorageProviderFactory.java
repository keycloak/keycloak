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

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.client.ClientStorageProviderFactory;
import org.keycloak.storage.client.ClientStorageProviderModel;

import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class HardcodedClientStorageProviderFactory implements ClientStorageProviderFactory<HardcodedClientStorageProvider> {
    @Override
    public HardcodedClientStorageProvider create(KeycloakSession session, ComponentModel model) {
        return new HardcodedClientStorageProvider(session, new ClientStorageProviderModel(model));
    }


    public static final String PROVIDER_ID = "hardcoded-client";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    protected static final List<ProviderConfigProperty> CONFIG_PROPERTIES;

    public static final String CLIENT_ID = "client_id";

    public static final String REDIRECT_URI = "redirect_uri";
    public static final String CONSENT = "consent";
    public static final String DELAYED_SEARCH = "delayed_search";

    static {
        CONFIG_PROPERTIES = ProviderConfigurationBuilder.create()
                .property().name(CLIENT_ID)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Hardcoded Client Id")
                .helpText("Only this client id is available for lookup")
                .defaultValue("hardcoded-client")
                .add()
                .property().name(REDIRECT_URI)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Redirect Uri")
                .helpText("Valid redirect uri.  Only one allowed")
                .defaultValue("http://localhost:8180/*")
                .add()
                .property().name(CONSENT)
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .label("Consent Required")
                .helpText("Is consent required")
                .defaultValue("false")
                .add()
                .property().name(DELAYED_SEARCH)
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .label("Delayes provider by 5s.")
                .helpText("If true it delayes search for clients within the provider by 5s.")
                .defaultValue(false)
                .add()
                .build();
    }


    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }
}

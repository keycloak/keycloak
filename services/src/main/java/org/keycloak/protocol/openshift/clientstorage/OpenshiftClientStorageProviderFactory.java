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
package org.keycloak.protocol.openshift.clientstorage;

import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.CacheableStorageProviderModel;
import org.keycloak.storage.client.ClientStorageProviderFactory;
import org.keycloak.storage.client.ClientStorageProviderModel;

import java.util.List;

import static org.keycloak.storage.CacheableStorageProviderModel.CACHE_POLICY;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OpenshiftClientStorageProviderFactory implements ClientStorageProviderFactory<OpenshiftClientStorageProvider> {
    @Override
    public OpenshiftClientStorageProvider create(KeycloakSession session, ComponentModel model) {
        return new OpenshiftClientStorageProvider(session, new ClientStorageProviderModel(model));
    }


    public static final String PROVIDER_ID = "openshift-oauth-clients";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "Openshift OAuth Client Adapter";
    }

    protected static final List<ProviderConfigProperty> CONFIG_PROPERTIES;

    public static final String ACCESS_TOKEN = "access_token";
    public static final String OPENSHIFT_URI = "openshift_uri";

    static {
        CONFIG_PROPERTIES = ProviderConfigurationBuilder.create()
                .property().name(ACCESS_TOKEN)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Access Token")
                .helpText("Bearer token that will be used to invoke on Openshift api server.  Must have privilege to lookup oauth clients, service accounts, and invoke on token review interface")
                .add()
                .property().name(OPENSHIFT_URI)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Openshift URL")
                .helpText("Openshift api server URL base endpoint.")
                .add()
                .build();
    }


    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config) throws ComponentValidationException {
        config.getConfig().putSingle(CACHE_POLICY, CacheableStorageProviderModel.CachePolicy.NO_CACHE.name());
    }
}

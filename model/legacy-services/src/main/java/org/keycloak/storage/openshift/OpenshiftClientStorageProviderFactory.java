/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.storage.openshift;

import static org.keycloak.storage.CacheableStorageProviderModel.CACHE_POLICY;

import java.util.List;
import java.util.regex.Pattern;

import com.openshift.restclient.ClientBuilder;
import com.openshift.restclient.IClient;
import org.keycloak.common.Profile;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.CacheableStorageProviderModel;
import org.keycloak.storage.client.ClientStorageProviderFactory;
import org.keycloak.storage.client.ClientStorageProviderModel;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class OpenshiftClientStorageProviderFactory implements ClientStorageProviderFactory<OpenshiftClientStorageProvider>, EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "openshift-oauth-client";
    static final Pattern SERVICE_ACCOUNT_PATTERN = Pattern.compile("system:serviceaccount:([^:]+):([^:]+)");
    public static final String CONFIG_PROPERTY_ACCESS_TOKEN = "openshift.access_token";
    public static final String CONFIG_PROPERTY_OPENSHIFT_URI = "openshift.uri";
    public static final String CONFIG_PROPERTY_DEFAULT_NAMESPACE = "openshift.namespace.default";
    public static final String CONFIG_PROPERTY_REQUIRE_USER_CONSENT = "user.consent.require";
    public static final String CONFIG_PROPERTY_DISPLAY_SCOPE_CONSENT_TEXT= "user.consent.scope.consent.text";

    private final List<ProviderConfigProperty> CONFIG_PROPERTIES;
    private IClient client;

    public OpenshiftClientStorageProviderFactory() {
        CONFIG_PROPERTIES = ProviderConfigurationBuilder.create()
                .property().name(CONFIG_PROPERTY_ACCESS_TOKEN)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Access Token")
                .helpText("Bearer token that will be used to invoke on Openshift api server.  Must have privilege to lookup oauth clients, service accounts, and invoke on token review interface")
                .add()
                .property().name(CONFIG_PROPERTY_OPENSHIFT_URI)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Openshift URL")
                .helpText("Openshift api server URL base endpoint.")
                .add()
                .property().name(CONFIG_PROPERTY_DEFAULT_NAMESPACE)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Default Namespace")
                .helpText("The default namespace to use when the server is not able to resolve the namespace from the client identifier. Useful when clients in Openshift don't have names with the following pattern: " + SERVICE_ACCOUNT_PATTERN.pattern())
                .add()
                .property().name(CONFIG_PROPERTY_REQUIRE_USER_CONSENT)
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue("true")
                .label("Require User Consent")
                .helpText("If set to true, clients from this storage will ask the end-user for any scope requested during the authorization flow")
                .add()
                .property().name(CONFIG_PROPERTY_DISPLAY_SCOPE_CONSENT_TEXT)
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue("true")
                .label("Display Scopes Consent Text")
                .helpText("If set to true, the consent page will display texts from the message bundle for scopes. Otherwise, the scope name will be displayed.")
                .add()
                .build();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public OpenshiftClientStorageProvider create(KeycloakSession session, ComponentModel model) {
        ClientStorageProviderModel providerModel = createProviderModel(model);
        IClient client = getClient(providerModel);

        if (client != null) {
            return new OpenshiftClientStorageProvider(session, providerModel, client);
        }

        client.getAuthorizationContext().setToken(providerModel.get(OpenshiftClientStorageProviderFactory.CONFIG_PROPERTY_ACCESS_TOKEN));

        return new OpenshiftClientStorageProvider(session, providerModel, client);
    }

    @Override
    public String getHelpText() {
        return "Openshift OAuth Client Adapter";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config) throws ComponentValidationException {
        config.getConfig().putSingle(CACHE_POLICY, CacheableStorageProviderModel.CachePolicy.NO_CACHE.name());
    }

    @Override
    public void onUpdate(KeycloakSession session, RealmModel realm, ComponentModel oldModel, ComponentModel newModel) {
        if (!oldModel.get(CONFIG_PROPERTY_OPENSHIFT_URI).equals(newModel.get(CONFIG_PROPERTY_OPENSHIFT_URI))) {
            client = null;
        } else {
            getClient(createProviderModel(newModel)).getAuthorizationContext().setToken(newModel.get(OpenshiftClientStorageProviderFactory.CONFIG_PROPERTY_ACCESS_TOKEN));
        }
    }

    @Override
    public boolean isSupported() {
        return Profile.isFeatureEnabled(Profile.Feature.OPENSHIFT_INTEGRATION);
    }

    private IClient getClient(ClientStorageProviderModel providerModel) {
        synchronized (this) {
            if (client == null) {
                client = new ClientBuilder(providerModel.get(OpenshiftClientStorageProviderFactory.CONFIG_PROPERTY_OPENSHIFT_URI)).build();
            }
        }

        return client;
    }

    private ClientStorageProviderModel createProviderModel(ComponentModel model) {
        return new ClientStorageProviderModel(model);
    }
}

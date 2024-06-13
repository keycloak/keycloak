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

package org.keycloak.keys;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.crypto.Algorithm;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ConfigurationValidationHelper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static org.keycloak.provider.ProviderConfigProperty.LIST_TYPE;
import static org.keycloak.provider.ProviderConfigProperty.STRING_TYPE;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JavaKeystoreKeyProviderFactory implements KeyProviderFactory {
    private static final Logger logger = Logger.getLogger(JavaKeystoreKeyProviderFactory.class);

    public static final String ID = "java-keystore";

    public static String KEYSTORE_KEY = "keystore";
    public static ProviderConfigProperty KEYSTORE_PROPERTY = new ProviderConfigProperty(KEYSTORE_KEY, "Keystore", "Path to keys file", STRING_TYPE, null);

    public static String KEYSTORE_PASSWORD_KEY = "keystorePassword";
    public static ProviderConfigProperty KEYSTORE_PASSWORD_PROPERTY = new ProviderConfigProperty(KEYSTORE_PASSWORD_KEY, "Keystore Password", "Password for the keys", STRING_TYPE, null, true);

    public static String KEYSTORE_TYPE_KEY = "keystoreType";

    // Initialization of this property is postponed to "init()" due the CryptoProvider must be set
    private ProviderConfigProperty keystoreTypeProperty;

    public static String KEY_ALIAS_KEY = "keyAlias";
    public static ProviderConfigProperty KEY_ALIAS_PROPERTY = new ProviderConfigProperty(KEY_ALIAS_KEY, "Key Alias", "Alias for the private key", STRING_TYPE, null);

    public static String KEY_PASSWORD_KEY = "keyPassword";
    public static ProviderConfigProperty KEY_PASSWORD_PROPERTY = new ProviderConfigProperty(KEY_PASSWORD_KEY, "Key Password", "Password for the private key", STRING_TYPE, null, true);

    private static final String HELP_TEXT = "Loads keys from a Java keys file";

    private List<ProviderConfigProperty> configProperties;


    @Override
    public void init(Config.Scope config) {
        String[] supportedKeystoreTypes = CryptoIntegration.getProvider().getSupportedKeyStoreTypes()
                .map(KeystoreUtil.KeystoreFormat::toString)
                .toArray(String[]::new);
        this.keystoreTypeProperty = new ProviderConfigProperty(KEYSTORE_TYPE_KEY, "Keystore Type",
                "Keystore type. This parameter is not mandatory. If omitted, the type will be detected from keystore file or default keystore type will be used", LIST_TYPE,
                supportedKeystoreTypes.length > 0 ? supportedKeystoreTypes[0] : null, supportedKeystoreTypes);

        configProperties = ProviderConfigurationBuilder.create()
                .property(Attributes.PRIORITY_PROPERTY)
                .property(Attributes.ENABLED_PROPERTY)
                .property(Attributes.ACTIVE_PROPERTY)
                .property(mergedAlgorithmProperties())
                .property(KEYSTORE_PROPERTY)
                .property(KEYSTORE_PASSWORD_PROPERTY)
                .property(keystoreTypeProperty)
                .property(KEY_ALIAS_PROPERTY)
                .property(KEY_PASSWORD_PROPERTY)
                .property(Attributes.KEY_USE_PROPERTY)
                .build();
    }

    @Override
    public KeyProvider create(KeycloakSession session, ComponentModel model) {
        return new JavaKeystoreKeyProvider(session.getContext().getRealm(), model);
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel model) throws ComponentValidationException {

        ConfigurationValidationHelper.check(model)
                .checkLong(Attributes.PRIORITY_PROPERTY, false)
                .checkBoolean(Attributes.ENABLED_PROPERTY, false)
                .checkBoolean(Attributes.ACTIVE_PROPERTY, false)
                .checkSingle(KEYSTORE_PROPERTY, true)
                .checkSingle(KEYSTORE_PASSWORD_PROPERTY, true)
                .checkSingle(keystoreTypeProperty, false)
                .checkSingle(KEY_ALIAS_PROPERTY, true)
                .checkSingle(KEY_PASSWORD_PROPERTY, true);

        try {
            new JavaKeystoreKeyProvider(realm, model).loadKey(realm, model);
        } catch (Throwable t) {
            logger.error("Failed to load keys.", t);
            throw new ComponentValidationException("Failed to load keys. " + t.getMessage(), t);
        }
    }

    // merge the algorithms supported for RSA and EC keys and provide them as one configuration property
    private static ProviderConfigProperty mergedAlgorithmProperties() {
        List<String> ecAlgorithms = List.of(Algorithm.ES256, Algorithm.ES384, Algorithm.ES512);
        List<String> algorithms = Stream.of(Attributes.RS_ALGORITHM_PROPERTY.getOptions(),
                        ecAlgorithms, Attributes.RS_ENC_ALGORITHM_PROPERTY.getOptions())
                .flatMap(Collection::stream)
                .toList();
        return new ProviderConfigProperty(Attributes.RS_ALGORITHM_PROPERTY.getName(), Attributes.RS_ALGORITHM_PROPERTY.getLabel(),
                Attributes.RS_ALGORITHM_PROPERTY.getHelpText(), Attributes.RS_ALGORITHM_PROPERTY.getType(),
                Attributes.RS_ALGORITHM_PROPERTY.getDefaultValue(), algorithms.toArray(String[]::new));

    }

    @Override
    public String getHelpText() {
        return HELP_TEXT;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return this.configProperties;
    }

    @Override
    public String getId() {
        return ID;
    }

}

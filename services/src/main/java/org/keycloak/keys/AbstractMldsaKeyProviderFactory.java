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
package org.keycloak.keys;

import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ConfigurationValidationHelper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.NamedParameterSpec;

import static org.keycloak.provider.ProviderConfigProperty.LIST_TYPE;

public abstract class AbstractMldsaKeyProviderFactory implements KeyProviderFactory {

    protected static final String MLDSA_PRIVATE_KEY_KEY = "mldsaPrivateKey";
    protected static final String MLDSA_PUBLIC_KEY_KEY = "mldsaPublicKey";

    protected static ProviderConfigProperty MLDSA_PROPERTY = new ProviderConfigProperty("ML-DSA", "Generates ML-DSA keys",
            LIST_TYPE, Algorithm.MLDSA44, Algorithm.MLDSA65, Algorithm.MLDSA87);

    public final static ProviderConfigurationBuilder configurationBuilder() {
        return ProviderConfigurationBuilder.create()
                .property(Attributes.PRIORITY_PROPERTY)
                .property(Attributes.ENABLED_PROPERTY)
                .property(Attributes.ACTIVE_PROPERTY);
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel model) throws ComponentValidationException {
        ConfigurationValidationHelper.check(model)
                .checkLong(Attributes.PRIORITY_PROPERTY, false)
                .checkBoolean(Attributes.ENABLED_PROPERTY, false)
                .checkBoolean(Attributes.ACTIVE_PROPERTY, false);
    }

    public static KeyPair generateMldsaKeyPair(String algorithm) {
        if (!JavaAlgorithm.isMldsaJavaAlgorithm(algorithm)) {
            throw new IllegalArgumentException(algorithm + " is not supported");
        }
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(algorithm);
            NamedParameterSpec spec = new NamedParameterSpec(algorithm);
            keyGen.initialize(spec);
            return keyGen.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

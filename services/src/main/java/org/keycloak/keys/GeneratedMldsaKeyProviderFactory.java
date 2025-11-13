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

import java.security.KeyPair;
import java.util.Base64;
import java.util.List;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.MLDSAClientSignatureVerifierProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;

import org.jboss.logging.Logger;

public class GeneratedMldsaKeyProviderFactory extends AbstractMldsaKeyProviderFactory implements EnvironmentDependentProviderFactory {

    private static final Logger logger = Logger.getLogger(GeneratedMldsaKeyProviderFactory.class);

    public static final String ID = "mldsa-generated";

    private static final String HELP_TEXT = "Generates ML-DSA keys";

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = AbstractMldsaKeyProviderFactory.configurationBuilder()
            .property(Attributes.MLDSA_ALGORITHM_PROPERTY)
            .build();

    @Override
    public KeyProvider create(KeycloakSession session, ComponentModel model) {
        if (Profile.isFeatureEnabled(Profile.Feature.PQC_ML_DSA)) {
            return new GeneratedMldsaKeyProvider(session.getContext().getRealm(), model);
        } else {
            return null;
        }
    }

    @Override
    public boolean createFallbackKeys(KeycloakSession session, KeyUse keyUse, String algorithm) {
        if (keyUse.equals(KeyUse.SIG) && JavaAlgorithm.isMldsaJavaAlgorithm(algorithm)
            && Profile.isFeatureEnabled(Profile.Feature.PQC_ML_DSA)) {
            RealmModel realm = session.getContext().getRealm();

            ComponentModel generated = new ComponentModel();
            generated.setName("fallback-" + algorithm);
            generated.setParentId(realm.getId());
            generated.setProviderId(ID);
            generated.setProviderType(KeyProvider.class.getName());

            MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
            config.putSingle(Attributes.PRIORITY_KEY, "-100");
            generated.setConfig(config);

            realm.addComponentModel(generated);

            return true;
        } else {
            return false;
        }
    }

    @Override
    public String getHelpText() {
        return HELP_TEXT;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel model) throws ComponentValidationException {
        super.validateConfiguration(session, realm, model);

        String algorithm = realm.getDefaultSignatureAlgorithm();
        generateKeys(model, algorithm);
        logger.debugv("Generated keys for {0}", realm.getName());
    }

    private void generateKeys(ComponentModel model, String algorithm) throws IllegalArgumentException {
        KeyPair keyPair;
        if (!JavaAlgorithm.isMldsaJavaAlgorithm(algorithm)) {
            throw new IllegalStateException("No known Algorithm: " + algorithm);
        }
        try {
            keyPair = generateMldsaKeyPair(algorithm);

            byte[] publicEncoded = keyPair.getPublic().getEncoded();
            byte[] privateEncoded = keyPair.getPrivate().getEncoded();

            model.put(Attributes.CERTIFICATE_KEY, Base64.getUrlEncoder().encodeToString(publicEncoded));
            model.put(Attributes.PRIVATE_KEY_KEY, Base64.getUrlEncoder().encodeToString(privateEncoded));
        } catch (Throwable t) {
            throw new ComponentValidationException("Failed to generate ML-DSA keys", t);
        }
    }

    /**
     * Check if the provider is supported and should be available based on the provider configuration.
     *
     * @param config the provider configuration
     * @return {@code true} if the provider is supported. Otherwise, {@code false}.
     */
    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.PQC_ML_DSA);
    }
}

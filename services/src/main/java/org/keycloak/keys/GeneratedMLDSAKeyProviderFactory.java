/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Set;

import org.keycloak.Config;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.MLDSAUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ConfigurationValidationHelper;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import org.jboss.logging.Logger;

import static org.keycloak.provider.ProviderConfigProperty.LIST_TYPE;

public class GeneratedMLDSAKeyProviderFactory implements KeyProviderFactory<KeyProvider>, EnvironmentDependentProviderFactory {

    private static final Logger logger = Logger.getLogger(GeneratedMLDSAKeyProviderFactory.class);
    private static final Set<String> ALGORITHMS = Set.of(
            Algorithm.ML_DSA_44, Algorithm.ML_DSA_65, Algorithm.ML_DSA_87);

    public static final String ID = "mldsa-generated";
    public static final String MLDSA_ALGORITHM_KEY = "mldsaAlgorithm";
    public static final String MLDSA_PRIVATE_KEY_KEY = "mldsaPrivateKey";
    public static final String MLDSA_PUBLIC_KEY_KEY = "mldsaPublicKey";
    public static final String DEFAULT_MLDSA_ALGORITHM = Algorithm.ML_DSA_44;

    private static final ProviderConfigProperty MLDSA_ALGORITHM_PROPERTY = new ProviderConfigProperty(
            MLDSA_ALGORITHM_KEY, "Algorithm", "ML-DSA parameter set", LIST_TYPE, DEFAULT_MLDSA_ALGORITHM,
            Algorithm.ML_DSA_44, Algorithm.ML_DSA_65, Algorithm.ML_DSA_87);
    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = ProviderConfigurationBuilder.create()
            .property(Attributes.PRIORITY_PROPERTY)
            .property(Attributes.ENABLED_PROPERTY)
            .property(Attributes.ACTIVE_PROPERTY)
            .property(MLDSA_ALGORITHM_PROPERTY)
            .build();

    @Override
    public KeyProvider create(KeycloakSession session, ComponentModel model) {
        return new GeneratedMLDSAKeyProvider(session.getContext().getRealm(), model);
    }

    @Override
    public boolean createFallbackKeys(KeycloakSession session, KeyUse keyUse, String algorithm) {
        if (!KeyUse.SIG.equals(keyUse) || !ALGORITHMS.contains(algorithm)
                || !MLDSAUtils.isKeyGenerationSupported(algorithm)) {
            return false;
        }

        RealmModel realm = session.getContext().getRealm();
        ComponentModel generated = new ComponentModel();
        generated.setName("fallback-" + algorithm);
        generated.setParentId(realm.getId());
        generated.setProviderId(ID);
        generated.setProviderType(KeyProvider.class.getName());

        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        config.putSingle(Attributes.PRIORITY_KEY, "-100");
        config.putSingle(MLDSA_ALGORITHM_KEY, algorithm);
        generated.setConfig(config);
        realm.addComponentModel(generated);
        return true;
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel model)
            throws ComponentValidationException {
        ConfigurationValidationHelper.check(model)
                .checkLong(Attributes.PRIORITY_PROPERTY, false)
                .checkBoolean(Attributes.ENABLED_PROPERTY, false)
                .checkBoolean(Attributes.ACTIVE_PROPERTY, false)
                .checkList(MLDSA_ALGORITHM_PROPERTY, false);

        String algorithm = model.get(MLDSA_ALGORITHM_KEY, DEFAULT_MLDSA_ALGORITHM);
        if (!ALGORITHMS.contains(algorithm)) {
            throw new ComponentValidationException("Unsupported ML-DSA algorithm: " + algorithm);
        }
        if (!MLDSAUtils.isKeyGenerationSupported(algorithm)) {
            throw new ComponentValidationException("ML-DSA key generation is unavailable for " + algorithm);
        }

        if (!model.contains(MLDSA_PRIVATE_KEY_KEY) || !model.contains(MLDSA_PUBLIC_KEY_KEY)
                || !algorithm.equals(getStoredAlgorithm(model))) {
            generateKeys(model, algorithm);
            logger.debugv("Generated {0} keys for {1}", algorithm, realm.getName());
        }
    }

    private static String getStoredAlgorithm(ComponentModel model) {
        String encoded = model.get(MLDSA_PUBLIC_KEY_KEY);
        if (encoded == null) {
            return null;
        }

        for (String algorithm : ALGORITHMS) {
            try {
                KeyFactory keyFactory = CryptoIntegration.getProvider().getKeyFactory(algorithm);
                PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(encoded)));
                return publicKey.getAlgorithm();
            } catch (Exception ignored) {
            }
        }
        throw new ComponentValidationException("Failed to decode stored ML-DSA public key");
    }

    private static void generateKeys(ComponentModel model, String algorithm) {
        try {
            KeyPair keyPair = CryptoIntegration.getProvider().getKeyPairGen(algorithm).generateKeyPair();
            if (!algorithm.equals(keyPair.getPrivate().getAlgorithm())
                    || !algorithm.equals(keyPair.getPublic().getAlgorithm())) {
                throw new IllegalStateException("Generated key does not match " + algorithm);
            }
            model.put(MLDSA_PRIVATE_KEY_KEY, Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
            model.put(MLDSA_PUBLIC_KEY_KEY, Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
            model.put(MLDSA_ALGORITHM_KEY, algorithm);
            model.removeNote(KeyWrapper.class.getName());
        } catch (Exception e) {
            throw new ComponentValidationException("Failed to generate " + algorithm + " keys", e);
        }
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return ALGORITHMS.stream().anyMatch(MLDSAUtils::isKeyGenerationSupported);
    }

    @Override
    public String getHelpText() {
        return "Generates ML-DSA signature keys";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public String getId() {
        return ID;
    }
}

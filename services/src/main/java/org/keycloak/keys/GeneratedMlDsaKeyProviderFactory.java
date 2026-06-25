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
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ConfigurationValidationHelper;
import org.keycloak.provider.ProviderConfigProperty;

import org.jboss.logging.Logger;

public class GeneratedMlDsaKeyProviderFactory extends AbstractMlDsaKeyProviderFactory {

    private static final Logger logger = Logger.getLogger(GeneratedMlDsaKeyProviderFactory.class);

    public static final String ID = "mldsa-generated";

    private static final String HELP_TEXT = "Generates ML-DSA keys";

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = AbstractMlDsaKeyProviderFactory.configurationBuilder()
            .property(MLDSA_ALGORITHM_PROPERTY)
            .build();

    @Override
    public KeyProvider create(KeycloakSession session, ComponentModel model) {
        return new GeneratedMlDsaKeyProvider(session.getContext().getRealm(), model);
    }

    @Override
    public boolean createFallbackKeys(KeycloakSession session, KeyUse keyUse, String algorithm) {
        if (keyUse.equals(KeyUse.SIG) && (algorithm.equals(Algorithm.ML_DSA_44) || algorithm.equals(Algorithm.ML_DSA_65) || algorithm.equals(Algorithm.ML_DSA_87))) {
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

        ConfigurationValidationHelper.check(model).checkList(MLDSA_ALGORITHM_PROPERTY, false);

        String algorithm = model.get(MLDSA_ALGORITHM_KEY);
        if (algorithm == null) algorithm = DEFAULT_MLDSA_ALGORITHM;

        if (!(model.contains(MLDSA_PRIVATE_KEY_KEY) && model.contains(MLDSA_PUBLIC_KEY_KEY))) {
            generateKeys(model, algorithm);
            logger.debugv("Generated ML-DSA keys for {0}", realm.getName());
        } else {
            String currentAlg = getAlgorithmFromPublicKey(model.getConfig().getFirst(MLDSA_PUBLIC_KEY_KEY));
            if (!algorithm.equals(currentAlg)) {
                generateKeys(model, algorithm);
                logger.debugv("ML-DSA algorithm changed, generating new keys for {0}", realm.getName());
            }
        }
    }

    private void generateKeys(ComponentModel model, String algorithm) {
        KeyPair keyPair;
        try {
            KeyPairGenerator keyGen = CryptoIntegration.getProvider().getKeyPairGen(algorithm);
            keyPair = keyGen.generateKeyPair();
            model.put(MLDSA_PRIVATE_KEY_KEY, Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
            model.put(MLDSA_PUBLIC_KEY_KEY, Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
            model.put(MLDSA_ALGORITHM_KEY, algorithm);
        } catch (Throwable t) {
            throw new ComponentValidationException("Failed to generate ML-DSA keys", t);
        }
    }

    private String getAlgorithmFromPublicKey(String publicKeyBase64) {
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.getMimeDecoder().decode(publicKeyBase64));
        for (String alg : List.of(Algorithm.ML_DSA_44, Algorithm.ML_DSA_65, Algorithm.ML_DSA_87)) {
            try {
                KeyFactory kf = CryptoIntegration.getProvider().getKeyFactory(alg);
                PublicKey pubKey = kf.generatePublic(publicKeySpec);
                if (pubKey != null) {
                    return alg;
                }
            } catch (Throwable ignored) {
            }
        }
        throw new ComponentValidationException("Failed to determine ML-DSA algorithm from its public key");
    }
}

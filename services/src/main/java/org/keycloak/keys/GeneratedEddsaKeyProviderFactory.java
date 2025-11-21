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

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.interfaces.EdECPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;

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

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class GeneratedEddsaKeyProviderFactory extends AbstractEddsaKeyProviderFactory {

    private static final Logger logger = Logger.getLogger(GeneratedEddsaKeyProviderFactory.class);

    public static final String ID = "eddsa-generated";

    private static final String HELP_TEXT = "Generates EdDSA keys";

    public static final String DEFAULT_EDDSA_ELLIPTIC_CURVE = Algorithm.Ed25519;

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = AbstractEddsaKeyProviderFactory.configurationBuilder()
            .property(EDDSA_ELLIPTIC_CURVE_PROPERTY)
            .build();

    @Override
    public KeyProvider create(KeycloakSession session, ComponentModel model) {
        return new GeneratedEddsaKeyProvider(session.getContext().getRealm(), model);
    }

    @Override
    public boolean createFallbackKeys(KeycloakSession session, KeyUse keyUse, String algorithm) {
        if (keyUse.equals(KeyUse.SIG) && algorithm.equals(Algorithm.EdDSA)) {
            RealmModel realm = session.getContext().getRealm();

            ComponentModel generated = new ComponentModel();
            generated.setName("fallback-" + algorithm);
            generated.setParentId(realm.getId());
            generated.setProviderId(ID);
            generated.setProviderType(KeyProvider.class.getName());

            MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
            config.putSingle(Attributes.PRIORITY_KEY, "-100");
            config.putSingle(EDDSA_ELLIPTIC_CURVE_KEY, DEFAULT_EDDSA_ELLIPTIC_CURVE);
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

        ConfigurationValidationHelper.check(model).checkList(EDDSA_ELLIPTIC_CURVE_PROPERTY, false);

        String curveName = model.get(EDDSA_ELLIPTIC_CURVE_KEY);
        if (curveName == null) curveName = DEFAULT_EDDSA_ELLIPTIC_CURVE;

        if (!(model.contains(EDDSA_PRIVATE_KEY_KEY) && model.contains(EDDSA_PUBLIC_KEY_KEY))) {
            generateKeys(model, curveName);
            logger.debugv("Generated keys for {0}", realm.getName());
        } else {
            String currentEdEc = getCurveFromPublicKey(model.getConfig().getFirst(GeneratedEddsaKeyProviderFactory.EDDSA_PUBLIC_KEY_KEY));
            if (!curveName.equals(currentEdEc)) {
                generateKeys(model, curveName);
                logger.debugv("Twisted Edwards Curve changed, generating new keys for {0}", realm.getName());
            }
        }
    }

    private void generateKeys(ComponentModel model, String curveName) {
        KeyPair keyPair;
        try {
            keyPair = generateEddsaKeyPair(curveName);
            model.put(EDDSA_PRIVATE_KEY_KEY, Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
            model.put(EDDSA_PUBLIC_KEY_KEY, Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
            model.put(EDDSA_ELLIPTIC_CURVE_KEY, curveName);
        } catch (Throwable t) {
            throw new ComponentValidationException("Failed to generate EdDSA keys", t);
        }
    }

    private String getCurveFromPublicKey(String publicEddsaKeyBase64Encoded) {
        try {
            KeyFactory kf = KeyFactory.getInstance("EdDSA");
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(publicEddsaKeyBase64Encoded));
            EdECPublicKey edEcKey = (EdECPublicKey) kf.generatePublic(publicKeySpec);
            return edEcKey.getParams().getName();
        } catch (Throwable t) {
            throw new ComponentValidationException("Failed to get Twisted Edwards Curve from its public key", t);
        }
    }
}

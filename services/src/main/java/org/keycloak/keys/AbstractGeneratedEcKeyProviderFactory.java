/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
import java.security.interfaces.ECPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.crypto.KeyUse;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ConfigurationValidationHelper;
import org.keycloak.provider.ProviderConfigProperty;

import org.jboss.logging.Logger;

public abstract class AbstractGeneratedEcKeyProviderFactory<T extends KeyProvider>
        extends AbstractEcKeyProviderFactory<T> {

    abstract protected String getDefaultEcEllipticCurve();

    abstract protected String getEcEllipticCurveKey();

    abstract protected String getEcEllipticCurveKey(String algorithm);

    abstract protected ProviderConfigProperty getEcEllipticCurveProperty();

    abstract protected String getEcPrivateKeyKey();

    abstract protected String getEcPublicKeyKey();

    abstract protected Logger getLogger();

    abstract protected boolean isSupportedEcAlgorithm(String algorithm);

    abstract protected boolean isValidKeyUse(KeyUse keyUse);

    @Override
    public boolean createFallbackKeys(KeycloakSession session, KeyUse keyUse, String algorithm) {
        if (isValidKeyUse(keyUse) && isSupportedEcAlgorithm(algorithm)) {
            RealmModel realm = session.getContext().getRealm();

            ComponentModel generated = new ComponentModel();
            generated.setName("fallback-" + algorithm);
            generated.setParentId(realm.getId());
            generated.setProviderId(getId());
            generated.setProviderType(KeyProvider.class.getName());

            MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
            config.putSingle(Attributes.PRIORITY_KEY, "-100");
            config.putSingle(getEcEllipticCurveKey(), getEcEllipticCurveKey(algorithm));
            generated.setConfig(config);

            realm.addComponentModel(generated);

            return true;
        } else {
            return false;
        }
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel model) throws ComponentValidationException {
        super.validateConfiguration(session, realm, model);

        ConfigurationValidationHelper.check(model).checkList(getEcEllipticCurveProperty(), false);

        String ecInNistRep = model.get(getEcEllipticCurveKey());
        if (ecInNistRep == null) ecInNistRep = getDefaultEcEllipticCurve();

        if (!(model.contains(getEcPrivateKeyKey()) && model.contains(getEcPublicKeyKey()))) {
            generateKeys(model, ecInNistRep);
            getLogger().debugv("Generated keys for {0}", realm.getName());
        } else {
            String currentEc = getCurveFromPublicKey(model.getConfig().getFirst(getEcPublicKeyKey()));
            if (!ecInNistRep.equals(currentEc)) {
                generateKeys(model, ecInNistRep);
                getLogger().debugv("Elliptic Curve changed, generating new keys for {0}", realm.getName());
            }
        }
    }

    protected void generateKeys(ComponentModel model, String ecInNistRep) {
        KeyPair keyPair;
        try {
            keyPair = generateEcKeyPair(convertECDomainParmNistRepToSecRep(ecInNistRep));
            model.put(getEcPrivateKeyKey(), Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
            model.put(getEcPublicKeyKey(), Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
            model.put(getEcEllipticCurveKey(), ecInNistRep);
        } catch (Throwable t) {
            throw new ComponentValidationException("Failed to generate EC keys", t);
        }
    }

    protected String getCurveFromPublicKey(String publicEcKeyBase64Encoded) {
        try {
            KeyFactory kf = KeyFactory.getInstance("EC");
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(publicEcKeyBase64Encoded));
            ECPublicKey ecKey = (ECPublicKey) kf.generatePublic(publicKeySpec);
            return "P-" + ecKey.getParams().getCurve().getField().getFieldSize();
        } catch (Throwable t) {
            throw new ComponentValidationException("Failed to get EC from its public key", t);
        }
    }
}

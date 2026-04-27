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

import java.util.List;

import org.keycloak.component.ComponentModel;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;

import org.jboss.logging.Logger;

import static org.keycloak.provider.ProviderConfigProperty.LIST_TYPE;

public class GeneratedEcdsaKeyProviderFactory extends AbstractGeneratedEcKeyProviderFactory<KeyProvider> {

    private static final Logger logger = Logger.getLogger(GeneratedEcdsaKeyProviderFactory.class);

    public static final String ECDSA_PRIVATE_KEY_KEY = "ecdsaPrivateKey";
    public static final String ECDSA_PUBLIC_KEY_KEY = "ecdsaPublicKey";
    public static final String ECDSA_ELLIPTIC_CURVE_KEY = "ecdsaEllipticCurveKey";

    // only support NIST P-256 for ES256, P-384 for ES384, P-521 for ES512
    protected static ProviderConfigProperty ECDSA_ELLIPTIC_CURVE_PROPERTY = new ProviderConfigProperty(ECDSA_ELLIPTIC_CURVE_KEY, "Elliptic Curve", "Elliptic Curve used in ECDSA", LIST_TYPE,
            String.valueOf(GeneratedEcdsaKeyProviderFactory.DEFAULT_ECDSA_ELLIPTIC_CURVE),
            "P-256", "P-384", "P-521");

    public static final String ID = "ecdsa-generated";

    private static final String HELP_TEXT = "Generates ECDSA keys";

     // secp256r1,NIST P-256,X9.62 prime256v1,1.2.840.10045.3.1.7
    public static final String DEFAULT_ECDSA_ELLIPTIC_CURVE = DEFAULT_EC_ELLIPTIC_CURVE;

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = AbstractGeneratedEcKeyProviderFactory.configurationBuilder()
            .property(ECDSA_ELLIPTIC_CURVE_PROPERTY)
            .build();

    @Override
    public KeyProvider create(KeycloakSession session, ComponentModel model) {
        return new GeneratedEcdsaKeyProvider(session.getContext().getRealm(), model);
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
    protected Logger getLogger() {
        return logger;
    }

    @Override
    protected boolean isValidKeyUse(KeyUse keyUse) {
        return KeyUse.SIG.equals(keyUse);
    }

    @Override
    protected boolean isSupportedEcAlgorithm(String algorithm) {
        return (algorithm.equals(Algorithm.ES256) || algorithm.equals(Algorithm.ES384)
                || algorithm.equals(Algorithm.ES512));
    }

    @Override
    protected String getEcEllipticCurveKey(String algorithm) {
        return convertJWSAlgorithmToECDomainParmNistRep(algorithm);
    }

    @Override
    protected ProviderConfigProperty getEcEllipticCurveProperty() {
        return ECDSA_ELLIPTIC_CURVE_PROPERTY;
    }

    @Override
    protected String getEcEllipticCurveKey() {
        return ECDSA_ELLIPTIC_CURVE_KEY;
    }

    @Override
    protected String getEcPrivateKeyKey() {
        return ECDSA_PRIVATE_KEY_KEY;
    }

    @Override
    protected String getEcPublicKeyKey() {
        return ECDSA_PUBLIC_KEY_KEY;
    }

    @Override
    protected String getDefaultEcEllipticCurve() {
        return DEFAULT_ECDSA_ELLIPTIC_CURVE;
    }

    public static String convertECDomainParmNistRepToJWSAlgorithm(String ecInNistRep) {
        switch(ecInNistRep) {
            case "P-256" :
                return Algorithm.ES256;
            case "P-384" :
                return Algorithm.ES384;
            case "P-521" :
                return Algorithm.ES512;
            default :
                return null;
        }
    }

    public static String convertJWSAlgorithmToECDomainParmNistRep(String algorithm) {
        switch(algorithm) {
            case Algorithm.ES256 :
                return "P-256";
            case Algorithm.ES384 :
                return "P-384";
            case Algorithm.ES512 :
                return "P-521";
            default :
                return null;
        }
    }
}

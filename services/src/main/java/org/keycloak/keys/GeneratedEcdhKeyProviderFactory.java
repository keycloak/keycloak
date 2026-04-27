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

import java.util.List;

import org.keycloak.component.ComponentModel;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;

import org.jboss.logging.Logger;

import static org.keycloak.provider.ProviderConfigProperty.LIST_TYPE;

public class GeneratedEcdhKeyProviderFactory extends AbstractGeneratedEcKeyProviderFactory<KeyProvider> {

    // secp256r1,NIST P-256,X9.62 prime256v1,1.2.840.10045.3.1.7
    public static final String DEFAULT_ECDH_ELLIPTIC_CURVE = DEFAULT_EC_ELLIPTIC_CURVE;

    public static final String ECDH_ALGORITHM_KEY = "ecdhAlgorithm";

    public static final String ECDH_ELLIPTIC_CURVE_KEY = "ecdhEllipticCurveKey";
    public static final String ECDH_PRIVATE_KEY_KEY = "ecdhPrivateKey";
    public static final String ECDH_PUBLIC_KEY_KEY = "ecdhPublicKey";

    // only support NIST P-256 for ES256, P-384 for ES384, P-521 for ES512
    protected static ProviderConfigProperty ECDH_ELLIPTIC_CURVE_PROPERTY = new ProviderConfigProperty(ECDH_ELLIPTIC_CURVE_KEY, "Elliptic Curve", "Elliptic Curve used in ECDH", LIST_TYPE,
            String.valueOf(GeneratedEcdhKeyProviderFactory.DEFAULT_ECDH_ELLIPTIC_CURVE),
            "P-256", "P-384", "P-521");

    protected static ProviderConfigProperty ECDH_ALGORITHM_PROPERTY = new ProviderConfigProperty(ECDH_ALGORITHM_KEY,
            "Algorithm", "Algorithm for processing the Content Encryption Key", LIST_TYPE, Algorithm.ECDH_ES,
            Algorithm.ECDH_ES, Algorithm.ECDH_ES_A128KW, Algorithm.ECDH_ES_A192KW, Algorithm.ECDH_ES_A256KW);

    private static final String HELP_TEXT = "Generates ECDH keys";

    public static final String ID = "ecdh-generated";

    private static final Logger logger = Logger.getLogger(GeneratedEcdhKeyProviderFactory.class);

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = AbstractGeneratedEcKeyProviderFactory.configurationBuilder()
            .property(ECDH_ELLIPTIC_CURVE_PROPERTY)
            .property(ECDH_ALGORITHM_PROPERTY)
            .build();

    public static String convertECDomainParmNistRepToJWEAlgorithm(String ecInNistRep) {
        switch(ecInNistRep) {
            case "P-256" :
                return Algorithm.ECDH_ES_A128KW;
            case "P-384" :
                return Algorithm.ECDH_ES_A192KW;
            case "P-521" :
                return Algorithm.ECDH_ES_A256KW;
            default :
                return null;
        }
    }

    public static String convertJWEAlgorithmToECDomainParmNistRep(String algorithm) {
        switch(algorithm) {
            case Algorithm.ECDH_ES_A128KW :
                return "P-256";
            case Algorithm.ECDH_ES_A192KW :
                return "P-384";
            case Algorithm.ECDH_ES_A256KW :
                return "P-521";
            default :
                return null;
        }
    }

    @Override
    public KeyProvider create(KeycloakSession session, ComponentModel model) {
        return new GeneratedEcdhKeyProvider(session.getContext().getRealm(), model);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    protected String getDefaultEcEllipticCurve() {
        return DEFAULT_ECDH_ELLIPTIC_CURVE;
    }

    @Override
    protected String getEcEllipticCurveKey() {
        return ECDH_ELLIPTIC_CURVE_KEY;
    }

    @Override
    protected String getEcEllipticCurveKey(String algorithm) {
        if (Algorithm.ECDH_ES.equals(algorithm)) {
            return DEFAULT_ECDH_ELLIPTIC_CURVE;
        }
        return convertJWEAlgorithmToECDomainParmNistRep(algorithm);
    }

    @Override
    protected ProviderConfigProperty getEcEllipticCurveProperty() {
        return ECDH_ELLIPTIC_CURVE_PROPERTY;
    }

    @Override
    protected String getEcPrivateKeyKey() {
        return ECDH_PRIVATE_KEY_KEY;
    }

    @Override
    protected String getEcPublicKeyKey() {
        return ECDH_PUBLIC_KEY_KEY;
    }

    @Override
    public String getHelpText() {
        return HELP_TEXT;
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
    protected boolean isSupportedEcAlgorithm(String algorithm) {
        return (algorithm.equals(Algorithm.ECDH_ES) || algorithm.equals(Algorithm.ECDH_ES_A128KW)
                || algorithm.equals(Algorithm.ECDH_ES_A192KW) || algorithm.equals(Algorithm.ECDH_ES_A256KW));
    }

    @Override
    protected boolean isValidKeyUse(KeyUse keyUse) {
        return KeyUse.ENC.equals(keyUse);
    }
}

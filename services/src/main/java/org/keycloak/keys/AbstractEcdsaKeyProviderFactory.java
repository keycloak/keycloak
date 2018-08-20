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

import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.crypto.Algorithm;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ConfigurationValidationHelper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;

import static org.keycloak.provider.ProviderConfigProperty.LIST_TYPE;

public abstract class AbstractEcdsaKeyProviderFactory implements KeyProviderFactory {

    protected static final String ECDSA_PRIVATE_KEY_KEY = "ecdsaPrivateKey";
    protected static final String ECDSA_PUBLIC_KEY_KEY = "ecdsaPublicKey";
    protected static final String ECDSA_ELLIPTIC_CURVE_KEY = "ecdsaEllipticCurveKey";

    // only support NIST P-256 for ES256, P-384 for ES384, P-521 for ES512
    protected static ProviderConfigProperty ECDSA_ELLIPTIC_CURVE_PROPERTY = new ProviderConfigProperty(ECDSA_ELLIPTIC_CURVE_KEY, "Elliptic Curve", "Elliptic Curve used in ECDSA", LIST_TYPE,
            String.valueOf(GeneratedEcdsaKeyProviderFactory.DEFAULT_ECDSA_ELLIPTIC_CURVE),
            "P-256", "P-384", "P-521");
 
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

    public static KeyPair generateEcdsaKeyPair(String keySpecName) {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
            SecureRandom randomGen = SecureRandom.getInstance("SHA1PRNG");
            ECGenParameterSpec ecSpec = new ECGenParameterSpec(keySpecName);
            keyGen.initialize(ecSpec, randomGen);
            return keyGen.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String convertECDomainParmNistRepToSecRep(String ecInNistRep) {
        // convert Elliptic Curve Domain Parameter Name in NIST to SEC which is used to generate its EC key
        String ecInSecRep = null;
        switch(ecInNistRep) {
            case "P-256" :
            	ecInSecRep = "secp256r1";
                break;
            case "P-384" :
            	ecInSecRep = "secp384r1";
                break;
            case "P-521" :
            	ecInSecRep = "secp521r1";
                break;
            default :
                // return null
        }
        return ecInSecRep;
    }

    public static String convertECDomainParmNistRepToAlgorithm(String ecInNistRep) {
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

    public static String convertAlgorithmToECDomainParmNistRep(String algorithm) {
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

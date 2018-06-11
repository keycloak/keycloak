package org.keycloak.keys;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;

import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.crypto.Algorithm;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ConfigurationValidationHelper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import static org.keycloak.provider.ProviderConfigProperty.LIST_TYPE;

// KEYCLOAK-7560 Refactoring Token Signing and Verifying by Token Signature SPI

@SuppressWarnings("rawtypes")
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
        // convert Elliptic Curve Domain Parameter Name in NIST to Algorithm (JWA) representation
        String ecInAlgorithmRep = null;
        switch(ecInNistRep) {
            case "P-256" :
                ecInAlgorithmRep = Algorithm.ES256;
                break;
            case "P-384" :
                ecInAlgorithmRep = Algorithm.ES384;
                break;
            case "P-521" :
                ecInAlgorithmRep = Algorithm.ES512;
                break;
            default :
                // return null
        }
        return ecInAlgorithmRep;
    }

}

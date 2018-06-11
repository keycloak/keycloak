package org.keycloak.keys;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;

import org.jboss.logging.Logger;
import org.keycloak.common.util.Base64;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ConfigurationValidationHelper;
import org.keycloak.provider.ProviderConfigProperty;

// KEYCLOAK-7560 Refactoring Token Signing and Verifying by Token Signature SPI

public class GeneratedEcdsaKeyProviderFactory extends AbstractEcdsaKeyProviderFactory {

    private static final Logger logger = Logger.getLogger(GeneratedEcdsaKeyProviderFactory.class);

    public static final String ID = "ecdsa-generated";

    private static final String HELP_TEXT = "Generates ECDSA keys";

     // secp256r1,NIST P-256,X9.62 prime256v1,1.2.840.10045.3.1.7
    public static final String DEFAULT_ECDSA_ELLIPTIC_CURVE = "P-256";

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = AbstractEcdsaKeyProviderFactory.configurationBuilder()
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
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel model) throws ComponentValidationException {
        super.validateConfiguration(session, realm, model);

        ConfigurationValidationHelper.check(model).checkList(ECDSA_ELLIPTIC_CURVE_PROPERTY, false);

        String ecInNistRep = model.get(ECDSA_ELLIPTIC_CURVE_KEY);
        if (ecInNistRep == null) ecInNistRep = DEFAULT_ECDSA_ELLIPTIC_CURVE;

        if (!(model.contains(ECDSA_PRIVATE_KEY_KEY) && model.contains(ECDSA_PUBLIC_KEY_KEY))) {
            generateKeys(realm, model, ecInNistRep);
            logger.debugv("Generated keys for {0}", realm.getName());
        } else {
            String currentEc = model.get(ECDSA_ELLIPTIC_CURVE_KEY);
            if (!ecInNistRep.equals(currentEc)) {
                generateKeys(realm, model, ecInNistRep);
                logger.debugv("Elliptic Curve changed, generating new keys for {0}", realm.getName());
            }
        }
    }

    private void generateKeys(RealmModel realm, ComponentModel model, String ecInNistRep) {
        KeyPair keyPair;
        try {
            keyPair = generateEcdsaKeyPair(convertECDomainParmNistRepToSecRep(ecInNistRep));
            model.put(ECDSA_PRIVATE_KEY_KEY, Base64.encodeBytes(keyPair.getPrivate().getEncoded()));
            model.put(ECDSA_PUBLIC_KEY_KEY, Base64.encodeBytes(keyPair.getPublic().getEncoded()));
            model.put(ECDSA_ELLIPTIC_CURVE_KEY, ecInNistRep);
        } catch (Throwable t) {
            throw new ComponentValidationException("Failed to generate ECDSA keys", t);
        }
    }

}

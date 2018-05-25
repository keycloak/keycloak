package org.keycloak.keys;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;

import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.common.util.Base64;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ConfigurationValidationHelper;
import org.keycloak.provider.ProviderConfigProperty;

// KEYCLOAK-6770 JWS signatures using PS256 or ES256 algorithms for signing
public class GeneratedEcdsaKeyProviderFactory extends AbstractEcdsaKeyProviderFactory {
    private static final Logger logger = Logger.getLogger(GeneratedEcdsaKeyProviderFactory.class);

    public static final String DEFAULT_ECDSA_ELLIPTIC_CURVE = "P-256";
    public static final String ID = "ecdsa-generated";

    private static final String HELP_TEXT = "Generates ECDSA keys";

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = AbstractEcdsaKeyProviderFactory.configurationBuilder()
            .property(Attributes.ECDSA_ELLIPTIC_CURVE_PROPERTY)
            .build();

    @Override
    public KeyProvider create(KeycloakSession session, ComponentModel model) {
        return new GeneratedEcdsaKeyProvider(session.getContext().getRealm(), model);
    }

    @Override
    public void init(Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return ID;
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
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel model) throws ComponentValidationException {
        super.validateConfiguration(session, realm, model);

        ConfigurationValidationHelper.check(model).checkList(Attributes.ECDSA_ELLIPTIC_CURVE_PROPERTY, false);

        String ec = model.get(Attributes.ECDSA_ELLIPTIC_CURVE_KEY);
        if (ec == null) ec = DEFAULT_ECDSA_ELLIPTIC_CURVE;

        if (!(model.contains(Attributes.ECDSA_PRIVATE_KEY_KEY) && model.contains(Attributes.ECDSA_PUBLIC_KEY_KEY))) {
            generateKeys(realm, model, ec);

            logger.debugv("Generated keys for {0}", realm.getName());
        } else {
            String currentEc = model.get(Attributes.ECDSA_ELLIPTIC_CURVE_KEY);
            if (!ec.equals(currentEc)) {
                generateKeys(realm, model, ec);

                logger.debugv("Elliptic Curve changed, generating new keys for {0}", realm.getName());
            }
        }
    }

    private void generateKeys(RealmModel realm, ComponentModel model, String ec) {
        KeyPair keyPair;
        try {
            keyPair = KeyUtils.generateEcdsaKeyPair(getEcdsaKeySizeFromEc(ec));
            PrivateKey privateKey = keyPair.getPrivate();
            PublicKey publicKey = keyPair.getPublic();

            dumpRealm(realm);
            dumpPrivateKey(privateKey);
            dumpPublicKey(publicKey);

            String privateKeyBase64Encoded = Base64.encodeBytes(privateKey.getEncoded());

            logger.debugf("privateKeyBase64Encoded = ", privateKeyBase64Encoded);

            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(Base64.decode(privateKeyBase64Encoded));
            KeyFactory kf = KeyFactory.getInstance("EC");
            PrivateKey decodedPrivateKey = kf.generatePrivate(privateKeySpec);

            dumpDecodedPrivateKey(decodedPrivateKey);

            String publicKeyBase64Encoded = Base64.encodeBytes(publicKey.getEncoded());

            logger.debugf("publicKeyBase64Encoded = ", publicKeyBase64Encoded);

            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.decode(publicKeyBase64Encoded));
            PublicKey decodedPublicKey = kf.generatePublic(publicKeySpec);

            dumpDecodedPublicKey(decodedPublicKey);

            model.put(Attributes.ECDSA_PRIVATE_KEY_KEY, Base64.encodeBytes(keyPair.getPrivate().getEncoded()));
            model.put(Attributes.ECDSA_PUBLIC_KEY_KEY, Base64.encodeBytes(keyPair.getPublic().getEncoded()));
            model.put(Attributes.ECDSA_ELLIPTIC_CURVE_KEY, ec);
        } catch (Throwable t) {
            throw new ComponentValidationException("Failed to generate keys", t);
        }
    }
    private void dumpRealm(RealmModel realm) {
        logger.debugf("realm.getId() = ", realm.getId());
        logger.debugf("realm.getDisplayName() = ", realm.getDisplayName());
    }
    private void dumpPrivateKey(PrivateKey privateKey) {
        logger.debugf("privateKey.getAlgorithm() = ", privateKey.getAlgorithm());
        logger.debugf("privateKey.getFormat() = ", privateKey.getAlgorithm());
        logger.debugf("privateKey.getAlgorithm() = ", privateKey.getAlgorithm());
        logger.debugf("privateKey.toString() = ", privateKey.toString());
    }
    private void dumpPublicKey(PublicKey publicKey) {
        logger.debugf("publicKey.getAlgorithm() = ", publicKey.getAlgorithm());
        logger.debugf("publicKey.getFormat() = ", publicKey.getFormat());
        logger.debugf("publicKey.getEncoded() = ", publicKey.getEncoded());
        logger.debugf("publicKey.toString() = ", publicKey.toString());
    }
    private void dumpDecodedPrivateKey(PrivateKey decodedPrivateKey) {
        logger.debugf("decodedPrivateKey.getAlgorithm() = ", decodedPrivateKey.getAlgorithm());
        logger.debugf("decodedPrivateKey.getFormat() = ", decodedPrivateKey.getFormat());
        logger.debugf("decodedPrivateKey.getEncoded() = ", decodedPrivateKey.getEncoded());
        logger.debugf("decodedPrivateKey.toString() = ", decodedPrivateKey.toString());
    }
    private void dumpDecodedPublicKey(PublicKey decodedPublicKey) {
        logger.debugf("decodedPublicKey.getAlgorithm() = ", decodedPublicKey.getAlgorithm());
        logger.debugf("decodedPublicKey.getFormat() = ", decodedPublicKey.getFormat());
        logger.debugf("decodedPublicKey.getEncoded() = ", decodedPublicKey.getEncoded());
        logger.debugf("decodedPublicKey.toString() = ", decodedPublicKey.toString());
    }
    
    private static int getEcdsaKeySizeFromEc(String ec) {
        // only support P-256
        if (DEFAULT_ECDSA_ELLIPTIC_CURVE.equals(ec)) return 256;
        //if ("P-384".equals(ec)) return 384;
        //if ("P-521".equals(ec)) return 521;
        return 0;
    }

}

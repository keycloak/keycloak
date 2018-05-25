package org.keycloak.keys;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.jboss.logging.Logger;
import org.keycloak.common.util.Base64;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.RealmModel;

// KEYCLOAK-6770 JWS signatures using PS256 or ES256 algorithms for signing
public class GeneratedEcdsaKeyProvider extends AbstractEcdsaKeyProvider {
    private static final Logger logger = Logger.getLogger(GeneratedEcdsaKeyProvider.class);

    public GeneratedEcdsaKeyProvider(RealmModel realm, ComponentModel model) {
        super(realm, model);
    }

	@Override
	protected Keys loadKeys(RealmModel realm, ComponentModel model) {
        String privateEcdsaKeyBase64Encoded = model.getConfig().getFirst(Attributes.ECDSA_PRIVATE_KEY_KEY);
        String publicEcdsaKeyBase64Encoded = model.getConfig().getFirst(Attributes.ECDSA_PUBLIC_KEY_KEY);

        try {
            dumpRealm(realm);
            logger.debugf("privateEcdsaKeyBase64Encoded = ", privateEcdsaKeyBase64Encoded);

            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(Base64.decode(privateEcdsaKeyBase64Encoded));
            KeyFactory kf = KeyFactory.getInstance("EC");
            PrivateKey decodedPrivateKey = kf.generatePrivate(privateKeySpec);

            dumpDecodedPrivateKey(decodedPrivateKey);
            logger.debugf("publicEcdsaKeyBase64Encoded = ", publicEcdsaKeyBase64Encoded);

            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.decode(publicEcdsaKeyBase64Encoded));
            PublicKey decodedPublicKey = kf.generatePublic(publicKeySpec);

            dumpDecodedPublicKey(decodedPublicKey);

            KeyPair keyPair = new KeyPair(decodedPublicKey, decodedPrivateKey);

            String kid = KeyUtils.createKeyId(keyPair.getPublic());

            return new Keys(kid, keyPair);
        } catch (Exception e) {
            logger.warnf("Exception at decodeEcdsaPublicKey. %s", e.toString());
            return null;
        }

    }
    private void dumpRealm(RealmModel realm) {
        logger.debugf("realm.getId() = ", realm.getId());
        logger.debugf("realm.getDisplayName() = ", realm.getDisplayName());
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
}

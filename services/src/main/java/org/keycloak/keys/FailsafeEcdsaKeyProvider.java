package org.keycloak.keys;

import org.jboss.logging.Logger;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.Time;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Collections;
import java.util.List;

// KEYCLOAK-6770 JWS signatures using PS256 or ES256 algorithms for signing
public class FailsafeEcdsaKeyProvider implements EcdsaKeyProvider {
    private static final Logger logger = Logger.getLogger(FailsafeEcdsaKeyProvider.class);

    private static String KID;

    private static KeyPair KEY_PAIR;

    private static long EXPIRES;

    private KeyPair keyPair;

    private String kid;

    public FailsafeEcdsaKeyProvider() {
        logger.errorv("No active keys found, using failsafe provider, please login to admin console to add keys. Clustering is not supported.");

        synchronized (FailsafeEcdsaKeyProvider.class) {
            if (EXPIRES < Time.currentTime()) {
                KEY_PAIR = KeyUtils.generateEcdsaKeyPair(256);
                KID = KeyUtils.createKeyId(KEY_PAIR.getPublic());
                EXPIRES = Time.currentTime() + 60 * 10;

                if (EXPIRES > 0) {
                    logger.warnv("Keys expired, re-generated kid={0}", KID);
                }
            }

            kid = KID;
            keyPair = KEY_PAIR;
        }
    }

    @Override
    public String getKid() {
        return kid;
    }

    @Override
    public PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }

    @Override
    public PublicKey getPublicKey(String kid) {
        return kid.equals(this.kid) ? keyPair.getPublic() : null;
    }

    @Override
    public List<EcdsaKeyMetadata> getKeyMetadata() {
        return Collections.emptyList();
    }

    @Override
    public void close() {
    }
}

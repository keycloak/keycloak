package org.keycloak.keys;

import java.security.KeyPair;
import java.util.Collections;
import java.util.List;

import org.jboss.logging.Logger;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyStatus;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;

// KEYCLOAK-7560 Refactoring Token Signing and Verifying by Token Signature SPI

public class FailsafeEcdsaKeyProvider implements KeyProvider {

    private static final Logger logger = Logger.getLogger(FailsafeEcdsaKeyProvider.class);

    private static KeyWrapper KEY;

    private static long EXPIRES;

    private KeyWrapper key;

    public FailsafeEcdsaKeyProvider() {
        logger.errorv("No active keys found, using failsafe provider, please login to admin console to add keys. Clustering is not supported.");

        synchronized (FailsafeEcdsaKeyProvider.class) {
            if (EXPIRES < Time.currentTime()) {
                KEY = createKeyWrapper();
                EXPIRES = Time.currentTime() + 60 * 10;

                if (EXPIRES > 0) {
                    logger.warnv("Keys expired, re-generated kid={0}", KEY.getKid());
                }
            }

            key = KEY;
        }
    }

    @Override
    public List<KeyWrapper> getKeys() {
        return Collections.singletonList(key);
    }

    private KeyWrapper createKeyWrapper() {
        // secp256r1,NIST P-256,X9.62 prime256v1,1.2.840.10045.3.1.7
        KeyPair keyPair = AbstractEcdsaKeyProviderFactory.generateEcdsaKeyPair("secp256r1");

        KeyWrapper key = new KeyWrapper();

        key.setKid(KeyUtils.createKeyId(keyPair.getPublic()));
        key.setUse(KeyUse.SIG);
        key.setType(KeyType.EC);
        key.setAlgorithms(Algorithm.ES256);
        key.setStatus(KeyStatus.ACTIVE);
        key.setSignKey(keyPair.getPrivate());
        key.setVerifyKey(keyPair.getPublic());

        return key;
    }
}

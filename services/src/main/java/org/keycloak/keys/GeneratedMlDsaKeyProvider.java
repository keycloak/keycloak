package org.keycloak.keys;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.stream.Stream;

import org.keycloak.common.util.KeyUtils;
import org.keycloak.component.ComponentModel;
import org.keycloak.crypto.KeyStatus;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;

import org.jboss.logging.Logger;

public class GeneratedMlDsaKeyProvider implements KeyProvider {

    private static final Logger logger = Logger.getLogger(GeneratedMlDsaKeyProvider.class);

    private final KeyStatus status;

    private final ComponentModel model;

    private final KeyWrapper key;

    public GeneratedMlDsaKeyProvider(ComponentModel model) {
        this.model = model;
        this.status = KeyStatus.from(model.get(Attributes.ACTIVE_KEY, true), model.get(Attributes.ENABLED_KEY, true));

        if (model.hasNote(KeyWrapper.class.getName())) {
            key = model.getNote(KeyWrapper.class.getName());
        } else {
            key = loadKey(model);
            model.setNote(KeyWrapper.class.getName(), key);
        }
    }

    protected KeyWrapper loadKey(ComponentModel model) {
        String privateMlDsaKeyBase64Encoded = model.getConfig().getFirst(Attributes.PRIVATE_KEY_KEY);
        String publicMlDsaKeyBase64Encoded = model.getConfig().getFirst(Attributes.PUBLIC_KEY_KEY);
        String algorithm = model.getConfig().getFirst(Attributes.ALGORITHM_KEY);

        try {
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(Base64.getMimeDecoder().decode(privateMlDsaKeyBase64Encoded));
            KeyFactory kf = KeyFactory.getInstance(algorithm);
            PrivateKey decodedPrivateKey = kf.generatePrivate(privateKeySpec);

            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.getMimeDecoder().decode(publicMlDsaKeyBase64Encoded));
            PublicKey decodedPublicKey = kf.generatePublic(publicKeySpec);

            KeyPair keyPair = new KeyPair(decodedPublicKey, decodedPrivateKey);

            return createKeyWrapper(keyPair, algorithm);
        } catch (Exception e) {
            logger.warnv(e,"Failed to load stored keys for realm={0}, componentId={1}", model.getParentId(), model.getId());
            return null;
        }
    }

    @Override
    public Stream<KeyWrapper> getKeysStream() {
        return Stream.of(key);
    }

    protected KeyWrapper createKeyWrapper(KeyPair keyPair, String algorithm) {
        KeyWrapper key = new KeyWrapper();

        key.setProviderId(model.getId());
        key.setProviderPriority(model.get("priority", 0L));

        key.setKid(KeyUtils.createKeyId(keyPair.getPublic()));
        key.setUse(KeyUse.SIG);
        key.setType(KeyType.AKP);
        key.setAlgorithm(algorithm);
        key.setStatus(status);
        key.setPrivateKey(keyPair.getPrivate());
        key.setPublicKey(keyPair.getPublic());

        return key;
    }

}

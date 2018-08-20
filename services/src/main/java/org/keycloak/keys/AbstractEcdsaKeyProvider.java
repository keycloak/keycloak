package org.keycloak.keys;

import java.security.KeyPair;
import java.util.Collections;
import java.util.List;

import org.keycloak.common.util.KeyUtils;
import org.keycloak.component.ComponentModel;
import org.keycloak.crypto.KeyStatus;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.models.RealmModel;

// KEYCLOAK-7560 Refactoring Token Signing and Verifying by Token Signature SPI

public abstract class AbstractEcdsaKeyProvider implements KeyProvider {

    private final KeyStatus status;

    private final ComponentModel model;

    private final KeyWrapper key;

    public AbstractEcdsaKeyProvider(RealmModel realm, ComponentModel model) {
        this.model = model;
        this.status = KeyStatus.from(model.get(Attributes.ACTIVE_KEY, true), model.get(Attributes.ENABLED_KEY, true));

        if (model.hasNote(KeyWrapper.class.getName())) {
            key = model.getNote(KeyWrapper.class.getName());
        } else {
            key = loadKey(realm, model);
            model.setNote(KeyWrapper.class.getName(), key);
        }
    }

    protected abstract KeyWrapper loadKey(RealmModel realm, ComponentModel model);

    @Override
    public List<KeyWrapper> getKeys() {
        return Collections.singletonList(key);
    }

    protected KeyWrapper createKeyWrapper(KeyPair keyPair, String ecInNistRep) {
        KeyWrapper key = new KeyWrapper();

        key.setProviderId(model.getId());
        key.setProviderPriority(model.get("priority", 0l));

        key.setKid(KeyUtils.createKeyId(keyPair.getPublic()));
        key.setUse(KeyUse.SIG);
        key.setType(KeyType.EC);
        key.setAlgorithms(AbstractEcdsaKeyProviderFactory.convertECDomainParmNistRepToAlgorithm(ecInNistRep));
        key.setStatus(status);
        key.setSignKey(keyPair.getPrivate());
        key.setVerifyKey(keyPair.getPublic());

        return key;
    }
}

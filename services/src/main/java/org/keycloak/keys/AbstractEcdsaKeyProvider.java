package org.keycloak.keys;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Collections;
import java.util.List;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.RealmModel;

// KEYCLOAK-6770 JWS signatures using PS256 or ES256 algorithms for signing
public abstract class AbstractEcdsaKeyProvider implements EcdsaKeyProvider {
    private final boolean enabled;

    private final boolean active;

    private final ComponentModel model;

    private final Keys keys;

    public AbstractEcdsaKeyProvider(RealmModel realm, ComponentModel model) {
        this.model = model;

        this.enabled = model.get(Attributes.ENABLED_KEY, true);
        this.active = model.get(Attributes.ACTIVE_KEY, true);

        if (model.hasNote(Keys.class.getName())) {
            keys = model.getNote(Keys.class.getName());
        } else {
            keys = loadKeys(realm, model);
            model.setNote(Keys.class.getName(), keys);
        }
    }

    protected abstract Keys loadKeys(RealmModel realm, ComponentModel model);

    @Override
    public final String getKid() {
        return isActive() ? keys.getKid() : null;
    }

    @Override
    public final PrivateKey getPrivateKey() {
        return isActive() ? keys.getKeyPair().getPrivate() : null;
    }

    @Override
    public final PublicKey getPublicKey(String kid) {
        return isEnabled() && kid.equals(keys.getKid()) ? keys.getKeyPair().getPublic() : null;
    }

    @Override
    public final List<EcdsaKeyMetadata> getKeyMetadata() {
        String kid = keys.getKid();
        PublicKey publicKey = keys.getKeyPair().getPublic();
        if (kid != null && publicKey != null) {
            EcdsaKeyMetadata k = new EcdsaKeyMetadata();
            k.setProviderId(model.getId());
            k.setProviderPriority(model.get(Attributes.PRIORITY_KEY, 0l));
            k.setKid(kid);
            if (isActive()) {
                k.setStatus(KeyMetadata.Status.ACTIVE);
            } else if (isEnabled()) {
                k.setStatus(KeyMetadata.Status.PASSIVE);
            } else {
                k.setStatus(KeyMetadata.Status.DISABLED);
            }
            k.setPublicKey(publicKey);
            return Collections.singletonList(k);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public void close() {
    }

    private boolean isEnabled() {
        return keys != null && enabled;
    }

    private boolean isActive() {
        return isEnabled() && active;
    }

    public static class Keys {
        private String kid;
        private KeyPair keyPair;

        public Keys(String kid, KeyPair keyPair) {
            this.kid = kid;
            this.keyPair = keyPair;
        }

        public String getKid() {
            return kid;
        }

        public KeyPair getKeyPair() {
            return keyPair;
        }
    }

}

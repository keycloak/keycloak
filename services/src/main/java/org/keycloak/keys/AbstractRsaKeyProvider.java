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
import org.keycloak.jose.jws.AlgorithmType;
import org.keycloak.models.RealmModel;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public abstract class AbstractRsaKeyProvider implements RsaKeyProvider {

    private final boolean enabled;

    private final boolean active;

    private final ComponentModel model;

    private final Keys keys;

    public AbstractRsaKeyProvider(RealmModel realm, ComponentModel model) {
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
    public X509Certificate getCertificate(String kid) {
        return isEnabled() && kid.equals(keys.getKid()) ? keys.getCertificate() : null;
    }

    @Override
    public final List<RsaKeyMetadata> getKeyMetadata() {
        String kid = keys.getKid();
        PublicKey publicKey = keys.getKeyPair().getPublic();
        if (kid != null && publicKey != null) {
            RsaKeyMetadata k = new RsaKeyMetadata();
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
            k.setCertificate(keys.getCertificate());
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
        private X509Certificate certificate;

        public Keys(String kid, KeyPair keyPair, X509Certificate certificate) {
            this.kid = kid;
            this.keyPair = keyPair;
            this.certificate = certificate;
        }

        public String getKid() {
            return kid;
        }

        public KeyPair getKeyPair() {
            return keyPair;
        }

        public X509Certificate getCertificate() {
            return certificate;
        }
    }

}

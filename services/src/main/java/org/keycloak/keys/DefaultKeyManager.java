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

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeyManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderFactory;

import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultKeyManager implements KeyManager {

    private static final Logger logger = Logger.getLogger(DefaultKeyManager.class);

    private final KeycloakSession session;
    private final Map<String, List<KeyProvider>> providersMap = new HashMap<>();

    public DefaultKeyManager(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public ActiveKey getActiveKey(RealmModel realm) {
        for (KeyProvider p : getProviders(realm)) {
            if (p.getKid() != null && p.getPrivateKey() != null) {
                if (logger.isTraceEnabled()) {
                    logger.tracev("Active key realm={0} kid={1}", realm.getName(), p.getKid());
                }
                String kid = p.getKid();
                return new ActiveKey(kid, p.getPrivateKey(), p.getPublicKey(kid), p.getCertificate(kid));
            }
        }
        throw new RuntimeException("Failed to get keys");
    }

    @Override
    public PublicKey getPublicKey(RealmModel realm, String kid) {
        if (kid == null) {
            logger.warnv("KID is null, can't find public key", realm.getName(), kid);
            return null;
        }

        for (KeyProvider p : getProviders(realm)) {
            PublicKey publicKey = p.getPublicKey(kid);
            if (publicKey != null) {
                if (logger.isTraceEnabled()) {
                    logger.tracev("Found public key realm={0} kid={1}", realm.getName(), kid);
                }
                return publicKey;
            }
        }
        if (logger.isTraceEnabled()) {
            logger.tracev("Failed to find public key realm={0} kid={1}", realm.getName(), kid);
        }
        return null;
    }

    @Override
    public Certificate getCertificate(RealmModel realm, String kid) {
        if (kid == null) {
            logger.warnv("KID is null, can't find public key", realm.getName(), kid);
            return null;
        }

        for (KeyProvider p : getProviders(realm)) {
            Certificate certificate = p.getCertificate(kid);
            if (certificate != null) {
                if (logger.isTraceEnabled()) {
                    logger.tracev("Found certificate realm={0} kid={1}", realm.getName(), kid);
                }
                return certificate;
            }
        }
        if (logger.isTraceEnabled()) {
            logger.tracev("Failed to find certificate realm={0} kid={1}", realm.getName(), kid);
        }
        return null;
    }

    @Override
    public List<KeyMetadata> getKeys(RealmModel realm, boolean includeDisabled) {
        List<KeyMetadata> keys = new LinkedList<>();
        for (KeyProvider p : getProviders(realm)) {
            if (includeDisabled) {
                keys.addAll(p.getKeyMetadata());
            } else {
                p.getKeyMetadata().stream().filter(k -> k.getStatus() != KeyMetadata.Status.DISABLED).forEach(k -> keys.add(k));
            }
        }
        return keys;
    }

    private List<KeyProvider> getProviders(RealmModel realm) {
        boolean active = false;
        List<KeyProvider> providers = providersMap.get(realm.getId());
        if (providers == null) {
            providers = new LinkedList<>();

            List<ComponentModel> components = new LinkedList<>(realm.getComponents(realm.getId(), KeyProvider.class.getName()));
            components.sort(new ProviderComparator());

            for (ComponentModel c : components) {
                try {
                    ProviderFactory<KeyProvider> f = session.getKeycloakSessionFactory().getProviderFactory(KeyProvider.class, c.getProviderId());
                    KeyProviderFactory factory = (KeyProviderFactory) f;
                    KeyProvider provider = factory.create(session, c);
                    session.enlistForClose(provider);
                    providers.add(provider);
                    if (!active && provider.getKid() != null && provider.getPrivateKey() != null) {
                        active = true;
                    }
                } catch (Throwable t) {
                    logger.errorv(t, "Failed to load provider {0}", c.getId());
                }
            }

            if (!active) {
                providers.add(new FailsafeRsaKeyProvider());
            }

            providersMap.put(realm.getId(), providers);
        }
        return providers;
    }

    private class ProviderComparator implements Comparator<ComponentModel> {

        @Override
        public int compare(ComponentModel o1, ComponentModel o2) {
            int i = Integer.compare(o2.get("priority", 0), o1.get("priority", 0));
            return i != 0 ? i : o1.getId().compareTo(o2.getId());
        }

    }
}

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

import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.crypto.SecretKey;

import org.keycloak.component.ComponentModel;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.models.KeyManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderFactory;

import org.jboss.logging.Logger;

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
    public KeyWrapper getActiveKey(RealmModel realm, KeyUse use, String algorithm) {
        KeyWrapper activeKey = getActiveKey(getProviders(realm), realm, use, algorithm);
        if (activeKey != null) {
            return activeKey;
        }

        logger.debugv("Failed to find active key for realm, trying fallback: realm={0} algorithm={1} use={2}",
                realm.getName(), algorithm, use.name());

        Optional<KeyProviderFactory> keyProviderFactory = session.getKeycloakSessionFactory()
                .getProviderFactoriesStream(KeyProvider.class)
                .map(KeyProviderFactory.class::cast)
                .filter(kf -> kf.createFallbackKeys(session, use, algorithm))
                .findFirst();
        if (keyProviderFactory.isPresent()) {
            providersMap.remove(realm.getId());
            List<KeyProvider> providers = getProviders(realm);
            activeKey = getActiveKey(providers, realm, use, algorithm);
            if (activeKey != null) {
                logger.infov("No keys found for realm={0} and algorithm={1} for use={2}. Generating keys.",
                        realm.getName(), algorithm, use.name());
                return activeKey;
            }
        }

        logger.errorv("Failed to create fallback key for realm: realm={0} algorithm={1} use={2}", realm.getName(), algorithm, use.name());
        throw new RuntimeException("Failed to find key: realm=" + realm.getName() + " algorithm=" + algorithm + " use=" + use.name());
    }

    private KeyWrapper getActiveKey(List<KeyProvider> providers, RealmModel realm, KeyUse use, String algorithm) {
        Consumer<KeyWrapper> loggerConsumer = key -> {
            if (logger.isTraceEnabled()) {
                logger.tracev("Active key found: realm={0} kid={1} algorithm={2} use={3}",
                        realm.getName(), key.getKid(), algorithm, use.name());
            }
        };

        for (KeyProvider p : providers) {
            Optional<KeyWrapper> keyWrapper = p.getKeysStream()
                    .filter(key -> key.getStatus().isActive() && matches(key, use, algorithm))
                    .peek(loggerConsumer)
                    .findFirst();
            if (keyWrapper.isPresent()) {
                return keyWrapper.get();
            }
        }
        return null;
    }

    @Override
    public KeyWrapper getKey(RealmModel realm, String kid, KeyUse use, String algorithm) {
        if (kid == null) {
            logger.warnv("kid is null, can't find public key: realm={0}", realm.getName());
            return null;
        }

        Consumer<KeyWrapper> loggerConsumer = key -> {
            if (logger.isTraceEnabled()) {
                logger.tracev("Found key: realm={0} kid={1} algorithm={2} use={3}",
                        realm.getName(), key.getKid(), algorithm, use.name());
            }
        };

        for (KeyProvider p : getProviders(realm)) {
            Optional<KeyWrapper> keyWrapper = p.getKeysStream()
                    .filter(key -> Objects.equals(key.getKid(), kid) && key.getStatus().isEnabled() && matches(key, use, algorithm))
                    .peek(loggerConsumer)
                    .findFirst();

            if (keyWrapper.isPresent()) {
                return keyWrapper.get();
            }
        }

        if (logger.isTraceEnabled()) {
            logger.tracev("Failed to find public key: realm={0} kid={1} algorithm={2} use={3}", realm.getName(), kid, algorithm, use.name());
        }

        return null;
    }

    @Override
    public Stream<KeyWrapper> getKeysStream(RealmModel realm, KeyUse use, String algorithm) {
        return getProviders(realm).stream()
                .flatMap(p -> p.getKeysStream()
                                .filter(key -> key.getStatus().isEnabled() && matches(key, use, algorithm)));
    }

    @Override
    public Stream<KeyWrapper> getKeysStream(RealmModel realm) {
        return getProviders(realm).stream().flatMap(KeyProvider::getKeysStream);
    }

    @Override
    @Deprecated
    public ActiveRsaKey getActiveRsaKey(RealmModel realm) {
        KeyWrapper key = getActiveKey(realm, KeyUse.SIG, Algorithm.RS256);
        return new ActiveRsaKey(key);
    }

    @Override
    @Deprecated
    public ActiveHmacKey getActiveHmacKey(RealmModel realm) {
        KeyWrapper key = getActiveKey(realm, KeyUse.SIG, Algorithm.HS256);
        return new ActiveHmacKey(key.getKid(), key.getSecretKey());
    }

    @Override
    @Deprecated
    public ActiveAesKey getActiveAesKey(RealmModel realm) {
        KeyWrapper key = getActiveKey(realm, KeyUse.ENC, Algorithm.AES);
        return new ActiveAesKey(key.getKid(), key.getSecretKey());
    }

    @Override
    @Deprecated
    public PublicKey getRsaPublicKey(RealmModel realm, String kid) {
        KeyWrapper key = getKey(realm, kid, KeyUse.SIG, Algorithm.RS256);
        return key != null ? (PublicKey) key.getPublicKey() : null;
    }

    @Override
    @Deprecated
    public Certificate getRsaCertificate(RealmModel realm, String kid) {
        KeyWrapper key = getKey(realm, kid, KeyUse.SIG, Algorithm.RS256);
        return key != null ? key.getCertificate() : null;
    }

    @Override
    @Deprecated
    public SecretKey getHmacSecretKey(RealmModel realm, String kid) {
        KeyWrapper key = getKey(realm, kid, KeyUse.SIG, Algorithm.HS256);
        return key != null ? key.getSecretKey() : null;
    }

    @Override
    @Deprecated
    public SecretKey getAesSecretKey(RealmModel realm, String kid) {
        KeyWrapper key = getKey(realm, kid, KeyUse.ENC, Algorithm.AES);
        return key.getSecretKey();
    }

    @Override
    @Deprecated
    public List<RsaKeyMetadata> getRsaKeys(RealmModel realm) {
        return getKeysStream(realm, KeyUse.SIG, Algorithm.RS256)
                .map(key -> {
                    RsaKeyMetadata m = new RsaKeyMetadata();
                    m.setCertificate(key.getCertificate());
                    m.setPublicKey((PublicKey) key.getPublicKey());
                    m.setKid(key.getKid());
                    m.setProviderId(key.getProviderId());
                    m.setProviderPriority(key.getProviderPriority());
                    m.setStatus(key.getStatus());
                    return m;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<SecretKeyMetadata> getHmacKeys(RealmModel realm) {
        return getKeysStream(realm, KeyUse.SIG, Algorithm.HS256)
                .map(key -> {
                    SecretKeyMetadata m = new SecretKeyMetadata();
                    m.setKid(key.getKid());
                    m.setProviderId(key.getProviderId());
                    m.setProviderPriority(key.getProviderPriority());
                    m.setStatus(key.getStatus());
                    return m;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<SecretKeyMetadata> getAesKeys(RealmModel realm) {
        return getKeysStream(realm, KeyUse.ENC, Algorithm.AES)
                .map(key -> {
                    SecretKeyMetadata m = new SecretKeyMetadata();
                    m.setKid(key.getKid());
                    m.setProviderId(key.getProviderId());
                    m.setProviderPriority(key.getProviderPriority());
                    m.setStatus(key.getStatus());
                    return m;
                })
                .collect(Collectors.toList());
    }

    private boolean matches(KeyWrapper key, KeyUse use, String algorithm) {
        return use.equals(key.getUse()) && key.getAlgorithmOrDefault().equals(algorithm);
    }

    private List<KeyProvider> getProviders(RealmModel realm) {
        List<KeyProvider> providers = providersMap.get(realm.getId());
        if (providers == null) {
            providers = realm.getComponentsStream(realm.getId(), KeyProvider.class.getName())
                    .sorted(new ProviderComparator())
                    .map(c -> {
                        try {
                            ProviderFactory<KeyProvider> f = session.getKeycloakSessionFactory().getProviderFactory(KeyProvider.class, c.getProviderId());
                            KeyProviderFactory factory = (KeyProviderFactory) f;
                            KeyProvider provider = factory.create(session, c);
                            session.enlistForClose(provider);
                            return provider;
                        } catch (Throwable t) {
                            logger.errorv(t, "Failed to load provider {0}", c.getId());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            providersMap.put(realm.getId(), providers);
        }
        return providers;
    }

    private static class ProviderComparator implements Comparator<ComponentModel> {

        @Override
        public int compare(ComponentModel o1, ComponentModel o2) {
            int i = Long.compare(o2.get("priority", 0l), o1.get("priority", 0l));
            return i != 0 ? i : o1.getId().compareTo(o2.getId());
        }

    }
}

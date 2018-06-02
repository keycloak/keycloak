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
import org.keycloak.jose.jws.Algorithm;
import org.keycloak.jose.jws.AlgorithmType;
import org.keycloak.models.KeyManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderFactory;

import javax.crypto.SecretKey;
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
    public ActiveRsaKey getActiveRsaKey(RealmModel realm) {
        for (KeyProvider p : getProviders(realm)) {
            if (p.getType().equals(AlgorithmType.RSA)) {
                RsaKeyProvider r = (RsaKeyProvider) p;
                if (r.getKid() != null && r.getPrivateKey() != null) {
                    if (logger.isTraceEnabled()) {
                        logger.tracev("Active key realm={0} kid={1}", realm.getName(), p.getKid());
                    }
                    String kid = p.getKid();
                    return new ActiveRsaKey(kid, r.getPrivateKey(), r.getPublicKey(kid), r.getCertificate(kid));
                }
            }
        }
        throw new RuntimeException("Failed to get RSA keys");
    }

    @Override
    public ActiveHmacKey getActiveHmacKey(RealmModel realm) {
        for (KeyProvider p : getProviders(realm)) {
            if (p.getType().equals(AlgorithmType.HMAC)) {
                HmacKeyProvider h = (HmacKeyProvider) p;
                if (h.getKid() != null && h.getSecretKey() != null) {
                    if (logger.isTraceEnabled()) {
                        logger.tracev("Active secret realm={0} kid={1}", realm.getName(), p.getKid());
                    }
                    String kid = p.getKid();
                    return new ActiveHmacKey(kid, h.getSecretKey());
                }
            }
        }
        throw new RuntimeException("Failed to get keys");
    }

    @Override
    public ActiveAesKey getActiveAesKey(RealmModel realm) {
        for (KeyProvider p : getProviders(realm)) {
            if (p.getType().equals(AlgorithmType.AES)) {
                AesKeyProvider h = (AesKeyProvider) p;
                if (h.getKid() != null && h.getSecretKey() != null) {
                    if (logger.isTraceEnabled()) {
                        logger.tracev("Active AES Key realm={0} kid={1}", realm.getName(), p.getKid());
                    }
                    String kid = p.getKid();
                    return new ActiveAesKey(kid, h.getSecretKey());
                }
            }
        }
        throw new RuntimeException("Failed to get keys");
    }

    @Override
    public PublicKey getRsaPublicKey(RealmModel realm, String kid) {
        if (kid == null) {
            logger.warnv("KID is null, can't find public key", realm.getName(), kid);
            return null;
        }

        for (KeyProvider p : getProviders(realm)) {

            // KEYCLOAK-6770 JWS signatures using PS256 or ES256 algorithms for signing
            dumpPublicKey(p);

            if (p.getType().equals(AlgorithmType.RSA)) {
                RsaKeyProvider r = (RsaKeyProvider) p;
                PublicKey publicKey = r.getPublicKey(kid);
                if (publicKey != null) {
                    if (logger.isTraceEnabled()) {
                        logger.tracev("Found public key realm={0} kid={1}", realm.getName(), kid);
                    }
                    return publicKey;
                }
            }
        }
        if (logger.isTraceEnabled()) {
            logger.tracev("Failed to find public key realm={0} kid={1}", realm.getName(), kid);
        }
        return null;
    }

    @Override
    public Certificate getRsaCertificate(RealmModel realm, String kid) {
        if (kid == null) {
            logger.warnv("KID is null, can't find public key", realm.getName(), kid);
            return null;
        }

        for (KeyProvider p : getProviders(realm)) {
            if (p.getType().equals(AlgorithmType.RSA)) {
                RsaKeyProvider r = (RsaKeyProvider) p;
                Certificate certificate = r.getCertificate(kid);
                if (certificate != null) {
                    if (logger.isTraceEnabled()) {
                        logger.tracev("Found certificate realm={0} kid={1}", realm.getName(), kid);
                    }
                    return certificate;
                }
            }
        }
        if (logger.isTraceEnabled()) {
            logger.tracev("Failed to find certificate realm={0} kid={1}", realm.getName(), kid);
        }
        return null;
    }

    @Override
    public SecretKey getHmacSecretKey(RealmModel realm, String kid) {
        if (kid == null) {
            logger.warnv("KID is null, can't find secret key", realm.getName(), kid);
            return null;
        }

        for (KeyProvider p : getProviders(realm)) {
            if (p.getType().equals(AlgorithmType.HMAC)) {
                HmacKeyProvider h = (HmacKeyProvider) p;
                SecretKey s = h.getSecretKey(kid);
                if (s != null) {
                    if (logger.isTraceEnabled()) {
                        logger.tracev("Found secret key realm={0} kid={1}", realm.getName(), kid);
                    }
                    return s;
                }
            }
        }
        if (logger.isTraceEnabled()) {
            logger.tracev("Failed to find secret key realm={0} kid={1}", realm.getName(), kid);
        }
        return null;
    }

    @Override
    public SecretKey getAesSecretKey(RealmModel realm, String kid) {
        if (kid == null) {
            logger.warnv("KID is null, can't find aes key", realm.getName(), kid);
            return null;
        }

        for (KeyProvider p : getProviders(realm)) {
            if (p.getType().equals(AlgorithmType.AES)) {
                AesKeyProvider h = (AesKeyProvider) p;
                SecretKey s = h.getSecretKey(kid);
                if (s != null) {
                    if (logger.isTraceEnabled()) {
                        logger.tracev("Found AES key realm={0} kid={1}", realm.getName(), kid);
                    }
                    return s;
                }
            }
        }
        if (logger.isTraceEnabled()) {
            logger.tracev("Failed to find AES key realm={0} kid={1}", realm.getName(), kid);
        }
        return null;
    }

    @Override
    public List<RsaKeyMetadata> getRsaKeys(RealmModel realm, boolean includeDisabled) {
        List<RsaKeyMetadata> keys = new LinkedList<>();
        for (KeyProvider p : getProviders(realm)) {
            if (p instanceof RsaKeyProvider) {
                if (includeDisabled) {
                    keys.addAll(p.getKeyMetadata());
                } else {
                    List<RsaKeyMetadata> metadata = p.getKeyMetadata();
                    metadata.stream().filter(k -> k.getStatus() != KeyMetadata.Status.DISABLED).forEach(k -> keys.add(k));
                }
            }
        }
        return keys;
    }

    @Override
    public List<SecretKeyMetadata> getHmacKeys(RealmModel realm, boolean includeDisabled) {
        List<SecretKeyMetadata> keys = new LinkedList<>();
        for (KeyProvider p : getProviders(realm)) {
            if (p instanceof HmacKeyProvider) {
                if (includeDisabled) {
                    keys.addAll(p.getKeyMetadata());
                } else {
                    List<SecretKeyMetadata> metadata = p.getKeyMetadata();
                    metadata.stream().filter(k -> k.getStatus() != KeyMetadata.Status.DISABLED).forEach(k -> keys.add(k));
                }
            }
        }
        return keys;
    }

    @Override
    public List<SecretKeyMetadata> getAesKeys(RealmModel realm, boolean includeDisabled) {
        List<SecretKeyMetadata> keys = new LinkedList<>();
        for (KeyProvider p : getProviders(realm)) {
            if (p instanceof AesKeyProvider) {
                if (includeDisabled) {
                    keys.addAll(p.getKeyMetadata());
                } else {
                    List<SecretKeyMetadata> metadata = p.getKeyMetadata();
                    metadata.stream().filter(k -> k.getStatus() != KeyMetadata.Status.DISABLED).forEach(k -> keys.add(k));
                }
            }
        }
        return keys;
    }

    private List<KeyProvider> getProviders(RealmModel realm) {
        List<KeyProvider> providers = providersMap.get(realm.getId());
        if (providers == null) {
            providers = new LinkedList<>();

            List<ComponentModel> components = new LinkedList<>(realm.getComponents(realm.getId(), KeyProvider.class.getName()));
            components.sort(new ProviderComparator());

            boolean activeRsa = false;
            boolean activeHmac = false;
            boolean activeAes = false;
            // KEYCLOAK-6770 JWS signatures using PS256 or ES256 algorithms for signing
            boolean activeEcdsa = false;

            for (ComponentModel c : components) {
                try {
                    ProviderFactory<KeyProvider> f = session.getKeycloakSessionFactory().getProviderFactory(KeyProvider.class, c.getProviderId());
                    KeyProviderFactory factory = (KeyProviderFactory) f;
                    KeyProvider provider = factory.create(session, c);
                    session.enlistForClose(provider);
                    providers.add(provider);
                    if (provider.getType().equals(AlgorithmType.RSA)) {
                        RsaKeyProvider r = (RsaKeyProvider) provider;
                        if (r.getKid() != null && r.getPrivateKey() != null) {
                            activeRsa = true;
                        }
                    } else if (provider.getType().equals(AlgorithmType.HMAC)) {
                        HmacKeyProvider r = (HmacKeyProvider) provider;
                        if (r.getKid() != null && r.getSecretKey() != null) {
                            activeHmac = true;
                        }
                    } else if (provider.getType().equals(AlgorithmType.AES)) {
                        AesKeyProvider r = (AesKeyProvider) provider;
                        if (r.getKid() != null && r.getSecretKey() != null) {
                            activeAes = true;
                        }
                    // KEYCLOAK-6770 JWS signatures using PS256 or ES256 algorithms for signing
                    } else if (provider.getType().equals(AlgorithmType.ECDSA)) {
                        EcdsaKeyProvider r = (EcdsaKeyProvider) provider;
                        if (r.getKid() != null && r.getPrivateKey() != null) {
                            activeEcdsa = true;
                        }
                    }
                } catch (Throwable t) {
                    logger.errorv(t, "Failed to load provider {0}", c.getId());
                }
            }

            if (!activeRsa) {
                providers.add(new FailsafeRsaKeyProvider());
            }

            if (!activeHmac) {
                providers.add(new FailsafeHmacKeyProvider());
            }

            if (!activeAes) {
                providers.add(new FailsafeAesKeyProvider());
            }

            // KEYCLOAK-6770 JWS signatures using PS256 or ES256 algorithms for signing
            if (!activeEcdsa) {
                providers.add(new FailsafeEcdsaKeyProvider());
            }

            providersMap.put(realm.getId(), providers);
        }
        return providers;
    }

    private class ProviderComparator implements Comparator<ComponentModel> {

        @Override
        public int compare(ComponentModel o1, ComponentModel o2) {
            int i = Long.compare(o2.get("priority", 0l), o1.get("priority", 0l));
            return i != 0 ? i : o1.getId().compareTo(o2.getId());
        }

    }

    // KEYCLOAK-6770 JWS signatures using PS256 or ES256 algorithms for signing
    @Override
    public ActiveEcdsaKey getActiveEcdsaKey(RealmModel realm) {
        for (KeyProvider p : getProviders(realm)) {
            if (p.getType().equals(AlgorithmType.ECDSA)) {
                EcdsaKeyProvider r = (EcdsaKeyProvider) p;
                if (r.getKid() != null && r.getPrivateKey() != null) {
                    if (logger.isTraceEnabled()) {
                        logger.tracev("Active key realm={0} kid={1}", realm.getName(), p.getKid());
                    }
                    String kid = p.getKid();
                    return new ActiveEcdsaKey(kid, r.getPrivateKey(), r.getPublicKey(kid));
                }
            }
        }
        throw new RuntimeException("Failed to get ECDSA keys");
    }

    // KEYCLOAK-6770 JWS signatures using PS256 or ES256 algorithms for signing
    @Override
    public PublicKey getEcdsaPublicKey(RealmModel realm, String kid) {
        if (kid == null) {
            logger.warnv("KID is null, can't find public key", realm.getName(), kid);
            return null;
        }

        for (KeyProvider p : getProviders(realm)) {

            dumpPublicKey(p);

            if (p.getType().equals(AlgorithmType.ECDSA)) {
                EcdsaKeyProvider r = (EcdsaKeyProvider) p;
                PublicKey publicKey = r.getPublicKey(kid);
                if (publicKey != null) {
                    if (logger.isTraceEnabled()) {
                        logger.tracev("Found public key realm={0} kid={1}", realm.getName(), kid);
                    }
                    return publicKey;
                }
            }
        }
        if (logger.isTraceEnabled()) {
            logger.tracev("Failed to find public key realm={0} kid={1}", realm.getName(), kid);
        }
        return null;
    }
    private void dumpPublicKey(KeyProvider p) {
        logger.debugf("p = %s", p);
        logger.debugf("p.getKid() = %s", p.getKid());
        logger.debugf("p.getType() = %s", p.getType());
        logger.debugf("p.getKeyMetadata() = %s", p.getKeyMetadata());
    }
    // KEYCLOAK-6770 JWS signatures using PS256 or ES256 algorithms for signing
    @Override
    public List<EcdsaKeyMetadata> getEcdsaKeys(RealmModel realm, boolean includeDisabled) {
        List<EcdsaKeyMetadata> keys = new LinkedList<>();
        for (KeyProvider p : getProviders(realm)) {
            if (p instanceof EcdsaKeyProvider) {
                if (includeDisabled) {
                    keys.addAll(p.getKeyMetadata());
                } else {
                    List<EcdsaKeyMetadata> metadata = p.getKeyMetadata();
                    metadata.stream().filter(k -> k.getStatus() != KeyMetadata.Status.DISABLED).forEach(k -> keys.add(k));
                }
            }
        }
        return keys;
    }

    // KEYCLOAK-6770 JWS signatures using PS256 or ES256 algorithms for signing
    @Override
    public ActivePublicKeyCryptographyKey getActivePublicKeyCryptographyKey(RealmModel realm, Algorithm jwsAlgorithm) {
        ActivePublicKeyCryptographyKey activePublicKeyCryptographyKey = null;
        AlgorithmType algorithmType = jwsAlgorithm.getType();
        if (AlgorithmType.RSA.equals(algorithmType)) {
            activePublicKeyCryptographyKey = getActiveRsaKey(realm);
        } else if (AlgorithmType.ECDSA.equals(algorithmType)) {
            activePublicKeyCryptographyKey = getActiveEcdsaKey(realm);
        } else {
            // error
        }
        return activePublicKeyCryptographyKey;
    }

    // KEYCLOAK-6770 JWS signatures using PS256 or ES256 algorithms for signing
    @Override
    public PublicKey getActivePublicKey(RealmModel realm, AlgorithmType algorithmType) {
        PublicKey publicKey = null;
        if (AlgorithmType.RSA.equals(algorithmType)) {
            publicKey = getActiveRsaKey(realm).getPublicKey();
        } else if (AlgorithmType.ECDSA.equals(algorithmType)) {
            publicKey = getActiveEcdsaKey(realm).getPublicKey();
        } else {
            // error
        }
        return publicKey;
    }

    // KEYCLOAK-6770 JWS signatures using PS256 or ES256 algorithms for signing
    @Override
    public PublicKey getPublicKey(RealmModel realm, AlgorithmType algorithmType, String kid) {
        PublicKey publicKey = null;
        if (AlgorithmType.RSA.equals(algorithmType)) {
            publicKey = getRsaPublicKey(realm, kid);
        } else if (AlgorithmType.ECDSA.equals(algorithmType)) {
            publicKey = getEcdsaPublicKey(realm, kid);
        } else {
        }
        return publicKey;
    }
}

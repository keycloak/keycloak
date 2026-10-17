/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.security.KeyPairGenerator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyStatus;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jwk.AKPPublicJWK;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.utils.JWKSServerUtils;
import org.keycloak.utils.AbstractUtilSessionTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GeneratedMLDSAKeyProviderTest extends AbstractUtilSessionTest {

    private static final List<String> ALGORITHMS = List.of(
            Algorithm.ML_DSA_44, Algorithm.ML_DSA_65, Algorithm.ML_DSA_87);

    @Test
    public void generateAndReloadAllParameterSets() {
        GeneratedMLDSAKeyProviderFactory factory = new GeneratedMLDSAKeyProviderFactory();
        RealmModel realm = realm(null);

        for (String algorithm : ALGORITHMS) {
            ComponentModel model = model(algorithm);
            factory.validateConfiguration(null, realm, model);
            String privateKey = model.get(GeneratedMLDSAKeyProviderFactory.MLDSA_PRIVATE_KEY_KEY);
            String publicKey = model.get(GeneratedMLDSAKeyProviderFactory.MLDSA_PUBLIC_KEY_KEY);

            KeyWrapper first = new GeneratedMLDSAKeyProvider(realm, model).getKeysStream().findFirst().orElseThrow();
            KeyWrapper reloaded = new GeneratedMLDSAKeyProvider(realm, model).getKeysStream().findFirst().orElseThrow();

            assertEquals(privateKey, model.get(GeneratedMLDSAKeyProviderFactory.MLDSA_PRIVATE_KEY_KEY));
            assertEquals(publicKey, model.get(GeneratedMLDSAKeyProviderFactory.MLDSA_PUBLIC_KEY_KEY));
            assertEquals(first.getKid(), reloaded.getKid());
            assertEquals(KeyType.AKP, first.getType());
            assertEquals(KeyUse.SIG, first.getUse());
            assertEquals(KeyStatus.ACTIVE, first.getStatus());
            assertEquals(algorithm, first.getAlgorithm());
            assertNotNull(first.getPrivateKey());
            assertNotNull(first.getPublicKey());
        }
    }

    @Test
    public void regenerateWhenAlgorithmChanges() {
        GeneratedMLDSAKeyProviderFactory factory = new GeneratedMLDSAKeyProviderFactory();
        RealmModel realm = realm(null);
        ComponentModel model = model(Algorithm.ML_DSA_44);
        factory.validateConfiguration(null, realm, model);
        KeyWrapper oldKey = new GeneratedMLDSAKeyProvider(realm, model).getKeysStream().findFirst().orElseThrow();
        String oldPrivateKey = model.get(GeneratedMLDSAKeyProviderFactory.MLDSA_PRIVATE_KEY_KEY);

        model.put(GeneratedMLDSAKeyProviderFactory.MLDSA_ALGORITHM_KEY, Algorithm.ML_DSA_65);
        factory.validateConfiguration(null, realm, model);
        KeyWrapper newKey = new GeneratedMLDSAKeyProvider(realm, model).getKeysStream().findFirst().orElseThrow();

        assertNotEquals(oldPrivateKey, model.get(GeneratedMLDSAKeyProviderFactory.MLDSA_PRIVATE_KEY_KEY));
        assertNotEquals(oldKey.getKid(), newKey.getKid());
        assertEquals(Algorithm.ML_DSA_65, newKey.getAlgorithm());
    }

    @Test
    public void createFallbackForEachParameterSet() {
        GeneratedMLDSAKeyProviderFactory factory = new GeneratedMLDSAKeyProviderFactory();

        for (String algorithm : ALGORITHMS) {
            AtomicReference<ComponentModel> added = new AtomicReference<>();
            RealmModel realm = realm(added);

            assertTrue(factory.createFallbackKeys(keySession(realm), KeyUse.SIG, algorithm));
            assertEquals(GeneratedMLDSAKeyProviderFactory.ID, added.get().getProviderId());
            assertEquals("-100", added.get().get(Attributes.PRIORITY_KEY));
            assertEquals(algorithm, added.get().get(GeneratedMLDSAKeyProviderFactory.MLDSA_ALGORITHM_KEY));
        }

        assertFalse(factory.createFallbackKeys(keySession(realm(null)), KeyUse.ENC, Algorithm.ML_DSA_44));
    }

    @Test
    public void rejectUnsupportedAlgorithm() {
        ComponentModel model = model("ML-DSA-99");

        assertThrows(ComponentValidationException.class,
                () -> new GeneratedMLDSAKeyProviderFactory().validateConfiguration(null, realm(null), model));
    }

    @Test
    public void disableWhenKeyGenerationUnavailable() {
        CryptoProvider original = CryptoIntegration.getProvider();
        CryptoProvider noKeyGeneration = delegateWithoutKeyGeneration(original);

        try {
            CryptoIntegration.setProvider(noKeyGeneration);
            assertFalse(new GeneratedMLDSAKeyProviderFactory().isSupported(null));
        } finally {
            CryptoIntegration.setProvider(original);
        }
    }

    @Test
    public void publishPublicAkpWithoutPrivateSeed() {
        GeneratedMLDSAKeyProviderFactory factory = new GeneratedMLDSAKeyProviderFactory();
        RealmModel realm = realm(null);
        ComponentModel model = model(Algorithm.ML_DSA_44);
        factory.validateConfiguration(null, realm, model);
        KeyWrapper key = new GeneratedMLDSAKeyProvider(realm, model).getKeysStream().findFirst().orElseThrow();

        JWK jwk = JWKSServerUtils.toJwk(key);

        assertEquals(KeyType.AKP, jwk.getKeyType());
        assertEquals(Algorithm.ML_DSA_44, jwk.getAlgorithm());
        assertNotNull(jwk.getOtherClaim(AKPPublicJWK.PUB, String.class));
        assertNull(jwk.getOtherClaim("priv", String.class));
    }

    private static ComponentModel model(String algorithm) {
        ComponentModel model = new ComponentModel();
        model.setId("provider");
        model.setName("provider");
        model.put(Attributes.ACTIVE_KEY, "true");
        model.put(Attributes.ENABLED_KEY, "true");
        model.put(GeneratedMLDSAKeyProviderFactory.MLDSA_ALGORITHM_KEY, algorithm);
        return model;
    }

    private static RealmModel realm(AtomicReference<ComponentModel> added) {
        return proxy(RealmModel.class, (method, args) -> {
            if ("getId".equals(method)) {
                return "realm";
            }
            if ("getName".equals(method)) {
                return "realm";
            }
            if ("addComponentModel".equals(method)) {
                if (added != null) {
                    added.set((ComponentModel) args[0]);
                }
                return args[0];
            }
            return null;
        });
    }

    private static KeycloakSession keySession(RealmModel realm) {
        KeycloakContext context = proxy(KeycloakContext.class,
                (method, args) -> "getRealm".equals(method) ? realm : null);
        return proxy(KeycloakSession.class,
                (method, args) -> "getContext".equals(method) ? context : null);
    }

    private static CryptoProvider delegateWithoutKeyGeneration(CryptoProvider delegate) {
        return (CryptoProvider) Proxy.newProxyInstance(CryptoProvider.class.getClassLoader(),
                new Class<?>[] { CryptoProvider.class }, (proxy, method, args) -> {
            if ("getKeyPairGen".equals(method.getName())) {
                throw new java.security.NoSuchAlgorithmException();
            }
            try {
                return method.invoke(delegate, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        });
    }

    private static <T> T proxy(Class<T> type, Invocation invocation) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type },
                (proxy, method, args) -> invocation.invoke(method.getName(), args)));
    }

    private interface Invocation {
        Object invoke(String method, Object[] args) throws Throwable;
    }
}

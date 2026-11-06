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

package org.keycloak.crypto;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;

import org.keycloak.common.VerificationException;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.Base64Url;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.keys.PublicKeyStorageProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.utils.AbstractUtilSessionTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MLDSAClientSignatureVerifierProviderTest extends AbstractUtilSessionTest {

    @Test
    public void verifyConfiguredAkpKey() throws Exception {
        KeyWrapper signingKey = generateKey(Algorithm.ML_DSA_44);
        KeyWrapper publicKey = publicKey(signingKey);
        JWSInput input = input(Algorithm.ML_DSA_44);
        byte[] data = input.getEncodedSignatureInput().getBytes(StandardCharsets.UTF_8);
        byte[] signature = new MLDSASignatureProvider(session, Algorithm.ML_DSA_44).signer(signingKey).sign(data);

        SignatureVerifierContext verifier = new MLDSAClientSignatureVerifierProvider(keySession(publicKey), Algorithm.ML_DSA_44)
                .verifier(client(), input);

        assertTrue(verifier.verify(data, signature));
    }

    @Test
    public void defaultMissingJwkAlgFromHeader() throws Exception {
        KeyWrapper key = publicKey(generateKey(Algorithm.ML_DSA_44));
        key.setAlgorithm(null);

        SignatureVerifierContext verifier = new MLDSAClientSignatureVerifierProvider(keySession(key), Algorithm.ML_DSA_44)
                .verifier(client(), input(Algorithm.ML_DSA_44));

        assertEquals(Algorithm.ML_DSA_44, verifier.getAlgorithm());
    }

    @Test
    public void rejectNonAkpKey() throws Exception {
        KeyWrapper key = publicKey(generateKey(Algorithm.ML_DSA_44));
        key.setType(KeyType.RSA);

        assertThrows(VerificationException.class, () -> new MLDSAClientSignatureVerifierProvider(keySession(key), Algorithm.ML_DSA_44)
                .verifier(client(), input(Algorithm.ML_DSA_44)));
    }

    @Test
    public void rejectJwkHeaderAlgorithmMismatch() throws Exception {
        KeyWrapper key = publicKey(generateKey(Algorithm.ML_DSA_65));

        assertThrows(VerificationException.class, () -> new MLDSAClientSignatureVerifierProvider(keySession(key), Algorithm.ML_DSA_44)
                .verifier(client(), input(Algorithm.ML_DSA_44)));
    }

    @Test
    public void rejectCrossParameterKey() throws Exception {
        KeyWrapper key = publicKey(generateKey(Algorithm.ML_DSA_65));
        key.setAlgorithm(Algorithm.ML_DSA_44);

        assertThrows(VerificationException.class, () -> new MLDSAClientSignatureVerifierProvider(keySession(key), Algorithm.ML_DSA_44)
                .verifier(client(), input(Algorithm.ML_DSA_44)));
    }

    @Test
    public void rejectProviderHeaderMismatch() throws Exception {
        KeyWrapper key = publicKey(generateKey(Algorithm.ML_DSA_65));

        assertThrows(VerificationException.class, () -> new MLDSAClientSignatureVerifierProvider(keySession(key), Algorithm.ML_DSA_44)
                .verifier(client(), input(Algorithm.ML_DSA_65)));
    }

    @Test
    public void rejectUnknownClientKey() throws Exception {
        assertThrows(VerificationException.class, () -> new MLDSAClientSignatureVerifierProvider(keySession(null), Algorithm.ML_DSA_44)
                .verifier(client(), input(Algorithm.ML_DSA_44)));
    }

    @Test
    public void registerFactoriesWhenSupported() {
        assertNotNull(session.getProvider(ClientSignatureVerifierProvider.class, Algorithm.ML_DSA_44));
        assertNotNull(session.getProvider(ClientSignatureVerifierProvider.class, Algorithm.ML_DSA_65));
        assertNotNull(session.getProvider(ClientSignatureVerifierProvider.class, Algorithm.ML_DSA_87));
    }

    private static KeyWrapper generateKey(String algorithm) throws Exception {
        KeyPair keyPair = CryptoIntegration.getProvider().getKeyPairGen(algorithm).generateKeyPair();
        KeyWrapper key = new KeyWrapper();
        key.setKid("kid");
        key.setUse(KeyUse.SIG);
        key.setType(KeyType.AKP);
        key.setAlgorithm(algorithm);
        key.setPublicKey(keyPair.getPublic());
        key.setPrivateKey(keyPair.getPrivate());
        return key;
    }

    private static KeyWrapper publicKey(KeyWrapper key) {
        KeyWrapper publicKey = key.cloneKey();
        publicKey.setPrivateKey(null);
        return publicKey;
    }

    private static JWSInput input(String algorithm) throws Exception {
        String header = Base64Url.encode(("{\"alg\":\"" + algorithm + "\",\"kid\":\"kid\"}")
                .getBytes(StandardCharsets.UTF_8));
        return new JWSInput(header + ".e30.AA");
    }

    private static ClientModel client() {
        RealmModel realm = proxy(RealmModel.class, (method, args) -> "getId".equals(method) ? "realm" : null);
        return proxy(ClientModel.class, (method, args) -> {
            if ("getRealm".equals(method)) {
                return realm;
            }
            return "getId".equals(method) ? "client" : null;
        });
    }

    private static KeycloakSession keySession(KeyWrapper key) {
        PublicKeyStorageProvider storage = proxy(PublicKeyStorageProvider.class,
                (method, args) -> "getPublicKey".equals(method) ? key : null);
        return proxy(KeycloakSession.class, (method, args) -> {
            if ("getProvider".equals(method) && args != null && args.length > 0 && args[0] == PublicKeyStorageProvider.class) {
                return storage;
            }
            return null;
        });
    }

    private static <T> T proxy(Class<T> type, Invocation invocation) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type },
                (proxy, method, args) -> invocation.invoke(method.getName(), args)));
    }

    private interface Invocation {
        Object invoke(String method, Object[] args);
    }
}

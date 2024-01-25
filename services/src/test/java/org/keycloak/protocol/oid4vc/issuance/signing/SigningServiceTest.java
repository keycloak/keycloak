/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oid4vc.issuance.signing;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.models.KeyManager;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oid4vc.issuance.TimeProvider;
import org.keycloak.protocol.oid4vc.model.CredentialSubject;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;

import java.net.URI;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Super class for all signing service tests. Provides convenience methods to ease the testing.
 */
public abstract class SigningServiceTest {

    protected static final String CONTEXT_URL = "https://www.w3.org/2018/credentials/v1";
    protected static final URI TEST_DID = URI.create("did:web:test.org");
    protected static final List<String> TEST_TYPES = List.of("VerifiableCredential");
    protected static final Date TEST_EXPIRATION_DATE = Date.from(Instant.ofEpochSecond(2000));
    protected static final Date TEST_ISSUANCE_DATE = Date.from(Instant.ofEpochSecond(1000));

    protected CredentialSubject getCredentialSubject(Map<String, Object> claims) {
        CredentialSubject credentialSubject = new CredentialSubject();
        claims.forEach(credentialSubject::setClaims);
        return credentialSubject;
    }

    protected VerifiableCredential getTestCredential(Map<String, Object> claims) {

        VerifiableCredential testCredential = new VerifiableCredential();
        testCredential.setId(URI.create(String.format("uri:uuid:%s", UUID.randomUUID())));
        testCredential.setContext(List.of(CONTEXT_URL));
        testCredential.setType(TEST_TYPES);
        testCredential.setIssuer(TEST_DID);
        testCredential.setExpirationDate(TEST_EXPIRATION_DATE);
        testCredential.setIssuanceDate(TEST_ISSUANCE_DATE);
        testCredential.setCredentialSubject(getCredentialSubject(claims));
        return testCredential;
    }


    public static KeyWrapper getECKey(String keyId) {
        try {
            Security.addProvider(new BouncyCastleProvider());
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", "BC");
            kpg.initialize(256);
            var keyPair = kpg.generateKeyPair();
            KeyWrapper kw = new KeyWrapper();
            kw.setPrivateKey(keyPair.getPrivate());
            kw.setPublicKey(keyPair.getPublic());
            kw.setUse(KeyUse.SIG);
            kw.setKid(keyId);
            kw.setType("EC");
            kw.setAlgorithm("ES256");
            return kw;

        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
    }

    public static KeyWrapper getEd25519Key(String keyId) {
        try {
            Security.addProvider(new BouncyCastleProvider());
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519", "BC");
            var keyPair = kpg.generateKeyPair();
            KeyWrapper kw = new KeyWrapper();
            kw.setPrivateKey(keyPair.getPrivate());
            kw.setPublicKey(keyPair.getPublic());
            kw.setUse(KeyUse.SIG);
            kw.setKid(keyId);
            kw.setType("Ed25519");
            return kw;

        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
    }


    public static KeyWrapper getRsaKey(String keyId) {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            var keyPair = kpg.generateKeyPair();
            KeyWrapper kw = new KeyWrapper();
            kw.setPrivateKey(keyPair.getPrivate());
            kw.setPublicKey(keyPair.getPublic());
            kw.setUse(KeyUse.SIG);
            kw.setType("RSA");
            kw.setKid(keyId);
            return kw;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static KeycloakSession getMockSession(KeyWrapper keyWrapper) {

        KeycloakSession session = mock(KeycloakSession.class);
        KeycloakContext context = mock(KeycloakContext.class);
        KeyManager keyManager = mock(KeyManager.class);
        RealmModel realmModel = mock(RealmModel.class);
        when(session.keys()).thenReturn(keyManager);
        when(session.getContext()).thenReturn(context);
        when(context.getRealm()).thenReturn(realmModel);
        when(keyManager.getKey(any(), eq(keyWrapper.getKid()), any(), anyString())).thenReturn(keyWrapper);
        return session;
    }

    class StaticTimeProvider implements TimeProvider {
        private final int currentTimeInS;

        StaticTimeProvider(int currentTimeInS) {
            this.currentTimeInS = currentTimeInS;
        }

        @Override
        public int currentTime() {
            return currentTimeInS;
        }

        @Override
        public long currentTimeMillis() {
            return currentTimeInS * 1000L;
        }
    }

}
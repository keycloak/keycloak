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

package org.keycloak.testsuite.oid4vc.issuance.signing;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jboss.logging.Logger;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.protocol.oid4vc.issuance.TimeProvider;
import org.keycloak.protocol.oid4vc.model.CredentialSubject;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;

import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Super class for all signing service tests. Provides convenience methods to ease the testing.
 */
public abstract class SigningServiceTest extends AbstractTestRealmKeycloakTest {

    private static final Logger LOGGER = Logger.getLogger(SigningServiceTest.class);
    protected static final String CONTEXT_URL = "https://www.w3.org/2018/credentials/v1";
    protected static final URI TEST_DID = URI.create("did:web:test.org");
    protected static final List<String> TEST_TYPES = List.of("VerifiableCredential");
    protected static final Date TEST_EXPIRATION_DATE = Date.from(Instant.ofEpochSecond(2000));
    protected static final Date TEST_ISSUANCE_DATE = Date.from(Instant.ofEpochSecond(1000));

    protected static CredentialSubject getCredentialSubject(Map<String, Object> claims) {
        CredentialSubject credentialSubject = new CredentialSubject();
        claims.forEach(credentialSubject::setClaims);
        return credentialSubject;
    }

    protected static VerifiableCredential getTestCredential(Map<String, Object> claims) {

        VerifiableCredential testCredential = new VerifiableCredential();
        testCredential.setId(URI.create(String.format("uri:uuid:%s", UUID.randomUUID())));
        testCredential.setContext(List.of(CONTEXT_URL));
        testCredential.setType(TEST_TYPES);
        testCredential.setIssuer(TEST_DID);
        testCredential.setExpirationDate(TEST_EXPIRATION_DATE);
        if (claims.containsKey("issuanceDate")) {
            testCredential.setIssuanceDate((Date) claims.get("issuanceDate"));
        }

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


    public static KeyWrapper getRsaKey() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            var keyPair = kpg.generateKeyPair();
            KeyWrapper kw = new KeyWrapper();
            kw.setPrivateKey(keyPair.getPrivate());
            kw.setPublicKey(keyPair.getPublic());
            kw.setUse(KeyUse.SIG);
            kw.setKid(KeyUtils.createKeyId(keyPair.getPublic()));
            kw.setType("RSA");
            kw.setAlgorithm("RS256");
            return kw;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    protected ComponentExportRepresentation getRsaKeyProvider(KeyWrapper keyWrapper) {
        ComponentExportRepresentation componentExportRepresentation = new ComponentExportRepresentation();
        componentExportRepresentation.setName("rsa-key-provider");
        componentExportRepresentation.setId(UUID.randomUUID().toString());
        componentExportRepresentation.setProviderId("rsa");


        Certificate certificate = CertificateUtils.generateV1SelfSignedCertificate(
                new KeyPair((PublicKey) keyWrapper.getPublicKey(), (PrivateKey) keyWrapper.getPrivateKey()), "TestKey");

        componentExportRepresentation.setConfig(new MultivaluedHashMap<>(
                Map.of(
                        "privateKey", List.of(PemUtils.encodeKey(keyWrapper.getPrivateKey())),
                        "certificate", List.of(PemUtils.encodeCertificate(certificate)),
                        "active", List.of("true"),
                        "priority", List.of("0"),
                        "enabled", List.of("true"),
                        "algorithm", List.of("RS256")
                )
        ));
        return componentExportRepresentation;
    }

    static class StaticTimeProvider implements TimeProvider {
        private final int currentTimeInS;

        StaticTimeProvider(int currentTimeInS) {
            this.currentTimeInS = currentTimeInS;
        }

        @Override
        public int currentTimeSeconds() {
            return currentTimeInS;
        }

        @Override
        public long currentTimeMillis() {
            return currentTimeInS * 1000L;
        }
    }

}
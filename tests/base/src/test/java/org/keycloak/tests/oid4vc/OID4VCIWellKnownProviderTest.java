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
package org.keycloak.tests.oid4vc;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.keycloak.admin.client.resource.ComponentsResource;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.PemUtils;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.keys.KeyProvider;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialResponseEncryptionMetadata;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;

import org.junit.jupiter.api.Test;

import static org.keycloak.common.crypto.CryptoConstants.A128KW;
import static org.keycloak.common.crypto.CryptoConstants.RSA_OAEP;
import static org.keycloak.jose.jwe.JWEConstants.RSA_OAEP_256;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class OID4VCIWellKnownProviderTest extends OID4VCIssuerTestBase {

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    boolean configuredAlready;

    @TestSetup
    public void configureTestRealm() {

        // When named differently this method is called before OID4VCIssuerTestBase.configureTestRealm()
        // When named the same name, it is called twice (i.e. does not override)
        //
        // [TODO] IntegrationTest sub class @TestSetup called before super class
        // https://github.com/keycloak/keycloak/issues/46667

        if (!configuredAlready) {
            super.configureTestRealm();

            ComponentsResource components = testRealm.admin().components();
            components.add(getRsaKeyProvider(getRsaKey_Default())).close();
            components.add(getRsaEncKeyProvider(RSA_OAEP_256, "enc-key-oaep256", 100)).close();
            components.add(getAesKeyProvider(A128KW, "aes-enc", "ENC", "aes-generated")).close();
            components.add(getAesKeyProvider(Algorithm.HS256, "aes-sig", "SIG", "hmac-generated")).close();

            configuredAlready = true;
        }
    }

    @Test
    public void assertOnlyAsymmetricIncluded() {

        // Server-side debugging: KC_TEST_SERVER = embedded
        //
        runOnServer.run(session -> {
            CredentialIssuer credentialIssuer = new OID4VCIssuerWellKnownProvider(session).getIssuerMetadata();
            CredentialResponseEncryptionMetadata credentialResponseEncryption = credentialIssuer.getCredentialResponseEncryption();
            List<String> algValuesSupported = credentialResponseEncryption.getAlgValuesSupported();
            assertEquals(2, algValuesSupported.size(), "Two asymmetric encryption are present.");
            assertTrue(algValuesSupported.contains(RSA_OAEP), "The default algorithm for asymmetric encryption should be available as well.");
            assertTrue(algValuesSupported.contains(RSA_OAEP_256), "The algorithm of the configured asymmetric encryption key should be provided.");
        });
    }

    ComponentRepresentation getAesKeyProvider(String algorithm, String keyName, String keyUse, String providerId) {
        // Generate a random AES key (default length: 256 bits)
        byte[] secret = SecretGenerator.getInstance().randomBytes(32); // 32 bytes = 256 bits

        String secretBase64 = Base64.getEncoder().encodeToString(secret);

        ComponentRepresentation component = new ComponentRepresentation();
        component.setProviderType(KeyProvider.class.getName());
        component.setName(keyName);
        component.setId(UUID.randomUUID().toString());
        component.setProviderId(providerId);

        component.setConfig(new MultivaluedHashMap<>(
                Map.of(
                        "secret", List.of(secretBase64),
                        "active", List.of("true"),
                        "priority", List.of(String.valueOf(100)),
                        "enabled", List.of("true"),
                        "algorithm", List.of(algorithm),
                        "keyUse", List.of(keyUse) // encryption usage
                )
        ));
        return component;
    }

    ComponentRepresentation getRsaKeyProvider(KeyWrapper keyWrapper) {
        ComponentRepresentation component = new ComponentRepresentation();
        component.setProviderType(KeyProvider.class.getName());
        component.setName("rsa-key-provider");
        component.setId(UUID.randomUUID().toString());
        component.setProviderId("rsa");

        Certificate certificate = CertificateUtils.generateV1SelfSignedCertificate(
                new KeyPair((PublicKey) keyWrapper.getPublicKey(), (PrivateKey) keyWrapper.getPrivateKey()), "TestKey");

        component.setConfig(new MultivaluedHashMap<>(
                Map.of(
                        "privateKey", List.of(PemUtils.encodeKey(keyWrapper.getPrivateKey())),
                        "certificate", List.of(PemUtils.encodeCertificate(certificate)),
                        "active", List.of("true"),
                        "priority", List.of("0"),
                        "enabled", List.of("true"),
                        "algorithm", List.of(keyWrapper.getAlgorithm()),
                        "keyUse", List.of(keyWrapper.getUse().name())
                )
        ));
        return component;
    }

    ComponentRepresentation getRsaEncKeyProvider(String algorithm, String keyName, int priority) {
        ComponentRepresentation component = new ComponentRepresentation();
        component.setProviderType(KeyProvider.class.getName());
        component.setName(keyName);
        component.setId(UUID.randomUUID().toString());
        component.setProviderId("rsa");

        KeyWrapper keyWrapper = getRsaKey(KeyUse.ENC, algorithm, keyName);
        Certificate certificate = CertificateUtils.generateV1SelfSignedCertificate(
                new KeyPair((PublicKey) keyWrapper.getPublicKey(), (PrivateKey) keyWrapper.getPrivateKey()), "TestKey");

        component.setConfig(new MultivaluedHashMap<>(
                Map.of(
                        "privateKey", List.of(PemUtils.encodeKey(keyWrapper.getPrivateKey())),
                        "certificate", List.of(PemUtils.encodeCertificate(certificate)),
                        "active", List.of("true"),
                        "priority", List.of(String.valueOf(priority)),
                        "enabled", List.of("true"),
                        "algorithm", List.of(algorithm),
                        "keyUse", List.of(KeyUse.ENC.name())
                )
        ));
        return component;
    }

    KeyWrapper getRsaKey_Default() {
        return getRsaKey(KeyUse.SIG, "RS256", null);
    }

    KeyWrapper getRsaKey(KeyUse keyUse, String algorithm, String keyName) {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            var keyPair = kpg.generateKeyPair();
            KeyWrapper kw = new KeyWrapper();
            kw.setPrivateKey(keyPair.getPrivate());
            kw.setPublicKey(keyPair.getPublic());
            kw.setUse(keyUse);
            kw.setKid(keyName != null ? keyName : KeyUtils.createKeyId(keyPair.getPublic()));
            kw.setType("RSA");
            kw.setAlgorithm(algorithm);
            return kw;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}

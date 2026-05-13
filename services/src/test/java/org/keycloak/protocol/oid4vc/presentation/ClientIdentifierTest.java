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
package org.keycloak.protocol.oid4vc.presentation;

import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.List;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class ClientIdentifierTest {

    private static final String RESPONSE_URI = "https://verifier.example.org/realms/test/broker/oid4vp/endpoint";

    @BeforeClass
    public static void initCrypto() {
        CryptoIntegration.init(CryptoProvider.class.getClassLoader());
    }

    @Test
    public void testRedirectUriClientIdentifierPrefix() {
        ClientIdentifier clientIdentifier = ClientIdentifier.redirectUri(RESPONSE_URI);

        assertEquals(ClientIdentifierPrefix.REDIRECT_URI, clientIdentifier.getPrefix());
        assertEquals(RESPONSE_URI, clientIdentifier.getIdentifier());
        assertEquals("redirect_uri:" + RESPONSE_URI, clientIdentifier.getValue());
    }

    @Test
    public void testX509SanDnsClientIdentifierPrefix() {
        ClientIdentifier clientIdentifier = ClientIdentifier.x509SanDns("verifier.example.org");

        assertEquals(ClientIdentifierPrefix.X509_SAN_DNS, clientIdentifier.getPrefix());
        assertEquals("verifier.example.org", clientIdentifier.getIdentifier());
        assertEquals("x509_san_dns:verifier.example.org", clientIdentifier.getValue());
    }

    @Test
    public void testX509HashClientIdentifierPrefix() throws Exception {
        X509Certificate certificate = certificate();
        String expectedThumbprint = Base64Url.encode(MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded()));

        ClientIdentifier clientIdentifier = ClientIdentifier.x509Hash(certificate);

        assertEquals(ClientIdentifierPrefix.X509_HASH, clientIdentifier.getPrefix());
        assertEquals(expectedThumbprint, clientIdentifier.getIdentifier());
        assertEquals("x509_hash:" + expectedThumbprint, clientIdentifier.getValue());
    }

    @Test
    public void testX509HashClientIdentifierPrefixRequiresCertificate() {
        assertThrows(IllegalArgumentException.class, () -> ClientIdentifier.x509Hash(null));
    }

    @Test
    public void testResolveUsesResponseUriForRedirectUriPrefix() {
        ClientIdentifier clientIdentifier = ClientIdentifier.resolve(
                ClientIdentifierPrefix.REDIRECT_URI,
                RESPONSE_URI,
                null);

        assertEquals("redirect_uri:" + RESPONSE_URI, clientIdentifier.getValue());
    }

    @Test
    public void testResolveX509SanDnsPrefixRequiresCertificate() {
        assertThrows(IllegalArgumentException.class, () -> ClientIdentifier.resolve(
                ClientIdentifierPrefix.X509_SAN_DNS,
                RESPONSE_URI,
                null));
    }

    @Test
    public void testResolveX509SanDnsPrefixRequiresCertificateSan() {
        X509Certificate certificate = certificate();

        assertThrows(IllegalArgumentException.class, () -> ClientIdentifier.resolve(
                ClientIdentifierPrefix.X509_SAN_DNS,
                RESPONSE_URI,
                certificate));
    }

    @Test
    public void testResolveUsesCertificateSanForX509SanDnsPrefix() throws Exception {
        X509Certificate certificate = certificateWithDnsSan("verifier-cert.example.org");

        ClientIdentifier clientIdentifier = ClientIdentifier.resolve(
                ClientIdentifierPrefix.X509_SAN_DNS,
                RESPONSE_URI,
                certificate);

        assertEquals("x509_san_dns:verifier-cert.example.org", clientIdentifier.getValue());
    }

    @Test
    public void testResolveUsesCertificateThumbprintForX509HashPrefix() throws Exception {
        X509Certificate certificate = certificate();
        String expectedThumbprint = Base64Url.encode(MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded()));

        ClientIdentifier clientIdentifier = ClientIdentifier.resolve(
                ClientIdentifierPrefix.X509_HASH,
                RESPONSE_URI,
                certificate);

        assertEquals("x509_hash:" + expectedThumbprint, clientIdentifier.getValue());
    }

    @Test
    public void testResolveX509HashPrefixRequiresCertificate() {
        assertThrows(IllegalArgumentException.class, () -> ClientIdentifier.resolve(
                ClientIdentifierPrefix.X509_HASH,
                RESPONSE_URI,
                null));
    }

    private X509Certificate certificate() {
        return CertificateUtils.generateV1SelfSignedCertificate(KeyUtils.generateEcKeyPair("secp256r1"), "oid4vp-verifier");
    }

    private X509Certificate certificateWithDnsSan(String dnsName) throws Exception {
        KeyPair keyPair = KeyUtils.generateEcKeyPair("secp256r1");
        X509Certificate caCert = CertificateUtils.generateV1SelfSignedCertificate(keyPair, dnsName);
        return CertificateUtils.generateV3Certificate(keyPair, keyPair.getPrivate(), caCert, dnsName, List.of(dnsName));
    }
}

/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oid4vc.issuance.keybinding;

import java.security.KeyPair;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.Set;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.protocol.oid4vc.issuance.VCIssuerException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies that key attestation x5c chains are only accepted when they anchor in the supplied trust
 * anchors (i.e. the configured truststore), and not for arbitrary or self-signed certificates.
 */
public class AttestationValidatorUtilX5cTest {

    @BeforeAll
    public static void beforeAll() {
        CryptoIntegration.init(CryptoProvider.class.getClassLoader());
    }

    @Test
    public void leafChainingToTrustedAnchorIsAccepted() throws Exception {
        KeyPair caKeyPair = KeyUtils.generateRsaKeyPair(2048);
        X509Certificate caCert = CertificateUtils.generateV1SelfSignedCertificate(caKeyPair, "CN=Test Attestation CA");

        KeyPair leafKeyPair = KeyUtils.generateRsaKeyPair(2048);
        X509Certificate leafCert = CertificateUtils.generateV3Certificate(leafKeyPair, caKeyPair.getPrivate(), caCert, "CN=Attestation Leaf");

        List<String> x5c = List.of(Base64.getEncoder().encodeToString(leafCert.getEncoded()));

        JWK jwk = assertDoesNotThrow(() -> AttestationValidatorUtil.resolveJwkFromValidatedX5c(
                x5c, "RS256", Set.of(new TrustAnchor(caCert, null))));
        assertNotNull(jwk);
        assertEquals("RSA", jwk.getKeyType());
    }

    @Test
    public void leafNotChainingToTrustedAnchorIsRejected() throws Exception {
        KeyPair caKeyPair = KeyUtils.generateRsaKeyPair(2048);
        X509Certificate caCert = CertificateUtils.generateV1SelfSignedCertificate(caKeyPair, "CN=Test Attestation CA");

        KeyPair leafKeyPair = KeyUtils.generateRsaKeyPair(2048);
        X509Certificate leafCert = CertificateUtils.generateV3Certificate(leafKeyPair, caKeyPair.getPrivate(), caCert, "CN=Attestation Leaf");

        List<String> x5c = List.of(Base64.getEncoder().encodeToString(leafCert.getEncoded()));

        // A certificate signed by a CA that is not among the trust anchors must be rejected, even though
        // it is a perfectly valid certificate. This is what stops an arbitrary publicly-trusted CA from
        // satisfying a key attestation.
        KeyPair otherCaKeyPair = KeyUtils.generateRsaKeyPair(2048);
        X509Certificate otherCaCert = CertificateUtils.generateV1SelfSignedCertificate(otherCaKeyPair, "CN=Untrusted CA");

        assertThrows(VCIssuerException.class, () -> AttestationValidatorUtil.resolveJwkFromValidatedX5c(
                x5c, "RS256", Set.of(new TrustAnchor(otherCaCert, null))));
    }

    @Test
    public void selfSignedLeafIsRejected() throws Exception {
        KeyPair caKeyPair = KeyUtils.generateRsaKeyPair(2048);
        X509Certificate selfSigned = CertificateUtils.generateV1SelfSignedCertificate(caKeyPair, "CN=Self Signed");

        List<String> x5c = List.of(Base64.getEncoder().encodeToString(selfSigned.getEncoded()));

        assertThrows(VCIssuerException.class, () -> AttestationValidatorUtil.resolveJwkFromValidatedX5c(
                x5c, "RS256", Set.of(new TrustAnchor(selfSigned, null))));
    }
}

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

package org.keycloak.services.resources.admin;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.common.util.CertificateUtils;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class IdentityProviderMtlsResourceTest {

    private static final String EKU_CLIENT_AUTH = "1.3.6.1.5.5.7.3.2";
    private static final String EKU_SERVER_AUTH = "1.3.6.1.5.5.7.3.1";
    private static final String EKU_ANY = "2.5.29.37.0";
    private static final String NO_CLIENT_AUTH = "The extended key usage of the certificate does not include client authentication.";
    private static final String NO_SIGNATURE = "The key usage of the certificate does not include digital signature.";

    private static KeyPair keyPair;

    @BeforeClass
    public static void init() throws Exception {
        CryptoIntegration.init(CryptoProvider.class.getClassLoader());
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
    }

    @Test
    public void fingerprintIsUppercaseHexWithColons() throws Exception {
        X509Certificate certificate = CertificateUtils.generateV1SelfSignedCertificate(keyPair, "client");
        HexFormat hex = HexFormat.ofDelimiter(":").withUpperCase();

        assertEquals(hex.formatHex(MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded())),
                IdentityProviderMtlsResource.fingerprintSha256(certificate));
    }

    @Test
    public void certificateWithoutUsageExtensionsHasNoProblems() {
        assertEquals(List.of(), IdentityProviderMtlsResource.usageProblems(null, null));
        assertEquals(List.of(), IdentityProviderMtlsResource.problems(CertificateUtils.generateV1SelfSignedCertificate(keyPair, "client")));
    }

    @Test
    public void clientAuthenticationUsageHasNoProblems() {
        assertEquals(List.of(), IdentityProviderMtlsResource.usageProblems(new boolean[] { true }, List.of(EKU_SERVER_AUTH, EKU_CLIENT_AUTH)));
        assertEquals(List.of(), IdentityProviderMtlsResource.usageProblems(null, List.of(EKU_ANY)));
    }

    @Test
    public void extendedKeyUsageWithoutClientAuthenticationIsAProblem() {
        assertEquals(List.of(NO_CLIENT_AUTH), IdentityProviderMtlsResource.usageProblems(null, List.of(EKU_SERVER_AUTH)));
    }

    @Test
    public void keyUsageWithoutDigitalSignatureIsAProblem() {
        assertEquals(List.of(NO_SIGNATURE), IdentityProviderMtlsResource.usageProblems(new boolean[] { false, false, true }, null));
    }

    @Test
    public void bothUsagesCanBeProblems() {
        assertEquals(List.of(NO_CLIENT_AUTH, NO_SIGNATURE), IdentityProviderMtlsResource.usageProblems(new boolean[] { false }, List.of(EKU_SERVER_AUTH)));
    }

    @Test
    public void expiredCertificateIsAProblem() {
        X509Certificate expired = CertificateUtils.generateV1SelfSignedCertificate(keyPair, "client", BigInteger.ONE,
                new Date(System.currentTimeMillis() - 10_000));
        assertEquals(List.of("The certificate has expired."), IdentityProviderMtlsResource.problems(expired));
    }

    @Test
    public void generatedChainCertificateIsMissingClientAuthentication() throws Exception {
        // the generated certificate has the server authentication and email protection usages only
        X509Certificate ca = CertificateUtils.generateV1SelfSignedCertificate(keyPair, "ca");
        X509Certificate certificate = CertificateUtils.generateV3Certificate(keyPair, keyPair.getPrivate(), ca, "client");
        assertEquals(List.of(NO_CLIENT_AUTH), IdentityProviderMtlsResource.problems(certificate));
    }
}

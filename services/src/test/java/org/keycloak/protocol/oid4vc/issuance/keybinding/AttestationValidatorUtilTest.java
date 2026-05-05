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

package org.keycloak.protocol.oid4vc.issuance.keybinding;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.Environment;
import org.keycloak.crypto.KeyType;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.protocol.oid4vc.issuance.VCIssuerException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AttestationValidatorUtilTest {

    @BeforeAll
    public static void beforeAll() {
        CryptoIntegration.init(CryptoProvider.class.getClassLoader());
    }

    @Test
    public void testSelfSignedX5cCertificateChainIsAcceptedInDevMode() throws Exception {
        withProfile(Environment.DEV_PROFILE_VALUE, () -> {
            JWK jwk = AttestationValidatorUtil.resolveJwkFromValidatedX5c(createSelfSignedX5c(), "RS256");
            assertNotNull(jwk);
        });
    }

    @Test
    public void testSelfSignedX5cCertificateChainIsRejectedOutsideDevMode() {
        withProfile("prod", () -> {
            assertThrows(VCIssuerException.class,
                    () -> AttestationValidatorUtil.resolveJwkFromValidatedX5c(createSelfSignedX5c(), "RS256"));
        });
    }

    private static List<String> createSelfSignedX5c() {
        try {
            KeyPairGenerator keyGen = CryptoIntegration.getProvider().getKeyPairGen(KeyType.RSA);
            KeyPair keyPair = keyGen.generateKeyPair();
            X509Certificate certificate = CertificateUtils.generateV1SelfSignedCertificate(keyPair, "Test Certificate");

            return List.of(Base64.getEncoder().encodeToString(certificate.getEncoded()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void withProfile(String profile, Runnable runnable) {
        String previousProfile = System.getProperty(Environment.PROFILE);
        try {
            System.setProperty(Environment.PROFILE, profile);
            runnable.run();
        } finally {
            if (previousProfile != null) {
                System.setProperty(Environment.PROFILE, previousProfile);
            } else {
                System.clearProperty(Environment.PROFILE);
            }
        }
    }
}

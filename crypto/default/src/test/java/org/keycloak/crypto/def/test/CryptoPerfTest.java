/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.crypto.def.test;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;

import org.keycloak.RSATokenVerifier;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.PemUtils;
import org.keycloak.common.util.Time;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.credential.hash.Pbkdf2PasswordHashProvider;
import org.keycloak.credential.hash.Pbkdf2Sha256PasswordHashProviderFactory;
import org.keycloak.credential.hash.Pbkdf2Sha512PasswordHashProviderFactory;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.representations.AccessToken;
import org.keycloak.rule.CryptoInitRule;
import org.keycloak.util.TokenUtil;

import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Simple test just to test how long it takes to generate keys of specific size, Sign+Verify with keys of specific size etc.
 *
 * Ignored by default to avoid being executed during regular runs (can be run on demand)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@Ignore
public class CryptoPerfTest {

    private static final int COUNT_ITERATIONS = 100;

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    private static final Logger logger = Logger.getLogger(CryptoPerfTest.class);

    @Test
    public void testImportedKey() {
        perfTest(this::testImportedKeyImpl, "testImportedKey");
    }

    private void perfTest(Runnable runnable, String testName) {
        perfTest(runnable, testName, COUNT_ITERATIONS);
    }

    private void perfTest(Runnable runnable, String testName, int count) {
        long start = Time.currentTimeMillis();
        for (int i=0 ; i<count ; i++) {
            runnable.run();
        }
        long took = Time.currentTimeMillis() - start;
        logger.infof("test '%s' took %d ms", testName, took);
    }

    private void testImportedKeyImpl() {
        String privateRsaKeyPem = "MIICXAIBAAKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQABAoGAfmO8gVhyBxdqlxmIuglbz8bcjQbhXJLR2EoS8ngTXmN1bo2L90M0mUKSdc7qF10LgETBzqL8jYlQIbt+e6TH8fcEpKCjUlyq0Mf/vVbfZSNaVycY13nTzo27iPyWQHK5NLuJzn1xvxxrUeXI6A2WFpGEBLbHjwpx5WQG9A+2scECQQDvdn9NE75HPTVPxBqsEd2z10TKkl9CZxu10Qby3iQQmWLEJ9LNmy3acvKrE3gMiYNWb6xHPKiIqOR1as7L24aTAkEAtyvQOlCvr5kAjVqrEKXalj0Tzewjweuxc0pskvArTI2Oo070h65GpoIKLc9jf+UA69cRtquwP93aZKtW06U8dQJAF2Y44ks/mK5+eyDqik3koCI08qaC8HYq2wVl7G2QkJ6sbAaILtcvD92ToOvyGyeE0flvmDZxMYlvaZnaQ0lcSQJBAKZU6umJi3/xeEbkJqMfeLclD27XGEFoPeNrmdx0q10Azp4NfJAY+Z8KRyQCR2BEG+oNitBOZ+YXF9KCpH3cdmECQHEigJhYg+ykOvr1aiZUMFT72HU0jnmQe2FVekuG+LJUt2Tm7GtMjTFoGpf0JwrVuZN39fOYAlo+nTixgeW7X8Y=";

        PrivateKey privateKey = PemUtils.decodePrivateKey(privateRsaKeyPem);
        PublicKey publicKey = KeyUtils.extractPublicKey(privateKey);

        KeyPair keyPair = new KeyPair(publicKey, privateKey);
    }

    @Test
    public void testGeneratedKeys1024() {
        perfTest(() -> generateKeys(1024), "testGeneratedKeys1024");
    }

    @Test
    public void testGeneratedKeys2048() {
        perfTest(() -> generateKeys(2048), "testGeneratedKeys2048");
    }

    @Test
    public void testSignAndVerifyTokens1024() {
        KeyPair keyPair = generateKeys(1024);
        perfTest(() -> testTokenSignAndVerify(keyPair), "testSignAndVerifyTokens1024");
    }

    @Test
    public void testSignAndVerifyTokens2048() {
        KeyPair keyPair = generateKeys(2048);
        perfTest(() -> testTokenSignAndVerify(keyPair), "testSignAndVerifyTokens2048");
    }

    @Test
    public void testPbkdf256() {
        int iterations = 600 * 1000;
        int derivedKeySize = 256;
        String providerId = Pbkdf2Sha256PasswordHashProviderFactory.ID;
        String algorithm = Pbkdf2Sha256PasswordHashProviderFactory.PBKDF2_ALGORITHM;
        perfTestPasswordHashins(iterations, derivedKeySize, providerId, algorithm);
    }

    @Test
    public void testPbkdf512() {
        int iterations = 210 * 1000;
        int derivedKeySize = 512;
        String providerId = Pbkdf2Sha512PasswordHashProviderFactory.ID;
        String algorithm = Pbkdf2Sha512PasswordHashProviderFactory.PBKDF2_ALGORITHM;
        perfTestPasswordHashins(iterations, derivedKeySize, providerId, algorithm);
    }

    private void perfTestPasswordHashins(int iterations, int derivedKeySize, String providerId, String algorithm) {
        Pbkdf2PasswordHashProvider provider = new Pbkdf2PasswordHashProvider(
                providerId,
                algorithm,
                iterations,
                0,
                derivedKeySize);

        perfTest(new Runnable() {
            @Override
            public void run() {
                provider.encodedCredential("password", -1);
            }
        }, "testPbkdf512", 1);
    }

    private KeyPair generateKeys(int size) {
        KeyPair keyPair;
        try {
            keyPair = KeyUtils.generateRsaKeyPair(size);
            String privateKey = PemUtils.encodeKey(keyPair.getPrivate());
            String publicKey = PemUtils.encodeKey(keyPair.getPublic());
        } catch (Throwable t) {
            throw new ComponentValidationException("Failed to generate keys", t);
        }

        generateCertificate(keyPair);
        return keyPair;
    }

    private void generateCertificate(KeyPair keyPair) {
        try {
            Certificate certificate = CertificateUtils.generateV1SelfSignedCertificate(keyPair, "test");
            String certificatee = PemUtils.encodeCertificate(certificate);
        } catch (Throwable t) {
            throw new ComponentValidationException("Failed to generate certificate", t);
        }
    }

    public void testTokenSignAndVerify(KeyPair keyPair) {
        try {
            AccessToken token = new AccessToken();
            token.type(TokenUtil.TOKEN_TYPE_BEARER)
                    .subject("CN=Client")
                    .issuer("http://localhost:8080/auth/realm")
                    .addAccess("service").addRole("admin");

            String encoded = new JWSBuilder()
                    .jsonContent(token)
                    .rsa256(keyPair.getPrivate());
            //System.out.print("encoded size: " + encoded.length());
            token = RSATokenVerifier.verifyToken(encoded, keyPair.getPublic(), "http://localhost:8080/auth/realm");
            Assert.assertTrue(token.getResourceAccess("service").getRoles().contains("admin"));
            Assert.assertEquals("CN=Client", token.getSubject());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

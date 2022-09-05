/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.jose;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.jose.jwe.JWE;
import org.keycloak.jose.jwe.JWEConstants;
import org.keycloak.jose.jwe.JWEException;
import org.keycloak.jose.jwe.JWEHeader;
import org.keycloak.jose.jwe.JWEKeyStorage;
import org.keycloak.jose.jwe.JWEUtils;
import org.keycloak.jose.jwe.alg.JWEAlgorithmProvider;
import org.keycloak.jose.jwe.enc.AesCbcHmacShaJWEEncryptionProvider;
import org.keycloak.jose.jwe.enc.AesGcmJWEEncryptionProvider;
import org.keycloak.jose.jwe.enc.JWEEncryptionProvider;
import org.keycloak.rule.CryptoInitRule;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyPair;

/**
 * This is not tested in keycloak-core. The subclasses should be created in the crypto modules to make sure it is tested with corresponding modules (bouncycastle VS bouncycastle-fips)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class JWETest {

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    private static final String PAYLOAD = "Hello world! How are you man? I hope you are fine. This is some quite a long text, which is much longer than just simple 'Hello World'";

    private static final byte[] HMAC_SHA256_KEY = new byte[] { 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 13, 14, 15, 16 };
    private static final byte[] AES_128_KEY =  new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };

    private static final byte[] HMAC_SHA512_KEY = new byte[] { 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 13, 14, 15, 16, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };
    private static final byte[] AES_256_KEY =  new byte[] { 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 13, 14, 15, 16, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };

    @Test
    public void testDirect_Aes128CbcHmacSha256() throws Exception {
        SecretKey aesKey = new SecretKeySpec(AES_128_KEY, "AES");
        SecretKey hmacKey = new SecretKeySpec(HMAC_SHA256_KEY, "HMACSHA2");

        testDirectEncryptAndDecrypt(aesKey, hmacKey, JWEConstants.A128CBC_HS256, PAYLOAD, true);
    }

    // Works just on OpenJDK 8. Other JDKs (IBM, Oracle) have restrictions on maximum key size of AES to be 128
    @Ignore
    @Test
    public void testDirect_Aes256CbcHmacSha512() throws Exception {
        final SecretKey aesKey = new SecretKeySpec(AES_256_KEY, "AES");
        final SecretKey hmacKey = new SecretKeySpec(HMAC_SHA512_KEY, "HMACSHA2");

        testDirectEncryptAndDecrypt(aesKey, hmacKey, JWEConstants.A256CBC_HS512, PAYLOAD, true);
    }


    private void testDirectEncryptAndDecrypt(Key aesKey, Key hmacKey, String encAlgorithm, String payload, boolean sysout) throws Exception {
        JWEHeader jweHeader = new JWEHeader(JWEConstants.DIRECT, encAlgorithm, null);
        JWE jwe = new JWE()
                .header(jweHeader)
                .content(payload.getBytes(StandardCharsets.UTF_8));

        jwe.getKeyStorage()
                .setCEKKey(aesKey, JWEKeyStorage.KeyUse.ENCRYPTION)
                .setCEKKey(hmacKey, JWEKeyStorage.KeyUse.SIGNATURE);

        String encodedContent = jwe.encodeJwe();

        if (sysout) {
            System.out.println("Encoded content: " + encodedContent);
            System.out.println("Encoded content length: " + encodedContent.length());
        }

        jwe = new JWE();
        jwe.getKeyStorage()
                .setCEKKey(aesKey, JWEKeyStorage.KeyUse.ENCRYPTION)
                .setCEKKey(hmacKey, JWEKeyStorage.KeyUse.SIGNATURE);

        jwe.verifyAndDecodeJwe(encodedContent);

        String decodedContent = new String(jwe.getContent(), StandardCharsets.UTF_8);

        Assert.assertEquals(payload, decodedContent);
    }


    @Ignore
    @Test
    public void testPerfDirect() throws Exception {
        int iterations = 50000;

        long start = System.currentTimeMillis();
        for (int i=0 ; i<iterations ; i++) {
            // took around 2950 ms with 50000 iterations
            SecretKey aesKey = new SecretKeySpec(AES_128_KEY, "AES");
            SecretKey hmacKey = new SecretKeySpec(HMAC_SHA256_KEY, "HMACSHA2");
            String encAlg = JWEConstants.A128CBC_HS256;

            // Similar perf like AES128CBC_HS256
            //SecretKey aesKey = new SecretKeySpec(AES_256_KEY, "AES");
            //SecretKey hmacKey = new SecretKeySpec(HMAC_SHA512_KEY, "HMACSHA2");
            //String encAlg = JWEConstants.A256CBC_HS512;

            String payload = PAYLOAD + i;
            testDirectEncryptAndDecrypt(aesKey, hmacKey, encAlg, payload, false);
        }

        long took = System.currentTimeMillis() - start;
        System.out.println("Iterations: " + iterations + ", took: " + took);
    }

    @Test
    public void testAesKW_Aes128CbcHmacSha256() throws Exception {
        SecretKey aesKey = new SecretKeySpec(AES_128_KEY, "AES");

        testAesKW_Aes128CbcHmacSha256(aesKey);
    }

    private void testAesKW_Aes128CbcHmacSha256(SecretKey aesKey) throws JWEException {
        JWEHeader jweHeader = new JWEHeader(JWEConstants.A128KW, JWEConstants.A128CBC_HS256, null);
        JWE jwe = new JWE()
                .header(jweHeader)
                .content(PAYLOAD.getBytes(StandardCharsets.UTF_8));

        jwe.getKeyStorage()
                .setEncryptionKey(aesKey);

        String encodedContent = jwe.encodeJwe();

        System.out.println("Encoded content: " + encodedContent);
        System.out.println("Encoded content length: " + encodedContent.length());

        jwe = new JWE();
        jwe.getKeyStorage()
                .setDecryptionKey(aesKey);

        jwe.verifyAndDecodeJwe(encodedContent);

        String decodedContent = new String(jwe.getContent(), StandardCharsets.UTF_8);

        Assert.assertEquals(PAYLOAD, decodedContent);
    }

    @Test
    public void testSalt() {
        byte[] random = JWEUtils.generateSecret(8);
        System.out.print("new byte[] = {");
        for (byte b : random) {
            System.out.print(""+Byte.toString(b)+",");
        }
    }


    @Test
    public void externalJweAes128CbcHmacSha256Test() throws JWEException {
        String externalJwe = "eyJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwiYWxnIjoiZGlyIn0..qysUrI1iVtiG4Z4jyr7XXg.apdNSQhR7WDMg6IHf5aLVI0gGp6JuOHYmIUtflns4WHmyxOOnh_GShLI6DWaK_SiywTV5gZvZYtl8H8Iv5fTfLkc4tiDDjbdtmsOP7tqyRxVh069gU5UvEAgmCXbIKALutgYXcYe2WM4E6BIHPTSt8jXdkktFcm7XHiD7mpakZyjXsG8p3XVkQJ72WbJI_t6.Ks6gHeko7BRTZ4CFs5ijRA";
        System.out.println("External encoded content length: " + externalJwe.length());

        final SecretKey aesKey = new SecretKeySpec(AES_128_KEY, "AES");
        final SecretKey hmacKey = new SecretKeySpec(HMAC_SHA256_KEY, "HMACSHA2");

        JWE jwe = new JWE();
        jwe.getKeyStorage()
                .setCEKKey(aesKey, JWEKeyStorage.KeyUse.ENCRYPTION)
                .setCEKKey(hmacKey, JWEKeyStorage.KeyUse.SIGNATURE);

        jwe.verifyAndDecodeJwe(externalJwe);

        String decodedContent = new String(jwe.getContent(), StandardCharsets.UTF_8);

        Assert.assertEquals(PAYLOAD, decodedContent);
    }


    // Works just on OpenJDK 8. Other JDKs (IBM, Oracle) have restrictions on maximum key size of AES to be 128
    @Ignore
    @Test
    public void externalJweAes256CbcHmacSha512Test() throws JWEException {
        String externalJwe = "eyJlbmMiOiJBMjU2Q0JDLUhTNTEyIiwiYWxnIjoiZGlyIn0..xUPndQ5U69CYaWMKr4nyeg.AzSzba6OdNsvTIoNpub8d2TmYnkY7W8Sd-1S33DjJwJsSaNcfvfXBq5bqXAGVAnLHrLZJKWoEYsmOrYHz3Nao-kpLtUpc4XZI8yiYUqkHTjmxZnfD02R6hz31a5KBCnDTtUEv23VSxm8yUyQKoUTpVHbJ3b2VQvycg2XFUXPsA6oaSSEpz-uwe1Vmun2hUBB.Qal4rMYn1RrXQ9AQ9ONUjUXvlS2ow8np-T8QWMBR0ns";
        System.out.println("External encoded content length: " + externalJwe.length());

        final SecretKey aesKey = new SecretKeySpec(AES_256_KEY, "AES");
        final SecretKey hmacKey = new SecretKeySpec(HMAC_SHA512_KEY, "HMACSHA2");

        JWE jwe = new JWE();
        jwe.getKeyStorage()
                .setCEKKey(aesKey, JWEKeyStorage.KeyUse.ENCRYPTION)
                .setCEKKey(hmacKey, JWEKeyStorage.KeyUse.SIGNATURE);

        jwe.verifyAndDecodeJwe(externalJwe);

        String decodedContent = new String(jwe.getContent(), StandardCharsets.UTF_8);

        Assert.assertEquals(PAYLOAD, decodedContent);
    }


    @Test
    public void externalJweAes128KeyWrapTest() throws Exception {
        // See example "A.3" from JWE specification - https://tools.ietf.org/html/rfc7516#page-41
        String externalJwe = "eyJhbGciOiJBMTI4S1ciLCJlbmMiOiJBMTI4Q0JDLUhTMjU2In0.6KB707dM9YTIgHtLvtgWQ8mKwboJW3of9locizkDTHzBC2IlrT1oOQ.AxY8DCtDaGlsbGljb3RoZQ.KDlTtXchhZTGufMYmOYGS4HffxPSUrfmqCHXaI9wOGY.U0m_YmjN04DJvceFICbCVQ";

        byte[] aesKey = Base64Url.decode("GawgguFyGrWKav7AX4VKUg");
        SecretKeySpec aesKeySpec = new SecretKeySpec(aesKey, "AES");

        JWE jwe = new JWE();
        jwe.getKeyStorage()
                .setDecryptionKey(aesKeySpec);

        jwe.verifyAndDecodeJwe(externalJwe);

        String decodedContent = new String(jwe.getContent(), StandardCharsets.UTF_8);

        Assert.assertEquals("Live long and prosper.", decodedContent);

    }

    @Test
    public void testRSA1_5_A128GCM() throws Exception {
        testKeyEncryption_ContentEncryptionAesGcm(JWEConstants.RSA1_5, JWEConstants.A128GCM);
    }

    @Test
    public void testRSAOAEP_A128GCM() throws Exception {
        testKeyEncryption_ContentEncryptionAesGcm(JWEConstants.RSA_OAEP, JWEConstants.A128GCM);
    }

    @Test
    public void testRSAOAEP256_A128GCM() throws Exception {
        testKeyEncryption_ContentEncryptionAesGcm(JWEConstants.RSA_OAEP_256, JWEConstants.A128GCM);
    }

    @Test
    public void testRSA1_5_A128CBCHS256() throws Exception {
        testKeyEncryption_ContentEncryptionAesHmacSha(JWEConstants.RSA1_5, JWEConstants.A128CBC_HS256);
    }

    @Test
    public void testRSAOAEP_A128CBCHS256() throws Exception {
        testKeyEncryption_ContentEncryptionAesHmacSha(JWEConstants.RSA_OAEP, JWEConstants.A128CBC_HS256);
    }

    @Test
    public void testRSAOAEP256_A128CBCHS256() throws Exception {
        testKeyEncryption_ContentEncryptionAesHmacSha(JWEConstants.RSA_OAEP_256, JWEConstants.A128CBC_HS256);
    }
 
    private void testKeyEncryption_ContentEncryptionAesGcm(String jweAlgorithmName, String jweEncryptionName) throws Exception {
        // generate key pair for KEK
        KeyPair keyPair = KeyUtils.generateRsaKeyPair(2048);
        JWEAlgorithmProvider jweAlgorithmProvider = CryptoIntegration.getProvider().getAlgorithmProvider(JWEAlgorithmProvider.class, jweAlgorithmName);
        JWEEncryptionProvider jweEncryptionProvider = new AesGcmJWEEncryptionProvider(jweEncryptionName);

        JWEHeader jweHeader = new JWEHeader(jweAlgorithmName, jweEncryptionName, null);
        JWE jwe = new JWE()
                .header(jweHeader)
                .content(PAYLOAD.getBytes(StandardCharsets.UTF_8));

        jwe.getKeyStorage()
                .setEncryptionKey(keyPair.getPublic());

        String encodedContent = jwe.encodeJwe(jweAlgorithmProvider, jweEncryptionProvider);
        System.out.println("Encoded content: " + encodedContent);
        System.out.println("Encoded content length: " + encodedContent.length());

        jwe = new JWE();
        jwe.getKeyStorage()
                .setDecryptionKey(keyPair.getPrivate());
        jwe.verifyAndDecodeJwe(encodedContent, jweAlgorithmProvider, jweEncryptionProvider);
        String decodedContent = new String(jwe.getContent(), StandardCharsets.UTF_8);
        System.out.println("Decoded content: " + decodedContent);
        System.out.println("Decoded content length: " + decodedContent.length());

        Assert.assertEquals(PAYLOAD, decodedContent);
    }

    private void testKeyEncryption_ContentEncryptionAesHmacSha(String jweAlgorithmName, String jweEncryptionName) throws Exception {
        // generate key pair for KEK
        KeyPair keyPair = KeyUtils.generateRsaKeyPair(2048);
        // generate CEK
        final SecretKey aesKey = new SecretKeySpec(AES_128_KEY, "AES");
        final SecretKey hmacKey = new SecretKeySpec(HMAC_SHA256_KEY, "HMACSHA2");

        JWEAlgorithmProvider jweAlgorithmProvider = CryptoIntegration.getProvider().getAlgorithmProvider(JWEAlgorithmProvider.class, jweAlgorithmName);
        JWEEncryptionProvider jweEncryptionProvider = new AesCbcHmacShaJWEEncryptionProvider(jweEncryptionName);

        JWEHeader jweHeader = new JWEHeader(jweAlgorithmName, jweEncryptionName, null);
        JWE jwe = new JWE()
                .header(jweHeader)
                .content(PAYLOAD.getBytes(StandardCharsets.UTF_8));

        jwe.getKeyStorage()
                .setEncryptionKey(keyPair.getPublic());

        jwe.getKeyStorage()
                .setCEKKey(aesKey, JWEKeyStorage.KeyUse.ENCRYPTION)
                .setCEKKey(hmacKey, JWEKeyStorage.KeyUse.SIGNATURE);

        String encodedContent = jwe.encodeJwe(jweAlgorithmProvider, jweEncryptionProvider);
        System.out.println("Encoded content: " + encodedContent);
        System.out.println("Encoded content length: " + encodedContent.length());

        jwe = new JWE();
        jwe.getKeyStorage()
            .setDecryptionKey(keyPair.getPrivate());
        jwe.getKeyStorage()
            .setCEKKey(aesKey, JWEKeyStorage.KeyUse.ENCRYPTION)
            .setCEKKey(hmacKey, JWEKeyStorage.KeyUse.SIGNATURE);
        jwe.verifyAndDecodeJwe(encodedContent, jweAlgorithmProvider, jweEncryptionProvider);
        String decodedContent = new String(jwe.getContent(), StandardCharsets.UTF_8);
        System.out.println("Decoded content: " + decodedContent);
        System.out.println("Decoded content length: " + decodedContent.length());

        Assert.assertEquals(PAYLOAD, decodedContent);
    }

}

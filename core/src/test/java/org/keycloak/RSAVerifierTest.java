/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.keycloak.common.VerificationException;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.Time;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.rule.CryptoInitRule;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.TokenUtil;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class RSAVerifierTest {
    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();
    // private static X509Certificate[] idpCertificates;
    private static KeyPair idpPair;
    private static KeyPair badPair;
    // private static KeyPair clientPair;
    // private static X509Certificate[] clientCertificateChain;
    private AccessToken token;

    @BeforeClass
    public static void setupCerts()
            throws Exception {
        // CryptoIntegration.init(ClassLoader.getSystemClassLoader());
        badPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        idpPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
    }

    @Before
    public void initTest() {

        token = new AccessToken();
        token.type(TokenUtil.TOKEN_TYPE_BEARER)
             .subject("CN=Client")
             .issuer("http://localhost:8080/auth/realm")
             .addAccess("service").addRole("admin");
    }

    @Test
    public void testSimpleVerification() throws Exception {
        String encoded = new JWSBuilder()
                .jsonContent(token)
                .rsa256(idpPair.getPrivate());
        System.out.print("encoded size: " + encoded.length());
        AccessToken token = verifySkeletonKeyToken(encoded);
        Assert.assertTrue(token.getResourceAccess("service").getRoles().contains("admin"));
        Assert.assertEquals("CN=Client", token.getSubject());
    }

    @Test
    public void testVerificationWithAddedX5cAndJwk() throws Exception {
        KeyPair caKeyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        X509Certificate caCertificate = CertificateUtils.generateV1SelfSignedCertificate(caKeyPair, "root");
        X509Certificate idpCertificate = CertificateUtils.generateV3Certificate(idpPair,
                                                                                caKeyPair.getPrivate(),
                                                                                caCertificate,
                                                                                "idp");
        JWK jwk = JWKBuilder.create().rsa(idpPair.getPublic());

        String encoded = new JWSBuilder()
                .jwk(jwk)
                .x5c(Arrays.asList(new X509Certificate[]{idpCertificate, caCertificate}))
                .jsonContent(token)
                .rsa256(idpPair.getPrivate());
        TokenVerifier tokenVerifier = TokenVerifier.create(encoded, JsonWebToken.class);
        verifySkeletonKeyToken(encoded);
        Assert.assertTrue(token.getResourceAccess("service").getRoles().contains("admin"));
        Assert.assertEquals("CN=Client", token.getSubject());

        List<String> x5c = tokenVerifier.getHeader().getX5c();
        Assert.assertEquals(2, x5c.size());
        Assert.assertEquals(Base64.getEncoder().encodeToString(idpCertificate.getEncoded()), x5c.get(0));
        Assert.assertEquals(Base64.getEncoder().encodeToString(caCertificate.getEncoded()), x5c.get(1));
        Assert.assertEquals(JsonSerialization.mapper.convertValue(jwk, Map.class),
                            JsonSerialization.mapper.convertValue(tokenVerifier.getHeader().getKey(), Map.class));
    }

    private AccessToken verifySkeletonKeyToken(String encoded) throws VerificationException {
        return RSATokenVerifier.verifyToken(encoded, idpPair.getPublic(), "http://localhost:8080/auth/realm");
    }

    // @Test
    public void testSpeed() throws Exception {
        // Took 44 seconds with 50000 iterations
        byte[] tokenBytes = JsonSerialization.writeValueAsBytes(token);

        long start = System.currentTimeMillis();
        int count = 50000;
        for (int i = 0; i < count; i++) {
            String encoded = new JWSBuilder()
                    .content(tokenBytes)
                    .rsa256(idpPair.getPrivate());

            verifySkeletonKeyToken(encoded);

        }
        long end = System.currentTimeMillis() - start;
        System.out.println("took: " + end);
    }

    @Test
    public void testBadSignature() {

        String encoded = new JWSBuilder()
                .jsonContent(token)
                .rsa256(badPair.getPrivate());

        AccessToken v = null;
        try {
            v = verifySkeletonKeyToken(encoded);
            Assert.fail();
        } catch (VerificationException ignored) {
        }
    }

    @Test
    public void testNotBeforeGood() throws Exception {
        token.nbf(Time.currentTime() - 100L);

        String encoded = new JWSBuilder()
                .jsonContent(token)
                .rsa256(idpPair.getPrivate());

        AccessToken v = null;
        try {
            v = verifySkeletonKeyToken(encoded);
        } catch (VerificationException ignored) {
            throw ignored;
        }
    }

    @Test
    public void testNotBeforeBad() {
        token.nbf(Time.currentTime() + 100L);

        String encoded = new JWSBuilder()
                .jsonContent(token)
                .rsa256(idpPair.getPrivate());

        AccessToken v = null;
        try {
            v = verifySkeletonKeyToken(encoded);
            Assert.fail();
        } catch (VerificationException ignored) {
            System.out.println(ignored.getMessage());
        }
    }

    @Test
    public void testExpirationGood() throws Exception {
        token.exp(Time.currentTime() + 100L);

        String encoded = new JWSBuilder()
                .jsonContent(token)
                .rsa256(idpPair.getPrivate());

        AccessToken v = null;
        try {
            v = verifySkeletonKeyToken(encoded);
        } catch (VerificationException ignored) {
            throw ignored;
        }
    }

    @Test
    public void testExpirationBad() {
        token.exp(Time.currentTime() - 100L);

        String encoded = new JWSBuilder()
                .jsonContent(token)
                .rsa256(idpPair.getPrivate());

        AccessToken v = null;
        try {
            v = verifySkeletonKeyToken(encoded);
            Assert.fail();
        } catch (VerificationException ignored) {
        }
    }

    @Test
    public void testTokenAuth() {
        token = new AccessToken();
        token.subject("CN=Client")
             .issuer("http://localhost:8080/auth/realms/demo")
             .addAccess("service").addRole("admin").verifyCaller(true);
        token.setEmail("bill@jboss.org");

        String encoded = new JWSBuilder()
                .jsonContent(token)
                .rsa256(idpPair.getPrivate());

        System.out.println("token size: " + encoded.length());

        AccessToken v = null;
        try {
            v = verifySkeletonKeyToken(encoded);
            Assert.fail();
        } catch (VerificationException ignored) {
        }
    }

    @Test
    public void testAudience() throws Exception {
        token.addAudience("my-app");
        token.addAudience("your-app");

        String encoded = new JWSBuilder()
                .jsonContent(token)
                .rsa256(idpPair.getPrivate());

        verifyAudience(encoded, "my-app");
        verifyAudience(encoded, "your-app");

        try {
            verifyAudience(encoded, "other-app");
            Assert.fail();
        } catch (VerificationException ignored) {
            System.out.println(ignored.getMessage());
        }

        try {
            verifyAudience(encoded, null);
            Assert.fail();
        } catch (VerificationException ignored) {
            System.out.println(ignored.getMessage());
        }
    }

    private void verifyAudience(String encodedToken, String expectedAudience) throws VerificationException {
        TokenVerifier.create(encodedToken, AccessToken.class)
                     .publicKey(idpPair.getPublic())
                     .realmUrl("http://localhost:8080/auth/realm")
                     .audience(expectedAudience)
                     .verify();
    }


}

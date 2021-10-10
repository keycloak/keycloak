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

import junit.framework.Assert;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v1CertificateBuilder;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.common.util.Time;
import org.keycloak.common.VerificationException;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.representations.AccessToken;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.TokenUtil;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Security;
import java.security.SignatureException;
import java.util.Date;
import javax.security.auth.x500.X500Principal;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RSAVerifierTest {
    private static X509Certificate[] idpCertificates;
    private static KeyPair idpPair;
    private static KeyPair badPair;
    private static KeyPair clientPair;
    private static X509Certificate[] clientCertificateChain;
    private AccessToken token;

    static {
        if (Security.getProvider("BC") == null) Security.addProvider(new BouncyCastleProvider());
    }

    public static X509Certificate generateTestCertificate(String subject, String issuer, KeyPair pair)
        throws CertificateException, InvalidKeyException, IOException,
               NoSuchProviderException, OperatorCreationException,
               SignatureException
    {
        X500Name issuerDN = new X500Name("CN=" + issuer);
        BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());
        Date notBefore = new Date(System.currentTimeMillis() - 10000);
        Date notAfter = new Date(System.currentTimeMillis() + 10000);
        X500Name subjectDN = new X500Name("CN=" + subject);
        SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo
            .getInstance(pair.getPublic().getEncoded());

        X509v1CertificateBuilder builder = new X509v1CertificateBuilder(
            issuerDN, serialNumber, notBefore, notAfter, subjectDN, subjectPublicKeyInfo
        );

        AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder()
            .find("SHA256WithRSAEncryption");

        AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder()
            .find(sigAlgId);

        ContentSigner signer = new BcRSAContentSignerBuilder(sigAlgId, digAlgId)
            .build(PrivateKeyFactory.createKey(pair.getPrivate().getEncoded()));

        X509CertificateHolder holder = builder.build(signer);

        return new JcaX509CertificateConverter().getCertificate(holder);
    }

    @BeforeClass
    public static void setupCerts()
        throws CertificateException, InvalidKeyException, IOException,
               NoSuchAlgorithmException, NoSuchProviderException,
               OperatorCreationException, SignatureException
    {
        badPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        idpPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        idpCertificates = new X509Certificate[]{generateTestCertificate("CN=IDP", "CN=IDP", idpPair)};
        clientPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        clientCertificateChain = new X509Certificate[]{generateTestCertificate("CN=Client", "CN=IDP", idpPair)};
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
    public void testPemWriter() {
        PublicKey realmPublicKey = idpPair.getPublic();
        StringWriter sw = new StringWriter();
        JcaPEMWriter writer = new JcaPEMWriter(sw);
        try {
            writer.writeObject(realmPublicKey);
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println(sw.toString());
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

    private AccessToken verifySkeletonKeyToken(String encoded) throws VerificationException {
        return RSATokenVerifier.verifyToken(encoded, idpPair.getPublic(), "http://localhost:8080/auth/realm");
    }


   // @Test
   public void testSpeed() throws Exception
   {
       // Took 44 seconds with 50000 iterations
      byte[] tokenBytes = JsonSerialization.writeValueAsBytes(token);

      long start = System.currentTimeMillis();
      int count = 50000;
      for (int i = 0; i < count; i++)
      {
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
        token.notBefore(Time.currentTime() - 100);

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
        token.notBefore(Time.currentTime() + 100);

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
        token.expiration(Time.currentTime() + 100);

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
        token.expiration(Time.currentTime() - 100);

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

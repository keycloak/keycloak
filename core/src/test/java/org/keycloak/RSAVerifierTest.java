package org.keycloak;

import junit.framework.Assert;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.x509.X509V1CertificateGenerator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.representations.AccessToken;
import org.keycloak.util.Time;

import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.Date;

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

    public static X509Certificate generateTestCertificate(String subject, String issuer, KeyPair pair) throws InvalidKeyException,
            NoSuchProviderException, SignatureException {

        X509V1CertificateGenerator certGen = new X509V1CertificateGenerator();

        certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
        certGen.setIssuerDN(new X500Principal(issuer));
        certGen.setNotBefore(new Date(System.currentTimeMillis() - 10000));
        certGen.setNotAfter(new Date(System.currentTimeMillis() + 10000));
        certGen.setSubjectDN(new X500Principal(subject));
        certGen.setPublicKey(pair.getPublic());
        certGen.setSignatureAlgorithm("SHA256WithRSAEncryption");

        return certGen.generateX509Certificate(pair.getPrivate(), "BC");
    }

    @BeforeClass
    public static void setupCerts() throws NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {
        badPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        idpPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        idpCertificates = new X509Certificate[]{generateTestCertificate("CN=IDP", "CN=IDP", idpPair)};
        clientPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        clientCertificateChain = new X509Certificate[]{generateTestCertificate("CN=Client", "CN=IDP", idpPair)};
    }

    @Before
    public void initTest() {

        token = new AccessToken();
        token.subject("CN=Client")
                .issuer("domain")
                .addAccess("service").addRole("admin");
    }

    @Test
    public void testPemWriter() throws Exception {
        PublicKey realmPublicKey = idpPair.getPublic();
        StringWriter sw = new StringWriter();
        PEMWriter writer = new PEMWriter(sw);
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
        AccessToken token = verifySkeletonKeyToken(encoded);
        Assert.assertTrue(token.getResourceAccess("service").getRoles().contains("admin"));
        Assert.assertEquals("CN=Client", token.getSubject());
    }

    private AccessToken verifySkeletonKeyToken(String encoded) throws VerificationException {
        return RSATokenVerifier.verifyToken(encoded, idpPair.getPublic(), "domain");
    }

   /*
   @Test
   public void testSpeed() throws Exception
   {

      byte[] tokenBytes = JsonSerialization.toByteArray(token, false);

      String encoded = new JWSBuilder()
              .content(tokenBytes)
              .rsa256(idpPair.getPrivate());

      long start = System.currentTimeMillis();
      int count = 10000;
      for (int i = 0; i < count; i++)
      {
         SkeletonKeyTokenVerification v = RSATokenVerifier.verify(null, encoded, metadata);

      }
      long end = System.currentTimeMillis() - start;
      System.out.println("rate: " + ((double)end/(double)count));
   }
   */


    @Test
    public void testBadSignature() throws Exception {

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
    public void testNotBeforeBad() throws Exception {
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
    public void testExpirationBad() throws Exception {
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
    public void testTokenAuth() throws Exception {
        token = new AccessToken();
        token.subject("CN=Client")
                .issuer("domain")
                .addAccess("service").addRole("admin").verifyCaller(true);

        String encoded = new JWSBuilder()
                .jsonContent(token)
                .rsa256(idpPair.getPrivate());

        AccessToken v = null;
        try {
            v = verifySkeletonKeyToken(encoded);
        } catch (VerificationException ignored) {
        }
    }


}

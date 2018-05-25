package org.keycloak;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.SignatureException;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.Time;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.representations.AccessToken;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.TokenUtil;

import org.junit.Assert;

// KEYCLOAK-6770 JWS signatures using PS256 or ES256 algorithms for signing
public class ECDSAVerifierTest {
    private static KeyPair idpPair;
    private static KeyPair badPair;
    private AccessToken token;

    static {
        if (Security.getProvider("BC") == null) Security.addProvider(new BouncyCastleProvider());
    }

    @BeforeClass
    public static void setupCerts() throws NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {
        badPair = KeyPairGenerator.getInstance("EC").generateKeyPair();
        idpPair = KeyPairGenerator.getInstance("EC").generateKeyPair();
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
                .ecdsa256(idpPair.getPrivate());
        System.out.println("encoded: " + encoded);
        System.out.println("encoded size: " + encoded.length());
        AccessToken token = verifySkeletonKeyToken(encoded);
        Assert.assertTrue(token.getResourceAccess("service").getRoles().contains("admin"));
        Assert.assertEquals("CN=Client", token.getSubject());
    }

    private AccessToken verifySkeletonKeyToken(String encoded) throws VerificationException {
    	// NOTE : if signed by ECDSA and call RSAVerifier, here checks algorithm type from token and call ECDSAProvier.verify
        return ECDSATokenVerifier.create(encoded).publicKey(idpPair.getPublic()).realmUrl("http://localhost:8080/auth/realm").verify().getToken();
    }

    //@Test
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
                   .ecdsa256(idpPair.getPrivate());

           verifySkeletonKeyToken(encoded);

       }
       long end = System.currentTimeMillis() - start;
       System.out.println("took: " + end);
    }

    @Test
    public void testBadSignature() throws Exception {

        String encoded = new JWSBuilder()
                .jsonContent(token)
                .ecdsa256(badPair.getPrivate());

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
                .ecdsa256(idpPair.getPrivate());

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
                .ecdsa256(idpPair.getPrivate());

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
                .ecdsa256(idpPair.getPrivate());

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
                .ecdsa256(idpPair.getPrivate());

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
                .issuer("http://localhost:8080/auth/realms/demo")
                .addAccess("service").addRole("admin").verifyCaller(true);
        token.setEmail("bill@jboss.org");

        String encoded = new JWSBuilder()
                .jsonContent(token)
                .ecdsa256(idpPair.getPrivate());

        System.out.println("token size: " + encoded.length());

        AccessToken v = null;
        try {
            v = verifySkeletonKeyToken(encoded);
        } catch (VerificationException ignored) {
        }
    }
}

package org.keycloak.jose.jwk;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyUse;
import org.keycloak.rule.CryptoInitRule;
import org.keycloak.util.JsonSerialization;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

public abstract class AKPJWKTest {

    // There is a chance that two keys are generated starting with the same byte, hence generating multiple keys to take the common prefix from all
    private static final int KEYS_TO_GENERATE = 5;

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    @Test
    public void parseMLS_DSA_44() throws IOException {
        testDecodingAndEncodingPublicKey(Algorithm.ML_DSA_44, AKPSamples.ML_DSA_44);
    }

    @Test
    public void parseMLS_DSA_65() throws IOException {
        testDecodingAndEncodingPublicKey(Algorithm.ML_DSA_65, AKPSamples.ML_DSA_65);
    }

    @Test
    public void parseMLS_DSA_87() throws IOException {
        testDecodingAndEncodingPublicKey(Algorithm.ML_DSA_87, AKPSamples.ML_DSA_87);
    }

    @Test
    public void testPrefixMLS_DSA_44() throws NoSuchAlgorithmException {
        testPrefix(Algorithm.ML_DSA_44);
    }

    @Test
    public void testPrefixMLS_DSA_65() throws NoSuchAlgorithmException {
        testPrefix(Algorithm.ML_DSA_65);
    }

    @Test
    public void testPrefixMLS_DSA_87() throws NoSuchAlgorithmException {
        testPrefix(Algorithm.ML_DSA_87);
    }

    private void testDecodingAndEncodingPublicKey(String algorithm, String sampleJwk) throws IOException {
        JWK jwk = JsonSerialization.readValue(sampleJwk, JWK.class);

        PublicKey publicKey = JWKParser.create(jwk).toPublicKey();

        Assert.assertTrue(publicKey.getAlgorithm().startsWith("ML-DSA"));

        JWK akp = JWKBuilder.create().algorithm(algorithm).kid(jwk.getKeyId()).akp(publicKey);

        Assert.assertEquals(algorithm, akp.getAlgorithm());
        Assert.assertEquals(KeyType.AKP, akp.getKeyType());
        Assert.assertEquals(KeyUse.SIG.getSpecName(), akp.getPublicKeyUse());
        Assert.assertEquals(jwk.getKeyId(), akp.getKeyId());
        Assert.assertEquals(jwk.getOtherClaim(AKPPublicJWK.PUB, String.class), akp.getOtherClaim(AKPPublicJWK.PUB, String.class));
    }

    private JWK getJwk(String algorithm) throws IOException {
        InputStream inputStream = getClass().getResourceAsStream(algorithm.replace('-', '_') + ".jose.json");
        return JsonSerialization.readValue(inputStream, JWK.class);
    }

    private void testPrefix(String algorithm) throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(algorithm);

        byte[] expectedPrefix = kpg.generateKeyPair().getPublic().getEncoded();

        for (int i = 0; i < KEYS_TO_GENERATE; i++) {
            byte[] bytes2 = kpg.generateKeyPair().getPublic().getEncoded();
            expectedPrefix = findMatchingPrefix(expectedPrefix, bytes2);
        }

        Assert.assertArrayEquals(AKPUtils.PREFIXES.get(algorithm), expectedPrefix);
    }

    private static byte[] findMatchingPrefix(byte[] a, byte[] b) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        for (int i = 0; i < a.length && a[i] == b[i]; i++) {
            bos.write(a[i]);
        }
        return bos.toByteArray();
    }

}

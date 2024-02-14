package org.keycloak.crypto.fips.test;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.fips.BCFIPSECDSACryptoProvider;
import org.keycloak.keys.AbstractEcdsaKeyProviderFactory;
import org.keycloak.rule.CryptoInitRule;

import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class BCFIPSECDSACryptoProviderTest {

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {Algorithm.ES256}, {Algorithm.ES384}, {Algorithm.ES512}
        });
    }

    private String algorithm;

    public BCFIPSECDSACryptoProviderTest(String algorithm) {
        this.algorithm = algorithm;
    }

    @Test
    public void getPublicFromPrivate() {
        KeyPair testKey = generateECKey(algorithm);

        BCFIPSECDSACryptoProvider bcfipsecdsaCryptoProvider = new BCFIPSECDSACryptoProvider();
        ECPublicKey derivedKey = bcfipsecdsaCryptoProvider.getPublicFromPrivate((ECPrivateKey) testKey.getPrivate());
        assertEquals("The derived key should be equal to the originally generated one.",
                testKey.getPublic(),
                derivedKey);
    }

    public static KeyPair generateECKey(String algorithm) {

        try {
            KeyPairGenerator kpg = CryptoIntegration.getProvider().getKeyPairGen("ECDSA");
            String domainParamNistRep = AbstractEcdsaKeyProviderFactory.convertAlgorithmToECDomainParmNistRep(algorithm);
            String curve = AbstractEcdsaKeyProviderFactory.convertECDomainParmNistRepToSecRep(domainParamNistRep);
            ECGenParameterSpec parameterSpec = new ECGenParameterSpec(curve);
            kpg.initialize(parameterSpec);
            return kpg.generateKeyPair();
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
    }
}
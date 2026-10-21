package org.keycloak.crypto.elytron.test;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.PSSParameterSpec;

import org.keycloak.crypto.KeyWrapper;

import org.junit.Test;

public class ElytronSignatureAlgTest {

    private byte[] data = "Test String to Encrypt".getBytes();

    @Test
    public void signatureDefaultAlg() throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, InvalidAlgorithmParameterException, InvalidKeySpecException {
        KeyPair keyPair = KeyPairGenerator.getInstance("RSASSA-PSS").genKeyPair();
        KeyWrapper key = new KeyWrapper();
            //key.setPrivateKey(keyPair.getPrivate());
            key.setAlgorithm("PS256");

        KeySpec kspec = new PKCS8EncodedKeySpec(keyPair.getPrivate().getEncoded());
        key.setPrivateKey(KeyFactory.getInstance("RSASSA-PSS").generatePrivate(kspec));
            
        Signature signature = Signature.getInstance("RSASSA-PSS");
        MGF1ParameterSpec ps = MGF1ParameterSpec.SHA256;
        AlgorithmParameterSpec params = new PSSParameterSpec(ps.getDigestAlgorithm(), "MGF1", ps, 32, 1);
        
        signature.setParameter(params);
        signature.initSign(keyPair.getPrivate());
        //signature.initSign((PrivateKey) key.getPrivateKey());
        signature.update(data);
        System.out.println(signature.getProvider() + "  Alg ###########");
        if(signature.getAlgorithm().equals("RSASSA-PSS")) {
        }
        signature.sign();
    }
    
}

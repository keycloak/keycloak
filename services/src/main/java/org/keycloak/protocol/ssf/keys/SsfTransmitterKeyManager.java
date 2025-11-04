package org.keycloak.protocol.ssf.keys;

import org.keycloak.protocol.ssf.event.parser.SsfParsingException;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class SsfTransmitterKeyManager {

    public static PublicKey decodePublicKey(String key, String keyType, String alg){
        try{
            byte[] byteKey = Base64.getDecoder().decode(key);
            X509EncodedKeySpec X509publicKey = new X509EncodedKeySpec(byteKey);

            KeyFactory kf = KeyFactory.getInstance(keyType);
            return kf.generatePublic(X509publicKey);
        }
        catch(Exception e){
            throw new SsfParsingException("Could not decode public key", e);
        }
    }
}

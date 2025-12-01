package org.keycloak.crypto.def;

import java.security.AlgorithmParameters;
import java.security.Key;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;


public class DefaultRsaKeyEncryption256JWEAlgorithmProvider extends DefaultRsaKeyEncryptionJWEAlgorithmProvider {

    public DefaultRsaKeyEncryption256JWEAlgorithmProvider(String jcaAlgorithmName) {
        super(jcaAlgorithmName);
    }

    @Override
    protected void initCipher(Cipher cipher, int mode, Key key) throws Exception {
        AlgorithmParameters algp = AlgorithmParameters.getInstance("OAEP");
        AlgorithmParameterSpec paramSpec = new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256,
                PSource.PSpecified.DEFAULT);
        algp.init(paramSpec);
        cipher.init(mode, key, algp);
    }
}

package org.keycloak.crypto;

import org.keycloak.jose.jwe.JWEException;
import org.keycloak.jose.jwe.JWEHeader;
import org.keycloak.jose.jwe.alg.JWEAlgorithmProvider;
import org.keycloak.jose.jwe.enc.JWEEncryptionProvider;
import org.keycloak.util.TokenUtil;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;

public class DefaultDecryptionVerifierContext implements DecryptionVerifierContext{

    private final DecryptionKEKAccessor decryptionKEKAccessor;
    private final JWEAlgorithmProviderAccessor algoProviderAccessor;
    private final JWEEncryptionProviderAccessor encProviderAccessor;

    public DefaultDecryptionVerifierContext(DecryptionKEKAccessor decryptionKEKAccessor, JWEAlgorithmProviderAccessor algoProviderAccessor, JWEEncryptionProviderAccessor encProviderAccessor) {
        this.decryptionKEKAccessor = decryptionKEKAccessor;
        this.algoProviderAccessor = algoProviderAccessor;
        this.encProviderAccessor = encProviderAccessor;
    }

    @Override
    public String decrypt(String jweString, JWEHeader jweHeader) throws JWEException {

        JWEAlgorithmProvider algorithmProvider = algoProviderAccessor.getJweAlgorithmProvider(jweHeader.getAlgorithm());
        JWEEncryptionProvider encryptionProvider = encProviderAccessor.getJweEncryptionProvider(jweHeader.getEncryptionAlgorithm());
        PrivateKey decryptionKEK = decryptionKEKAccessor.getKEK(jweHeader.getKeyId(), jweHeader.getEncryptionAlgorithm());

        byte[] decodedIdTokenString = TokenUtil.jweKeyEncryptionVerifyAndDecode(decryptionKEK, jweString, algorithmProvider, encryptionProvider);
        return new String(decodedIdTokenString, StandardCharsets.UTF_8);
    }
}

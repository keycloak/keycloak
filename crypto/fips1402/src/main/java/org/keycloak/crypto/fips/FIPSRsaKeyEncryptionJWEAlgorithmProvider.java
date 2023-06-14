package org.keycloak.crypto.fips;

import java.security.Key;
import java.security.SecureRandom;

import org.bouncycastle.crypto.KeyUnwrapperUsingSecureRandom;
import org.bouncycastle.crypto.KeyWrapperUsingSecureRandom;
import org.bouncycastle.crypto.asymmetric.AsymmetricRSAPrivateKey;
import org.bouncycastle.crypto.asymmetric.AsymmetricRSAPublicKey;
import org.bouncycastle.crypto.fips.FipsRSA;
import org.keycloak.jose.jwe.JWEKeyStorage;
import org.keycloak.jose.jwe.alg.JWEAlgorithmProvider;
import org.keycloak.jose.jwe.enc.JWEEncryptionProvider;

/**
 * Fips note: Based on https://downloads.bouncycastle.org/fips-java/BC-FJA-UserGuide-1.0.2.pdf, Section 4
 * There are no direct public/private key ciphers available in approved mode. Available ciphers are
 * restricted to use for key wrapping and key transport, see section 7 and section 8 for details.
 * Our solution is to pull out the CEK signature and encryption keys , encode them separately , and then
 */
public class FIPSRsaKeyEncryptionJWEAlgorithmProvider implements JWEAlgorithmProvider {

    private final FipsRSA.WrapParameters wrapParameters;

    public FIPSRsaKeyEncryptionJWEAlgorithmProvider(FipsRSA.WrapParameters wrapParameters) {
        this.wrapParameters = wrapParameters;
    }

    @Override
    public byte[] decodeCek(byte[] encodedCek, Key privateKey) throws Exception {
        AsymmetricRSAPrivateKey rsaPrivateKey =
                new AsymmetricRSAPrivateKey(FipsRSA.ALGORITHM, privateKey.getEncoded());

        FipsRSA.KeyWrapOperatorFactory wrapFact =
                new FipsRSA.KeyWrapOperatorFactory();
        KeyUnwrapperUsingSecureRandom<FipsRSA.WrapParameters> unwrapper =
                wrapFact.createKeyUnwrapper(rsaPrivateKey, wrapParameters)
                        .withSecureRandom(SecureRandom.getInstance("DEFAULT"));
        return unwrapper.unwrap(encodedCek, 0, encodedCek.length);
    }


    @Override
    public byte[] encodeCek(JWEEncryptionProvider encryptionProvider, JWEKeyStorage keyStorage, Key publicKey) throws Exception {
        AsymmetricRSAPublicKey rsaPubKey =
                new AsymmetricRSAPublicKey(FipsRSA.ALGORITHM, publicKey.getEncoded());
        byte[] inputKeyBytes = keyStorage.getCekBytes();
        FipsRSA.KeyWrapOperatorFactory wrapFact =
                new FipsRSA.KeyWrapOperatorFactory();

        KeyWrapperUsingSecureRandom<FipsRSA.WrapParameters> wrapper =
                wrapFact.createKeyWrapper(rsaPubKey, wrapParameters).withSecureRandom( SecureRandom.getInstance("DEFAULT"));
        return wrapper.wrap(inputKeyBytes, 0, inputKeyBytes.length);
    }

}

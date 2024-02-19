package org.keycloak.common.crypto;

import java.io.IOException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

public interface ECDSACryptoProvider {
    
    public byte[] concatenatedRSToASN1DER(final byte[] signature, int signLength) throws IOException;

    public byte[] asn1derToConcatenatedRS(final byte[] derEncodedSignatureValue, int signLength) throws IOException;

    public ECPublicKey getPublicFromPrivate(ECPrivateKey ecPrivateKey);
}

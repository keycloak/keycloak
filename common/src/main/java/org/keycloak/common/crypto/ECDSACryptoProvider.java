package org.keycloak.common.crypto;

import java.io.IOException;

public interface ECDSACryptoProvider {


    public byte[] concatenatedRSToASN1DER(final byte[] signature, int signLength) throws IOException;

    public byte[] asn1derToConcatenatedRS(final byte[] derEncodedSignatureValue, int signLength) throws IOException;

    
}

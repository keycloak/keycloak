/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.crypto.elytron;

import java.io.IOException;
import java.math.BigInteger;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

import org.keycloak.common.crypto.ECDSACryptoProvider;

import org.jboss.logging.Logger;
import org.wildfly.security.asn1.DERDecoder;
import org.wildfly.security.asn1.DEREncoder;

/**
 * @author <a href="mailto:david.anderson@redhat.com">David Anderson</a>
 */
public class ElytronECDSACryptoProvider implements ECDSACryptoProvider {

    Logger log = Logger.getLogger(getClass());

    @Override
    public byte[] concatenatedRSToASN1DER(final byte[] signature, int signLength) throws IOException {
        int len = signLength / 2;
        int arraySize = len + 1;

        byte[] r = new byte[arraySize];
        byte[] s = new byte[arraySize];
        System.arraycopy(signature, 0, r, 1, len);
        System.arraycopy(signature, len, s, 1, len);
        BigInteger rBigInteger = new BigInteger(r);
        BigInteger sBigInteger = new BigInteger(s);

        DEREncoder seq = new DEREncoder();

        seq.startSequence();
        seq.encodeInteger(rBigInteger);
        seq.encodeInteger(sBigInteger);
        seq.endSequence();

        return seq.getEncoded();

    }

    @Override
    public byte[] asn1derToConcatenatedRS(final byte[] derEncodedSignatureValue, int signLength) throws IOException {
        int len = signLength / 2;

        DERDecoder der = new DERDecoder(derEncodedSignatureValue);
        der.startSequence();
        byte[] r = convertToBytes(der.decodeInteger(), len);
        byte[] s = convertToBytes(der.decodeInteger(), len);
        der.endSequence();
        byte[] concatenatedSignatureValue = new byte[signLength];

        System.arraycopy(r, 0, concatenatedSignatureValue, 0, len);
        System.arraycopy(s, 0, concatenatedSignatureValue, len, len);

        return concatenatedSignatureValue;
    }

    @Override
    public ECPublicKey getPublicFromPrivate(ECPrivateKey ecPrivateKey) {
        throw new UnsupportedOperationException("Elytron Crypto Provider currently does not support extraction of EC Public Keys.");
    }

    // If byte array length doesn't match expected length, copy to new
    // byte array of the expected length
    private byte[] convertToBytes(BigInteger decodeInteger, int len) {

        byte[] bytes = decodeInteger.toByteArray();

        if (len < bytes.length) {
            log.debug("Decoded integer byte length greater than expected.");
            byte[] t = new byte[len];
            System.arraycopy(bytes, bytes.length - len, t, 0, len);
            bytes = t;
        } else if (len > bytes.length) {
            log.debug("Decoded integer byte length less than expected.");
            byte[] t = new byte[len];
            System.arraycopy(bytes, 0, t, len - bytes.length, bytes.length);
            bytes = t;
        }
        return bytes;
    }

}

/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.jose.jwk;

import org.keycloak.common.util.Base64Url;

import java.math.BigInteger;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JWKBuilder {

    public static final String DEFAULT_PUBLIC_KEY_USE = "sig";
    public static final String DEFAULT_MESSAGE_DIGEST = "SHA-256";


    private JWKBuilder() {
    }

    public static JWKBuilder create() {
        return new JWKBuilder();
    }

    public JWK rs256(PublicKey key) {
        RSAPublicKey rsaKey = (RSAPublicKey) key;

        RSAPublicJWK k = new RSAPublicJWK();
        k.setKeyId(createKeyId(key));
        k.setKeyType(RSAPublicJWK.RSA);
        k.setAlgorithm(RSAPublicJWK.RS256);
        k.setPublicKeyUse(DEFAULT_PUBLIC_KEY_USE);
        k.setModulus(Base64Url.encode(toIntegerBytes(rsaKey.getModulus())));
        k.setPublicExponent(Base64Url.encode(toIntegerBytes(rsaKey.getPublicExponent())));

        return k;
    }

    private String createKeyId(Key key) {
        try {
            return Base64Url.encode(MessageDigest.getInstance(DEFAULT_MESSAGE_DIGEST).digest(key.getEncoded()));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Copied from org.apache.commons.codec.binary.Base64
     */
    private static byte[] toIntegerBytes(final BigInteger bigInt) {
        int bitlen = bigInt.bitLength();
        // round bitlen
        bitlen = ((bitlen + 7) >> 3) << 3;
        final byte[] bigBytes = bigInt.toByteArray();

        if (((bigInt.bitLength() % 8) != 0) && (((bigInt.bitLength() / 8) + 1) == (bitlen / 8))) {
            return bigBytes;
        }
        // set up params for copying everything but sign bit
        int startSrc = 0;
        int len = bigBytes.length;

        // if bigInt is exactly byte-aligned, just skip signbit in copy
        if ((bigInt.bitLength() % 8) == 0) {
            startSrc = 1;
            len--;
        }
        final int startDst = bitlen / 8 - len; // to pad w/ nulls as per spec
        final byte[] resizedBytes = new byte[bitlen / 8];
        System.arraycopy(bigBytes, startSrc, resizedBytes, startDst, len);
        return resizedBytes;
    }

}

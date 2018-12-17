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
import org.keycloak.common.util.KeyUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyType;

import java.math.BigInteger;
import java.security.Key;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JWKBuilder {

    public static final String DEFAULT_PUBLIC_KEY_USE = "sig";

    private String kid;

    private String algorithm;

    private JWKBuilder() {
    }

    public static JWKBuilder create() {
        return new JWKBuilder();
    }

    public JWKBuilder kid(String kid) {
        this.kid = kid;
        return this;
    }

    public JWKBuilder algorithm(String algorithm) {
        this.algorithm = algorithm;
        return this;
    }

    public JWK rs256(PublicKey key) {
        algorithm(Algorithm.RS256);
        return rsa(key);
    }

    public JWK rsa(Key key) {
        RSAPublicKey rsaKey = (RSAPublicKey) key;

        RSAPublicJWK k = new RSAPublicJWK();

        String kid = this.kid != null ? this.kid : KeyUtils.createKeyId(key);
        k.setKeyId(kid);
        k.setKeyType(KeyType.RSA);
        k.setAlgorithm(algorithm);
        k.setPublicKeyUse(DEFAULT_PUBLIC_KEY_USE);
        k.setModulus(Base64Url.encode(toIntegerBytes(rsaKey.getModulus())));
        k.setPublicExponent(Base64Url.encode(toIntegerBytes(rsaKey.getPublicExponent())));

        return k;
    }


    public JWK ec(Key key) {
        ECPublicKey ecKey = (ECPublicKey) key;

        ECPublicJWK k = new ECPublicJWK();

        String kid = this.kid != null ? this.kid : KeyUtils.createKeyId(key);
        int fieldSize = ecKey.getParams().getCurve().getField().getFieldSize();
        int fieldSizeBytes = fieldSize % 8;
        k.setKeyId(kid);
        k.setKeyType(KeyType.EC);
        k.setAlgorithm(algorithm);
        k.setPublicKeyUse(DEFAULT_PUBLIC_KEY_USE);
        k.setCrv("P-" + fieldSize);
        
        byte[] affineX = ecKey.getW().getAffineX().toByteArray();
        byte[] affineY = ecKey.getW().getAffineY().toByteArray();

        // fieldSizeBytes + 1 = 33 bytes for P-256, 49 bytes for P-384, 65 bytes for P-521.
        if (affineX.length == fieldSizeBytes + 1) {
            byte[] x = new byte[fieldSizeBytes];
            System.arraycopy(affineX, 1, x, 0, fieldSizeBytes);
            k.setX(Base64Url.encode(x));
        } else {
            k.setX(Base64Url.encode(affineX));
        }

        if (affineY.length == fieldSizeBytes + 1) {
            byte[] y = new byte[fieldSizeBytes];
            System.arraycopy(affineY, 1, y, 0, fieldSizeBytes);
            k.setY(Base64Url.encode(y));

        } else {
            k.setY(Base64Url.encode(affineY));
        }
        return k;
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

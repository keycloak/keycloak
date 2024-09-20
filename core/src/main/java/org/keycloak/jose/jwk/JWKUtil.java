/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

import java.math.BigInteger;
import java.util.Arrays;

public class JWKUtil {

    /**
     * Converts {@code BigInteger} to 64-byte array removing the sign byte if
     * necessary.
     *
     * @param bigInt {@code BigInteger} to be converted
     * @return Byte array representation of the BigInteger parameter
     */
    public static byte[] toIntegerBytes(final BigInteger bigInt) {
        return toIntegerBytes(bigInt, bigInt.bitLength());
    }

    /**
     * Converts {@code BigInteger} to 64-byte array but maintaining the length
     * to bitlen as specified in rfc7518 for certain fields (X and Y parameter
     * for EC keys).
     *
     * @param bigInt {@code BigInteger} to be converted
     * @param bitlen The bit length size of the integer (for example 521 for EC P-521)
     * @return Byte array representation of the BigInteger parameter with length (bitlen + 7) / 8
     * @throws IllegalStateException if the big integer is longer than bitlen
     */
    public static byte[] toIntegerBytes(final BigInteger bigInt, int bitlen) {
        assert bigInt.bitLength() <= bitlen : "Incorrect big integer with bit length " + bigInt.bitLength() + " for " + bitlen;
        final int bytelen = (bitlen + 7) / 8;
        final byte[] array = bigInt.toByteArray();
        if (array.length == bytelen) {
            // expected number of bytes, return them
            return array;
        } else if (bytelen < array.length) {
            // if array is greater is because the sign bit (it can be only 1 byte more), remove it
            return Arrays.copyOfRange(array, array.length - bytelen, array.length);
        } else {
            // if array is smaller fill it with zeros
            final byte[] resizedBytes = new byte[bytelen];
            System.arraycopy(array, 0, resizedBytes, bytelen - array.length, array.length);
            return resizedBytes;
        }
    }
}

/**
 * MIT License
 *
 * Copyright (c) 2017 Sebastian Ruhleder
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.keycloak.common.util;

import java.io.ByteArrayOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;


/*
 * Base62 encoding/decoding (based on https://github.com/seruco/base62)
 */
public class Base62 {

    private static final int STANDARD_BASE = 256;

    private static final int TARGET_BASE = 62;

    private static final byte[] alphabet = {
            (byte) '0', (byte) '1', (byte) '2', (byte) '3', (byte) '4', (byte) '5', (byte) '6', (byte) '7',
            (byte) '8', (byte) '9', (byte) 'A', (byte) 'B', (byte) 'C', (byte) 'D', (byte) 'E', (byte) 'F',
            (byte) 'G', (byte) 'H', (byte) 'I', (byte) 'J', (byte) 'K', (byte) 'L', (byte) 'M', (byte) 'N',
            (byte) 'O', (byte) 'P', (byte) 'Q', (byte) 'R', (byte) 'S', (byte) 'T', (byte) 'U', (byte) 'V',
            (byte) 'W', (byte) 'X', (byte) 'Y', (byte) 'Z', (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd',
            (byte) 'e', (byte) 'f', (byte) 'g', (byte) 'h', (byte) 'i', (byte) 'j', (byte) 'k', (byte) 'l',
            (byte) 'm', (byte) 'n', (byte) 'o', (byte) 'p', (byte) 'q', (byte) 'r', (byte) 's', (byte) 't',
            (byte) 'u', (byte) 'v', (byte) 'w', (byte) 'x', (byte) 'y', (byte) 'z'
    };

    private static final byte[] lookup = createLookupTable();

    /**
     * Encodes a sequence of bytes in Base62 encoding.
     *
     * @param message a byte sequence.
     * @return a sequence of Base62-encoded bytes.
     */
    public static byte[] encode(final byte[] message) {
        final byte[] indices = convert(message, STANDARD_BASE, TARGET_BASE);

        return translate(indices, alphabet);
    }

    public static String encodeToString(final byte[] message) {
        byte[] encoded = encode(message);
        return new String(encoded, UTF_8);
    }

    public static byte[] decode(final String encoded) {
        return decode(encoded.getBytes(UTF_8));
    }

    /**
     * Decodes a sequence of Base62-encoded bytes.
     *
     * @param encoded a sequence of Base62-encoded bytes.
     * @return a byte sequence.
     * @throws IllegalArgumentException when {@code encoded} is not encoded over the Base62 alphabet.
     */
    public static byte[] decode(final byte[] encoded) {
        final byte[] prepared = translate(encoded, lookup);

        return convert(prepared, TARGET_BASE, STANDARD_BASE);
    }

    /**
     * Uses the elements of a byte array as indices to a dictionary and returns the corresponding values
     * in form of a byte array.
     */
    private static byte[] translate(final byte[] indices, final byte[] dictionary) {
        final byte[] translation = new byte[indices.length];

        for (int i = 0; i < indices.length; i++) {
            translation[i] = dictionary[indices[i]];
        }

        return translation;
    }

    /**
     * Converts a byte array from a source base to a target base using the alphabet.
     */
    private static byte[] convert(final byte[] message, final int sourceBase, final int targetBase) {
        /* This algorithm is inspired by: http://codegolf.stackexchange.com/a/21672 */

        final int estimatedLength = estimateOutputLength(message.length, sourceBase, targetBase);

        final ByteArrayOutputStream out = new ByteArrayOutputStream(estimatedLength);

        byte[] source = message;

        while (source.length > 0) {
            final ByteArrayOutputStream quotient = new ByteArrayOutputStream(source.length);

            int remainder = 0;

            for (byte b : source) {
                final int accumulator = (b & 0xFF) + remainder * sourceBase;
                final int digit = (accumulator - (accumulator % targetBase)) / targetBase;

                remainder = accumulator % targetBase;

                if (quotient.size() > 0 || digit > 0) {
                    quotient.write(digit);
                }
            }

            out.write(remainder);

            source = quotient.toByteArray();
        }

        // pad output with zeroes corresponding to the number of leading zeroes in the message
        for (int i = 0; i < message.length - 1 && message[i] == 0; i++) {
            out.write(0);
        }

        return reverse(out.toByteArray());
    }

    /**
     * Estimates the length of the output in bytes.
     */
    private static int estimateOutputLength(int inputLength, int sourceBase, int targetBase) {
        return (int) Math.ceil((Math.log(sourceBase) / Math.log(targetBase)) * inputLength);
    }

    /**
     * Reverses a byte array.
     */
    private static byte[] reverse(final byte[] arr) {
        final int length = arr.length;

        final byte[] reversed = new byte[length];

        for (int i = 0; i < length; i++) {
            reversed[length - i - 1] = arr[i];
        }

        return reversed;
    }

    /**
     * Creates the lookup table from character to index of character in character set.
     */
    private static byte[] createLookupTable() {
        byte[] lookup = new byte[256];

        for (int i = 0; i < alphabet.length; i++) {
            lookup[alphabet[i]] = (byte) (i & 0xFF);
        }
        return lookup;
    }

}

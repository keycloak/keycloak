/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.authorization.client.util.crypto;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author rmartinc
 */
class ASN1Decoder {

    private final ByteArrayInputStream is;
    private final int limit;
    private int count;

    ASN1Decoder(byte[] bytes) {
        is = new ByteArrayInputStream(bytes);
        count = 0;
        limit = bytes.length;
    }

    public static ASN1Decoder create(byte[] bytes) {
        return new ASN1Decoder(bytes);
    }

    public List<byte[]> readSequence() throws IOException {
        int tag = readTag();
        int tagNo = readTagNumber(tag);
        if (tagNo != ASN1Encoder.SEQUENCE) {
            throw new IOException("Invalid Sequence tag " + tagNo);
        }
        int length = readLength();
        List<byte[]> result = new ArrayList<>();
        while (length > 0) {
            byte[] bytes = readNext();
            result.add(bytes);
            length = length - bytes.length;
        }
        return result;
    }

    public BigInteger readInteger() throws IOException {
        int tag = readTag();
        int tagNo = readTagNumber(tag);
        if (tagNo != ASN1Encoder.INTEGER) {
            throw new IOException("Invalid Integer tag " + tagNo);
        }
        int length = readLength();
        byte[] bytes = read(length);
        return new BigInteger(bytes);
    }

    byte[] readNext() throws IOException {
        mark();
        int tag = readTag();
        readTagNumber(tag);
        int length = readLength();
        length += reset();
        return read(length);
    }

    int readTag() throws IOException {
        int tag = read();
        if (tag < 0) {
            throw new EOFException("EOF found inside tag value.");
        }
        return tag;
    }

    int readTagNumber(int tag) throws IOException {
        int tagNo = tag & 0x1f;

        //
        // with tagged object tag number is bottom 5 bits, or stored at the start of the content
        //
        if (tagNo == 0x1f) {
            tagNo = 0;

            int b = read();

            // X.690-0207 8.1.2.4.2
            // "c) bits 7 to 1 of the first subsequent octet shall not all be zero."
            if ((b & 0x7f) == 0) // Note: -1 will pass
            {
                throw new IOException("corrupted stream - invalid high tag number found");
            }

            while ((b >= 0) && ((b & 0x80) != 0)) {
                tagNo |= (b & 0x7f);
                tagNo <<= 7;
                b = read();
            }

            if (b < 0) {
                throw new EOFException("EOF found inside tag value.");
            }

            tagNo |= (b & 0x7f);
        }

        return tagNo;
    }

    int readLength() throws IOException {
        int length = read();
        if (length < 0) {
            throw new EOFException("EOF found when length expected");
        }

        if (length == 0x80) {
            return -1;      // indefinite-length encoding
        }

        if (length > 127) {
            int size = length & 0x7f;

            // Note: The invalid long form "0xff" (see X.690 8.1.3.5c) will be caught here
            if (size > 4) {
                throw new IOException("DER length more than 4 bytes: " + size);
            }

            length = 0;
            for (int i = 0; i < size; i++) {
                int next = read();

                if (next < 0) {
                    throw new EOFException("EOF found reading length");
                }

                length = (length << 8) + next;
            }

            if (length < 0) {
                throw new IOException("corrupted stream - negative length found");
            }

            if (length >= limit) // after all we must have read at least 1 byte
            {
                throw new IOException("corrupted stream - out of bounds length found");
            }
        }

        return length;
    }

    byte[] read(int length) throws IOException {
        byte[] bytes = new byte[length];
        int totalBytesRead = 0;

        while (totalBytesRead < length) {
            int bytesRead = is.read(bytes, totalBytesRead, length - totalBytesRead);
            if (bytesRead == -1) {
                throw new IOException(String.format("EOF found reading %d bytes", length));
            }
            totalBytesRead += bytesRead;
        }
        count += length;
        return bytes;
    }

    void mark() {
        count = 0;
        is.mark(is.available());
    }

    int reset() {
        int tmp = count;
        is.reset();
        return tmp;
    }

    int read() {
        int tmp = is.read();
        if (tmp >= 0) {
            count++;
        }
        return tmp;
    }
}

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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;

/**
 *
 * @author rmartinc
 */
class ASN1Encoder {

    static final int INTEGER = 0x02;
    static final int SEQUENCE = 0x10;
    static final int CONSTRUCTED = 0x20;

    private final ByteArrayOutputStream os;

    private ASN1Encoder() {
        this.os = new ByteArrayOutputStream();
    }

    static public ASN1Encoder create() {
        return new ASN1Encoder();
    }

    public ASN1Encoder write(BigInteger value) throws IOException {
        writeEncoded(INTEGER, value.toByteArray());
        return this;
    }

    public ASN1Encoder writeDerSeq(ASN1Encoder... objects) throws IOException {
        writeEncoded(CONSTRUCTED | SEQUENCE, concatenate(objects));
        return this;
    }

    public byte[] toByteArray() {
        return os.toByteArray();
    }

    void writeEncoded(int tag, byte[] bytes) throws IOException {
        write(tag);
        writeLength(bytes.length);
        write(bytes);
    }

    void writeLength(int length) throws IOException {
        if (length > 127) {
            int size = 1;
            int val = length;

            while ((val >>>= 8) != 0) {
                size++;
            }

            write((byte) (size | 0x80));

            for (int i = (size - 1) * 8; i >= 0; i -= 8) {
                write((byte) (length >> i));
            }
        } else {
            write((byte) length);
        }
    }

    void write(byte[] bytes) throws IOException {
        os.write(bytes);
    }

    void write(int b) throws IOException {
        os.write(b);
    }

    byte[] concatenate(ASN1Encoder... objects) throws IOException {
        ByteArrayOutputStream tmp = new ByteArrayOutputStream();
        for (ASN1Encoder object : objects) {
            tmp.write(object.toByteArray());
        }
        return tmp.toByteArray();
    }
}

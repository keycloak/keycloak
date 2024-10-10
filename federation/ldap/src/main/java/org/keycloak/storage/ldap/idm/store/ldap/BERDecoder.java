/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.storage.ldap.idm.store.ldap;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;


/**
 * A decoder for the ASN.1 BER encoding.
 *
 * Very limited implementation, only supports what is needed by the current LDAP extension controls.
 */
public class BERDecoder {
    // Universal tags.
    public static final int TAG_SEQUENCE = 0x30;

    // Tag classes.
    public static final int TAG_CLASS_CONTEXT_SPECIFIC = 0x80;

    // Tag forms.
    public static final int TAG_FORM_PRIMITIVE = 0x00;

    private ByteBuffer encoded;

    public BERDecoder(byte[] encodedValue) {
        this.encoded = ByteBuffer.wrap(encodedValue);
    }

    /**
     * Start decoding a sequence.
     */
    public void startSequence() throws DecodeException {
        try {
            byte tag = encoded.get();
            if (tag != TAG_SEQUENCE) {
                throw new DecodeException("Expected SEQUENCE (" + TAG_SEQUENCE + ") but got " + tag);
            }
            readLength();
        } catch (BufferUnderflowException e) {
            throw new DecodeException("Unexpected end of input");
        }
    }

    /**
     * Check if the next element matches with the given tag, but do not consume it.
     */
    public boolean isNextTag(int clazz, int form, int tag) throws DecodeException {
        encoded.mark();
        try {
            int expected = clazz | form | tag;
            int unsignedTag = encoded.get() & 0xFF;
            encoded.reset();
            return unsignedTag == expected;
        } catch (BufferUnderflowException e) {
            throw new DecodeException("Unexpected end of input");
        } finally {
            encoded.reset();
        }
    }

    /**
     * Skip over the next element.
     */
    public void skipElement() throws DecodeException {
        try {
            int length = readLength();
            encoded.position(encoded.position() + length);
        } catch (BufferUnderflowException e) {
            throw new DecodeException("Unexpected end of input");
        }
    }

    /**
     * Drain the value bytes of the next element.
     */
    public byte[] drainElementValue() throws DecodeException {
        try {
            int length = readLength();
            byte[] value = new byte[length];
            encoded.get(value);
            return value;
        } catch (BufferUnderflowException e) {
            throw new DecodeException("Unexpected end of input");
        }
    }

    private int readLength() throws DecodeException {
        int length = encoded.get() & 0xFF;

        // Short form.
        if ((length & 0x80) == 0) {
            return length;
        }

        // Long form.
        int numBytes = length & 0x7F;
        if (numBytes > 4) {
            throw new DecodeException("Cannot handle more than 4 bytes of length, got " + numBytes + " bytes");
        }

        length = 0;
        for (int i = 0; i < numBytes; i++) {
            length = (length << 8) | (encoded.get() & 0xFF);
        }

        return length;
    }

    public static final class DecodeException extends IOException {
        DecodeException(String message) {
            super(message);
        }
    }

}

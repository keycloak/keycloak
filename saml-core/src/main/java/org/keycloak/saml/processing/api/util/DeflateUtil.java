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
package org.keycloak.saml.processing.api.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.keycloak.saml.common.constants.GeneralConstants;

/**
 * Encoder of saml messages based on DEFLATE compression
 *
 * @author Anil.Saldhana@redhat.com
 * @since Dec 11, 2008
 */
public class DeflateUtil {

    /**
     * Maximum size for inflating. Default is 128KB like quarkus.http.limits.max-form-attribute-size.
     */
    public static long DEFAULT_MAX_INFLATING_SIZE = 131072;

    private DeflateUtil() {
        // utility class
    }

    /**
     * Apply DEFLATE encoding
     *
     * @param message
     *
     * @return
     *
     * @throws IOException
     */
    public static byte[] encode(byte[] message) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Deflater deflater = new Deflater(Deflater.DEFLATED, true);
        DeflaterOutputStream deflaterStream = new DeflaterOutputStream(baos, deflater);
        deflaterStream.write(message);
        deflaterStream.finish();

        return baos.toByteArray();
    }

    /**
     * Apply DEFLATE encoding
     *
     * @param message
     *
     * @return
     *
     * @throws IOException
     */
    public static byte[] encode(String message) throws IOException {
        return encode(message.getBytes(GeneralConstants.SAML_CHARSET));
    }

    /**
     * DEFLATE decoding
     *
     * @param msgToDecode the message that needs decoding
     *
     * @return
     */
    public static InputStream decode(byte[] msgToDecode) {
        return decode(msgToDecode, DEFAULT_MAX_INFLATING_SIZE);
    }

    /**
     * DEFLATE decoding
     *
     * @param msgToDecode the message that needs decoding
     * @param maxInflatingSize the maximum size to inflate, IOExceptio is thrown if more data is inflated
     *
     * @return
     */
    public static InputStream decode(byte[] msgToDecode, long maxInflatingSize) {
        ByteArrayInputStream bais = new ByteArrayInputStream(msgToDecode);
        return new LimitedInflaterInputStream(bais, maxInflatingSize);
    }

    private static class LimitedInflaterInputStream extends InputStream {

        private final InflaterInputStream is;
        private final Inflater inflater;
        private final long maxInflatingSize;

        private LimitedInflaterInputStream(InputStream is, long maxInflatingSize) {
            this.inflater = new Inflater(true);
            this.is = new InflaterInputStream(is, inflater);
            this.maxInflatingSize = maxInflatingSize;
        }

        private void checkMaxInflatingsize() throws IOException {
            if (inflater.getBytesWritten() > maxInflatingSize) {
                throw new IOException(String.format("Maximum inflating size %d reached. Total bytes witten %d.",
                        maxInflatingSize, inflater.getTotalOut()));
            }
        }

        @Override
        public int read() throws IOException {
            int result = is.read();
            checkMaxInflatingsize();
            return result;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int result = is.read(b, off, len);
            checkMaxInflatingsize();
            return result;
        }

        @Override
        public int read(byte[] b) throws IOException {
            int result = is.read(b);
            checkMaxInflatingsize();
            return result;
        }

        @Override
        public boolean markSupported() {
            return false;
        }

        @Override
        public void reset() throws IOException {
            throw new IOException("mark/reset not supported");
        }

        @Override
        public void mark(int readlimit) {
            // nothing
        }

        @Override
        public void close() throws IOException {
            is.close();
        }

        @Override
        public int available() throws IOException {
            return is.available();
        }

        @Override
        public long skip(long n) throws IOException {
            return is.skip(n);
        }
    }
}
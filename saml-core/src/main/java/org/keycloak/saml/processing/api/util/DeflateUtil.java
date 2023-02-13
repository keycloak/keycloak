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

import org.keycloak.saml.common.constants.GeneralConstants;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * Encoder of saml messages based on DEFLATE compression
 *
 * @author Anil.Saldhana@redhat.com
 * @since Dec 11, 2008
 */
public class DeflateUtil {

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
        ByteArrayInputStream bais = new ByteArrayInputStream(msgToDecode);
        return new InflaterInputStream(bais, new Inflater(true));
    }
}
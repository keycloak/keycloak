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
package org.keycloak.saml.processing.web.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;
import org.keycloak.saml.common.constants.GeneralConstants;

/**
 * Utility for the HTTP/Post binding
 *
 * @author Anil.Saldhana@redhat.com
 * @since May 22, 2009
 */
public class PostBindingUtil {

    private static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    /**
     * Apply base64 encoding on the message
     *
     * @param stringToEncode
     *
     * @return
     */
    public static String base64Encode(String stringToEncode) throws IOException {
        return Base64.getEncoder().encodeToString(stringToEncode.getBytes(GeneralConstants.SAML_CHARSET));
    }

    /**
     * Apply base64 decoding on the message and return the byte array
     *
     * @param encodedString
     *
     * @return
     */
    public static byte[] base64Decode(String encodedString) {
        if (encodedString == null)
            throw logger.nullArgumentError("encodedString");

        try {
            return Base64.getDecoder().decode(encodedString);
        } catch (Exception e) {
            logger.error(e);
            throw logger.invalidArgumentError("base64 decode failed: " + e.getMessage());
        }
    }

    /**
     * Apply base64 decoding on the message and return the stream
     *
     * @param encodedString
     *
     * @return
     */
    public static InputStream base64DecodeAsStream(String encodedString) {
        if (encodedString == null)
            throw logger.nullArgumentError("encodedString");

        return new ByteArrayInputStream(base64Decode(encodedString));
    }

    public static String escapeHTML(String toEscape) {
        StringBuilder escaped = new StringBuilder();

        for (int i = 0; i < toEscape.length(); i++) {
            char chr = toEscape.charAt(i);

            if (chr != '"' && chr != '<' && chr != '>') {
                escaped.append(chr);
            }
        }

        return escaped.toString();
    }
}

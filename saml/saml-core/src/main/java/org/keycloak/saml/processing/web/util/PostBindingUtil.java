/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.saml.processing.web.util;

import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;
import org.keycloak.saml.common.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

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
        return Base64.encodeBytes(stringToEncode.getBytes("UTF-8"), Base64.DONT_BREAK_LINES);
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

        return Base64.decode(encodedString);
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
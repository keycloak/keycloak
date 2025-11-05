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

package org.keycloak.util;


import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * The default implementation is compliant with <a href="https://datatracker.ietf.org/doc/html/rfc2617">RFC 2617</a>
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class BasicAuthHelper {
    public static String createHeader(String username, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((username + ':' + password).getBytes(StandardCharsets.UTF_8));
    }

    public static String[] parseHeader(String header) {
        if (header.length() < 6) return null;

        String type = header.substring(0, 5);
        type = type.toLowerCase();
        if (!type.equalsIgnoreCase("Basic")) return null;

        String val;
        try {
            val = new String(Base64.getDecoder().decode(header.substring(6)));
        } catch (IllegalArgumentException e) {
            return null;
        }

        int separatorIndex = val.indexOf(":");
        if (separatorIndex == -1) return null;

        String username = val.substring(0, separatorIndex);
        String password = val.substring(separatorIndex + 1);

        return new String[]{ username, password };
    }

    /**
     * compliant with <a href="https://datatracker.ietf.org/doc/html/rfc6749#section-2.3.1">RFC 6749</a>
     */
    public static abstract class RFC6749 {

        public static String createHeader(String username, String password) {
            try {
                return BasicAuthHelper.createHeader(
                    URLEncoder.encode(username, "UTF-8"),
                    URLEncoder.encode(password, "UTF-8")
                );
            } catch (UnsupportedEncodingException e) {
                return null;
            }
        }

        public static String[] parseHeader(String header) {
            String[] val = BasicAuthHelper.parseHeader(header);
            if (null == val) {
                return null;
            }

            try {
                String username = val[0];
                String password = val[1];

                boolean usernameIsEncoded = containsValidUrlEncoding(username);
                boolean passwordIsEncoded = containsValidUrlEncoding(password);

                return new String[]{
                    usernameIsEncoded ? URLDecoder.decode(username, "UTF-8") : username,
                    passwordIsEncoded ? URLDecoder.decode(password, "UTF-8") : password
                };
            } catch (UnsupportedEncodingException e) {
                return null;
            } catch (IllegalArgumentException e) {
                return val;
            }
        }

        /**
         * Check if a string appears to be URL-encoded by looking for common URL-encoded patterns.
         * We check for percent-encoded sequences that are commonly used in URLs and would
         * be present if the string was URL-encoded.
         *
         */
        private static boolean containsValidUrlEncoding(String value) {
            if (value == null || value.isEmpty()) {
                return false;
            }
            return value.matches(".*%(?:2[0-9A-Fa-f]|3[0-9A-Fa-f]|40|21).*");
        }
    }
}

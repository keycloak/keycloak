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

package org.keycloak.common.util;


/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class Base64Url {
    public static String encode(byte[] bytes) {
        String s = Base64.encodeBytes(bytes);
        return encodeBase64ToBase64Url(s);
    }

    public static byte[] decode(String s) {
        s = encodeBase64UrlToBase64(s);
        try {
            return Base64.decode(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * @param base64 String in base64 encoding
     * @return String in base64Url encoding
     */
    public static String encodeBase64ToBase64Url(String base64) {
        String s = base64.split("=")[0]; // Remove any trailing '='s
        s = s.replace('+', '-'); // 62nd char of encoding
        s = s.replace('/', '_'); // 63rd char of encoding
        return s;
    }


    /**
     * @param base64Url String in base64Url encoding
     * @return String in base64 encoding
     */
    public static String encodeBase64UrlToBase64(String base64Url) {
        String s = base64Url.replace('-', '+'); // 62nd char of encoding
        s = s.replace('_', '/'); // 63rd char of encoding
        switch (s.length() % 4) // Pad with trailing '='s
        {
            case 0:
                break; // No pad chars in this case
            case 2:
                s += "==";
                break; // Two pad chars
            case 3:
                s += "=";
                break; // One pad char
            default:
                throw new RuntimeException(
                        "Illegal base64url string!");
        }

        return s;
    }


}

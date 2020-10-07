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

import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.util.Base64;
import org.keycloak.saml.common.util.StringUtil;
import org.keycloak.saml.processing.api.util.DeflateUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * Utility class for SAML HTTP/Redirect binding
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jan 14, 2009
 */
public class RedirectBindingUtil {

    /**
     * URL encode the string
     *
     * @param str
     *
     * @return
     *
     * @throws IOException
     */
    public static String urlEncode(String str) throws IOException {
        return URLEncoder.encode(str, GeneralConstants.SAML_CHARSET_NAME);
    }

    /**
     * URL decode the string
     *
     * @param str
     *
     * @return
     *
     * @throws IOException
     */
    public static String urlDecode(String str) throws IOException {
        return URLDecoder.decode(str, GeneralConstants.SAML_CHARSET_NAME);
    }

    /**
     * On the byte array, apply base64 encoding
     *
     * @param stringToEncode
     *
     * @return
     *
     * @throws IOException
     */
    public static String base64Encode(byte[] stringToEncode) throws IOException {
        return Base64.encodeBytes(stringToEncode, Base64.DONT_BREAK_LINES);
    }

    /**
     * On the byte array, apply base64 encoding following by URL encoding
     *
     * @param stringToEncode
     *
     * @return
     *
     * @throws IOException
     */
    public static String base64URLEncode(byte[] stringToEncode) throws IOException {
        String base64Request = Base64.encodeBytes(stringToEncode, Base64.DONT_BREAK_LINES);
        return urlEncode(base64Request);
    }

    /**
     * On the byte array, apply URL decoding followed by base64 decoding
     *
     * @param encodedString
     *
     * @return
     *
     * @throws IOException
     */
    public static byte[] urlBase64Decode(String encodedString) throws IOException {
        String decodedString = urlDecode(encodedString);
        return Base64.decode(decodedString);
    }

    /**
     * Apply deflate compression followed by base64 encoding and URL encoding
     *
     * @param stringToEncode
     *
     * @return
     *
     * @throws IOException
     */
    public static String deflateBase64URLEncode(String stringToEncode) throws IOException {
        return deflateBase64URLEncode(stringToEncode.getBytes(GeneralConstants.SAML_CHARSET));
    }

    /**
     * Apply deflate compression followed by base64 encoding and URL encoding
     *
     * @param stringToEncode
     *
     * @return
     *
     * @throws IOException
     */
    public static String deflateBase64URLEncode(byte[] stringToEncode) throws IOException {
        byte[] deflatedMsg = DeflateUtil.encode(stringToEncode);
        return base64URLEncode(deflatedMsg);
    }

    /**
     * Apply deflate compression followed by base64 encoding
     *
     * @param stringToEncode
     *
     * @return
     *
     * @throws IOException
     */
    public static String deflateBase64Encode(byte[] stringToEncode) throws IOException {
        byte[] deflatedMsg = DeflateUtil.encode(stringToEncode);
        return Base64.encodeBytes(deflatedMsg, Base64.DONT_BREAK_LINES);
    }

    /**
     * Apply URL decoding, followed by base64 decoding followed by deflate decompression
     *
     * @param encodedString
     *
     * @return
     *
     * @throws IOException
     */
    public static InputStream urlBase64DeflateDecode(String encodedString) throws IOException {
        byte[] deflatedString = urlBase64Decode(encodedString);
        return DeflateUtil.decode(deflatedString);
    }

    /**
     * Base64 decode followed by Deflate decoding
     *
     * @param encodedString
     *
     * @return
     */
    public static InputStream base64DeflateDecode(String encodedString) {
        byte[] base64decodedMsg = Base64.decode(encodedString);
        return DeflateUtil.decode(base64decodedMsg);
    }

    /**
     * Get the Query String for the destination url
     *
     * @param urlEncodedRequest
     * @param urlEncodedRelayState
     * @param sendRequest either going to be saml request or response
     *
     * @return
     */
    public static String getDestinationQueryString(String urlEncodedRequest, String urlEncodedRelayState, boolean sendRequest) {
        StringBuilder sb = new StringBuilder();
        if (sendRequest)
            sb.append("SAMLRequest=").append(urlEncodedRequest);
        else
            sb.append("SAMLResponse=").append(urlEncodedRequest);
        if (StringUtil.isNotNull(urlEncodedRelayState))
            sb.append("&RelayState=").append(urlEncodedRelayState);
        return sb.toString();
    }

    /**
     * Get the destination url
     *
     * @param holder
     *
     * @return
     *
     * @throws IOException
     */
    public static String getDestinationURL(RedirectBindingUtilDestHolder holder) throws IOException {
        String destination = holder.destination;
        StringBuilder destinationURL = new StringBuilder(destination);

        if (destination.contains("?"))
            destinationURL.append("&");
        else
            destinationURL.append("?");

        destinationURL.append(holder.destinationQueryString);

        return destinationURL.toString();
    }

    /**
     * A Destination holder that holds the destination host url and the destination query string
     */
    public static class RedirectBindingUtilDestHolder {

        private String destination;
        private String destinationQueryString;

        public RedirectBindingUtilDestHolder setDestinationQueryString(String dest) {
            destinationQueryString = dest;
            return this;
        }

        public RedirectBindingUtilDestHolder setDestination(String dest) {
            destination = dest;
            return this;
        }
    }
}
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
package org.keycloak.services.x509;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.StringTokenizer;

import org.keycloak.common.util.PemUtils;
import org.keycloak.http.HttpRequest;

public class EnvoyProxySslClientCertificateLookup implements X509ClientCertificateLookup {

    protected final static String XFCC_HEADER = "x-forwarded-client-cert";
    protected final static String XFCC_HEADER_CERT_KEY = "Cert";
    protected final static String XFCC_HEADER_CHAIN_KEY = "Chain";

    @Override
    public void close() {
    }


    /**
     * Extracts the client certificate chain from the HTTP request forwarded by Envoy.
     *
     * Envoy encodes the client certificate and the certificate chain in the  header in following format:
     *
     *   x-forwarded-client-cert: key1="url encoded value 1";key2="url encoded value 2";...
     *
     * Following keys are supported by this implementation:
     *
     * 1. Cert - The entire client certificate in URL encoded PEM format.
     * 2. Chain - The entire client certificate chain (including the leaf certificate) in URL encoded PEM format.
     *
     * For Envoy documentation, see
     * https://www.envoyproxy.io/docs/envoy/latest/configuration/http/http_conn_man/headers#x-forwarded-client-cert
     *
     * @param httpRequest The HTTP request forwarded by Envoy.
     * @return The client certificate chain extracted from the HTTP request.
     */
    @Override
    public X509Certificate[] getCertificateChain(HttpRequest httpRequest) throws GeneralSecurityException {
        String xfcc = httpRequest.getHttpHeaders().getRequestHeaders().getFirst(XFCC_HEADER);
        if (xfcc == null) {
            return null;
        }

        X509Certificate[] certs = null;

        StringTokenizer st = new StringTokenizer(xfcc, ";");
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            int index = token.indexOf("=");
            if (index != -1) {
                String key = token.substring(0, index).trim();
                String value = token.substring(index + 1).trim();

                if (key.equals(XFCC_HEADER_CHAIN_KEY)) {
                    // Chain contains the entire chain including the leaf certificate so we can stop processing the header.
                    certs = PemUtils.decodeCertificates(decodeValue(value));
                    break;
                } else if (key.equals(XFCC_HEADER_CERT_KEY)) {
                    // Cert contains only the leaf certificate. We need to continue processing the header in case Chain is present.
                    certs = PemUtils.decodeCertificates(decodeValue(value));
                }
           }
        }

        return certs;
    }

    private String decodeValue(String value) {
        // Remove enclosing quotes if present.
        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

}

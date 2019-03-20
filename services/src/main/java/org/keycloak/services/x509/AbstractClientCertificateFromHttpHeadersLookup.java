/*
 * Copyright 2017 Analytical Graphics, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.services.x509;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.common.util.PemException;

import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author <a href="mailto:brat000012001@gmail.com">Peter Nalyvayko</a>
 * @version $Revision: 1 $
 * @since 3/29/2017
 */

public abstract class AbstractClientCertificateFromHttpHeadersLookup implements X509ClientCertificateLookup {

    protected static final Logger logger = Logger.getLogger(AbstractClientCertificateFromHttpHeadersLookup.class);

    protected final String sslClientCertHttpHeader;
    protected final String sslCertChainHttpHeaderPrefix;
    protected final int certificateChainLength;

    public AbstractClientCertificateFromHttpHeadersLookup(String sslCientCertHttpHeader,
                                                          String sslCertChainHttpHeaderPrefix,
                                                          int certificateChainLength) {
        if (sslCientCertHttpHeader == null) {
            throw new IllegalArgumentException("sslClientCertHttpHeader");
        }

        if (certificateChainLength < 0) {
            throw new IllegalArgumentException("certificateChainLength must be greater or equal to zero");
        }

        this.sslClientCertHttpHeader = sslCientCertHttpHeader;
        this.sslCertChainHttpHeaderPrefix = sslCertChainHttpHeaderPrefix;
        this.certificateChainLength = certificateChainLength;
    }

    @Override
    public void close() {

    }

    static String getHeaderValue(HttpRequest httpRequest, String headerName) {
        return httpRequest.getHttpHeaders().getRequestHeaders().getFirst(headerName);
    }

    private static String trimDoubleQuotes(String quotedString) {

        if (quotedString == null) return null;

        int len = quotedString.length();
        if (len > 1 && quotedString.charAt(0) == '"' &&
                quotedString.charAt(len - 1) == '"') {
            logger.trace("Detected a certificate enclosed in double quotes");
            return quotedString.substring(1, len - 1);
        }
        return quotedString;
    }

    protected abstract X509Certificate decodeCertificateFromPem(String pem) throws PemException;

    protected X509Certificate getCertificateFromHttpHeader(HttpRequest request, String httpHeader) throws GeneralSecurityException {

        String encodedCertificate = getHeaderValue(request, httpHeader);

        // Remove double quotes
        encodedCertificate = trimDoubleQuotes(encodedCertificate);

        if (encodedCertificate == null ||
                encodedCertificate.trim().length() == 0) {
            logger.warnf("HTTP header \"%s\" is empty", httpHeader);
            return null;
        }

        try {
            X509Certificate cert = decodeCertificateFromPem(encodedCertificate);
            if (cert == null) {
                logger.warnf("HTTP header \"%s\" does not contain a valid x.509 certificate\n%s",
                        httpHeader, encodedCertificate);
            } else {
                logger.debugf("Found a valid x.509 certificate in \"%s\" HTTP header",
                        httpHeader);
            }
            return cert;
        }
        catch(PemException e) {
            logger.error(e.getMessage(), e);
            throw new GeneralSecurityException(e);
        }
    }


    @Override
    public X509Certificate[] getCertificateChain(HttpRequest httpRequest) throws GeneralSecurityException {
        List<X509Certificate> chain = new ArrayList<>();

        // Get the client certificate
        X509Certificate cert = getCertificateFromHttpHeader(httpRequest, sslClientCertHttpHeader);
        if (cert != null) {
            chain.add(cert);
            // Get the certificate of the client certificate chain
            for (int i = 0; i < certificateChainLength; i++) {
                try {
                    String s = String.format("%s_%s", sslCertChainHttpHeaderPrefix, i);
                    cert = getCertificateFromHttpHeader(httpRequest, s);
                    if (cert != null) {
                        chain.add(cert);
                    }
                }
                catch(GeneralSecurityException e) {
                    logger.warn(e.getMessage(), e);
                }
            }
        }
        return chain.toArray(new X509Certificate[0]);
    }
}

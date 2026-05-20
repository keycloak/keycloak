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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.DerUtils;
import org.keycloak.http.HttpRequest;

import org.jboss.logging.Logger;

/**
 * Extracts X.509 client certificates forwarded by an HAProxy reverse proxy.
 *
 * <p>Header values must be DER-encoded certificates, base64-encoded and wrapped in colons as
 * RFC 8941 byte sequences ({@code :base64:}). This matches the output of HAProxy's
 * {@code ssl_c_der,base64} and {@code ssl_c_chain_der,base64} sample fetches, for example:
 *
 * <pre>
 * http-request set-header Client-Cert :%[ssl_c_der,base64]:
 * http-request set-header Client-Cert-Chain :%[ssl_c_chain_der,base64]:
 * </pre>
 *
 * <p>The client certificate header contains a single certificate. The chain header contains the
 * concatenated DER encodings of all CA certificates, base64-encoded as one value. Only the first
 * {@code certificateChainLength} certificates from the chain are loaded.
 */
public record HaProxySslClientCertificateLookup(String sslClientCertHttpHeader,
                                                String sslCertChainHttpHeader,
                                                int certificateChainLength) implements X509ClientCertificateLookup {

    private static final Logger logger = Logger.getLogger(HaProxySslClientCertificateLookup.class);

    @Override
    public X509Certificate[] getCertificateChain(HttpRequest httpRequest) throws GeneralSecurityException {
        if (!httpRequest.isProxyTrusted()) {
            logger.warnf("HTTP header \"%s\" is not trusted", sslClientCertHttpHeader);
            return null;
        }

        X509Certificate clientCertificate = getClientCertificateFromHeader(httpRequest);
        List<X509Certificate> chain = new ArrayList<>(certificateChainLength + 1);
        if (clientCertificate != null) {
            chain.add(clientCertificate);
            chain.addAll(getClientCertificateChainFromHeader(httpRequest));
        }
        return chain.toArray(new X509Certificate[0]);
    }

    private X509Certificate getClientCertificateFromHeader(HttpRequest httpRequest) throws GeneralSecurityException {
        List<String> headerValues = httpRequest.getHttpHeaders().getRequestHeader(sslClientCertHttpHeader);
        if (headerValues == null || headerValues.isEmpty()) {
            return null;
        }
        String headerValue = headerValues.get(0);
        byte[] derBytes = decodeDerFromByteSequence(headerValue);
        try (InputStream is = new ByteArrayInputStream(derBytes)) {
            X509Certificate cert = DerUtils.decodeCertificate(is);
            if (cert != null) {
                logger.debugf("Parsed client certificate: Subject DN=[%s]  SerialNumber=[%s]",
                        cert.getSubjectX500Principal(), cert.getSerialNumber());
            }
            return cert;
        } catch (Exception e) {
            throw new GeneralSecurityException("Failed to parse client certificate from header " + sslClientCertHttpHeader, e);
        }
    }

    private List<X509Certificate> getClientCertificateChainFromHeader(HttpRequest httpRequest) throws GeneralSecurityException {
        if (sslCertChainHttpHeader == null || certificateChainLength == 0) {
            return List.of();
        }
        List<String> headerValues = httpRequest.getHttpHeaders().getRequestHeader(sslCertChainHttpHeader);
        if (headerValues == null || headerValues.isEmpty()) {
            return List.of();
        }
        String headerValue = headerValues.get(0);
        byte[] derBytes = decodeDerFromByteSequence(headerValue);
        try (InputStream is = new ByteArrayInputStream(derBytes)) {
            CertificateFactory cf = CryptoIntegration.getProvider().getX509CertFactory();
            Collection<? extends Certificate> certs = cf.generateCertificates(is);
            List<X509Certificate> chain = new ArrayList<>();
            for (Certificate cert : certs) {
                if (chain.size() >= certificateChainLength) {
                    break;
                }
                X509Certificate x509Cert = (X509Certificate) cert;
                logger.debugf("Parsed chain certificate: Subject DN=[%s]  SerialNumber=[%s]",
                        x509Cert.getSubjectX500Principal(), x509Cert.getSerialNumber());
                chain.add(x509Cert);
            }
            return chain;
        } catch (Exception e) {
            throw new GeneralSecurityException("Failed to parse certificate chain from header " + sslCertChainHttpHeader, e);
        }
    }

    private static byte[] decodeDerFromByteSequence(String byteSequence) throws GeneralSecurityException {
        if (byteSequence == null || byteSequence.length() < 3 || !byteSequence.startsWith(":") || !byteSequence.endsWith(":")) {
            throw new GeneralSecurityException("Invalid byte sequence format: expected value wrapped in colons");
        }
        String base64Content = byteSequence.substring(1, byteSequence.length() - 1);
        try {
            return Base64.getMimeDecoder().decode(base64Content);
        } catch (IllegalArgumentException e) {
            throw new GeneralSecurityException("Failed to decode base64 content from header", e);
        }
    }

    @Override
    public void close() {
    }
}

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
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.PemException;
import org.keycloak.common.util.PemUtils;
import org.keycloak.http.HttpRequest;

import org.jboss.logging.Logger;

/**
 * Extracts X.509 client certificates forwarded by an HAProxy reverse proxy.
 *
 * <p>HAProxy is not RFC 9440 compliant as it is currently only possible to Base64 encode the entire chain, whereas
 * RFC 9440 expects each certificate to be Base64 encoded individually, surrounded by `:` and provided as a CSV when multiple values exist.
 * If HAProxy adds support generating headers in this format, then it will be possible to deprecate and remove this
 * provider in favor of {@link Rfc9440ClientCertificateLookup} <a href="https://github.com/haproxy/haproxy/issues/2235">haproxy/#2235</a>.
 *
 * <p>Header values must be base64-encoded DER certificates, matching the output of HAProxy's
 * {@code ssl_c_der,base64} and {@code ssl_c_chain_der,base64} sample fetches.
 *
 * <p>Two modes are provided for reading the certificate chain:
 *
 * <ul>
 *   <li><b>Single header</b> (via {@code sslCertChain}): the entire chain is in one header as
 *       concatenated DER certificates, base64-encoded. Only the first {@code certificateChainLength}
 *       certificates are loaded. Example HAProxy config:
 *       <pre>
 * http-request set-header Client-Cert %[ssl_c_der,base64]
 * http-request set-header Client-Cert-Chain %[ssl_c_chain_der,base64]
 *       </pre>
 *   </li>
 *   <li><b>Indexed headers</b> (deprecated, based upon {@code sslCertChainPrefix}): each chain certificate is
 *       in a separate header named {@code {prefix}_{index}}, e.g. {@code Client-Cert-Chain_0},
 *       {@code Client-Cert-Chain_1}. This is problematic when more than one intermediate cert exists as HAProxy does not
 *       provide a built-in mechanism to define a header per intermediate cert in the chain, so this will only work as
 *       expected if a single intermediate certificate exists in the chain. Example HAProxy config:
 *       <pre>
 * http-request set-header Client-Cert %[ssl_c_der,base64]
 * http-request set-header Client-Cert-Chain_0 %[ssl_c_chain_der,base64]
 *       </pre>
 *   </li>
 * </ul>
 *
 * @author <a href="mailto:brat000012001@gmail.com">Peter Nalyvayko</a>
 * @version $Revision: 1 $
 * @since 3/27/2017
 */
public class HaProxySslClientCertificateLookup extends AbstractClientCertificateFromHttpHeadersLookup {

    private static final Logger logger = Logger.getLogger(HaProxySslClientCertificateLookup.class);

    private final String sslCertChainHttpHeader;

    public HaProxySslClientCertificateLookup(String sslClientCertHttpHeader,
                                             String sslCertChainHttpHeaderPrefix,
                                             String sslCertChainHttpHeader,
                                             int certificateChainLength) {
        super(sslClientCertHttpHeader, sslCertChainHttpHeaderPrefix, certificateChainLength);
        this.sslCertChainHttpHeader = sslCertChainHttpHeader;
    }

    @Override
    protected X509Certificate decodeCertificateFromPem(String pem) throws PemException {
        if (pem == null) {
            return null;
        }
        return PemUtils.decodeCertificate(pem);
    }

    @Override
    protected void buildChain(HttpRequest httpRequest, List<X509Certificate> chain, X509Certificate cert) {
        chain.add(cert);
        if (sslCertChainHttpHeader != null) {
            try {
                addCertificateChainFromSingleHeader(httpRequest, chain);
            } catch (GeneralSecurityException e) {
                logger.warn(e.getMessage(), e);
            }
        } else {
            addCertificateChainFromIndexedHeaders(httpRequest, chain);
        }
    }

    private void addCertificateChainFromSingleHeader(HttpRequest httpRequest, List<X509Certificate> chain) throws GeneralSecurityException {
        if (certificateChainLength == 0) {
            return;
        }

        String headerValue = getHeaderValue(httpRequest, sslCertChainHttpHeader);
        if (headerValue == null || headerValue.isEmpty()) {
            return;
        }

        byte[] derBytes;
        try {
            derBytes = Base64.getMimeDecoder().decode(headerValue);
        } catch (IllegalArgumentException e) {
            throw new GeneralSecurityException("Failed to decode base64 content from header " + sslCertChainHttpHeader, e);
        }

        try (InputStream is = new ByteArrayInputStream(derBytes)) {
            CryptoIntegration.getProvider().getX509CertFactory()
                  .generateCertificates(is)
                  .stream()
                  .limit(certificateChainLength)
                  .map(X509Certificate.class::cast)
                  .peek(cert -> logger.debugf("Parsed chain certificate: Subject DN=[%s]", cert.getSubjectX500Principal()))
                  .forEach(chain::add);
        } catch (IOException e) {
            throw new GeneralSecurityException("Failed to parse certificate chain from header " + sslCertChainHttpHeader, e);
        }
    }
}

/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
import java.util.stream.Stream;

import org.keycloak.common.util.PemException;
import org.keycloak.common.util.PemUtils;
import org.keycloak.http.HttpRequest;

import org.jboss.logging.Logger;

/**
 * The provider allows to extract X.509 client certificates forwarded
 * to the Keycloak middleware configured behind a Traefik reverse proxy
 * using the PassTLSClientCert middleware with {@code pem: true}.
 *
 * <p>Traefik's PassTLSClientCert middleware (with {@code pem: true}) forwards the client
 * certificate and any intermediate CA certificates as URL-encoded PEM blocks
 * in the {@code X-Forwarded-Tls-Client-Cert} HTTP header, separated by commas.
 *
 * <p>Example Traefik configuration:
 * <pre>
 * [http.middlewares.my-tls-client-cert.passTLSClientCert]
 *   [http.middlewares.my-tls-client-cert.passTLSClientCert.pem]
 *     pem = true
 * </pre>
 *
 * @see <a href="https://doc.traefik.io/traefik/middlewares/http/passtlsclientcert/">Traefik PassTLSClientCert middleware</a>
 */
public class TraefikProxySslClientCertificateLookup implements X509ClientCertificateLookup {

    private static final Logger log = Logger.getLogger(TraefikProxySslClientCertificateLookup.class);

    protected final String sslClientCertHttpHeader;
    private final boolean certIsUrlEncoded;
    protected int certificateChainLength;

    public TraefikProxySslClientCertificateLookup(String sslClientCertHttpHeader, boolean certIsUrlEncoded, int certificateChainLength) {
        this.sslClientCertHttpHeader = sslClientCertHttpHeader;
        this.certIsUrlEncoded = certIsUrlEncoded;
        this.certificateChainLength = certificateChainLength;
    }

    @Override
    public X509Certificate[] getCertificateChain(HttpRequest httpRequest) throws GeneralSecurityException {
        if (!httpRequest.isProxyTrusted()) {
            log.warnf("HTTP header \"%s\" is not trusted", sslClientCertHttpHeader);
            return null;
        }

        String headerValue = httpRequest.getHttpHeaders().getRequestHeaders().getFirst(sslClientCertHttpHeader);

        if (headerValue == null || headerValue.isBlank()) {
            log.warnf("HTTP header \"%s\" is empty", sslClientCertHttpHeader);
            return new X509Certificate[0];
        }

        // Traefik may URL-encode - see https://github.com/traefik/traefik/issues/9669
        if (certIsUrlEncoded) {
            headerValue = URLDecoder.decode(headerValue, StandardCharsets.UTF_8);
        }

        try {
            X509Certificate[] certs = Stream.of(headerValue.split(",")).map(PemUtils::decodeCertificate)
                    .limit(certificateChainLength + 1).toArray(X509Certificate[]::new);
            if (certs.length == 0) {
                log.warnf("HTTP header \"%s\" does not contain any valid X.509 certificates", sslClientCertHttpHeader);
            } else {
                log.debugf("Found %d X.509 certificate(s) in \"%s\" HTTP header", certs.length, sslClientCertHttpHeader);
            }
            return certs;
        } catch (PemException e) {
            throw new GeneralSecurityException(e);
        }
    }

    @Override
    public void close() {
        // intentionally left blank
    }
}

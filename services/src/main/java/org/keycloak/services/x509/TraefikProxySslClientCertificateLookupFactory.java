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

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import org.jboss.logging.Logger;

/**
 * Factory for the Traefik proxy SSL client certificate lookup provider.
 *
 * <p>This factory creates {@link TraefikProxySslClientCertificateLookup} instances
 * that extract X.509 client certificates forwarded by Traefik's PassTLSClientCert
 * middleware.
 *
 * <p>Configuration options:
 * <ul>
 *   <li>{@code sslClientCert} - the HTTP header name containing the client certificate
 *       (default: {@code X-Forwarded-Tls-Client-Cert})</li>
 * </ul>
 *
 * @see TraefikProxySslClientCertificateLookup
 */
public class TraefikProxySslClientCertificateLookupFactory implements X509ClientCertificateLookupFactory {

    private static final Logger logger = Logger.getLogger(TraefikProxySslClientCertificateLookupFactory.class);

    private static final String PROVIDER = "traefik";

    protected static final String HTTP_HEADER_CLIENT_CERT = "sslClientCert";
    public static final String HTTP_HEADER_CLIENT_CERT_DEFAULT = "X-Forwarded-Tls-Client-Cert";

    protected static final String CERT_IS_URL_ENCODED = "cert-is-url-encoded";
    protected final static String HTTP_HEADER_CERT_CHAIN_LENGTH = "certificateChainLength";
    protected final static int HTTP_HEADER_CERT_CHAIN_LENGTH_DEFAULT = 1;

    protected String sslClientCertHttpHeader;
    private boolean certIsUrlEncoded;
    protected int certificateChainLength;

    @Override
    public void init(Config.Scope config) {
        certificateChainLength = config.getInt(HTTP_HEADER_CERT_CHAIN_LENGTH, HTTP_HEADER_CERT_CHAIN_LENGTH_DEFAULT);
        if (certificateChainLength < 0) {
            throw new IllegalArgumentException("certificateChainLength must be greater or equal to zero");
        }
        sslClientCertHttpHeader = config.get(HTTP_HEADER_CLIENT_CERT, HTTP_HEADER_CLIENT_CERT_DEFAULT);
        if (sslClientCertHttpHeader == null || sslClientCertHttpHeader.isBlank()) {
            throw new IllegalArgumentException("sslClientCertHttpHeader must not be blank");
        }
        certIsUrlEncoded = config.getBoolean(CERT_IS_URL_ENCODED, false);
        logger.tracev("{0}:   ''{1}''", HTTP_HEADER_CLIENT_CERT, sslClientCertHttpHeader);
        logger.tracev("{0}:   ''{1}''", CERT_IS_URL_ENCODED, certIsUrlEncoded);
    }

    @Override
    public X509ClientCertificateLookup create(KeycloakSession session) {
        return new TraefikProxySslClientCertificateLookup(sslClientCertHttpHeader, certIsUrlEncoded, certificateChainLength);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // intentionally left blank
    }

    @Override
    public void close() {
        // intentionally left blank
    }

    @Override
    public String getId() {
        return PROVIDER;
    }
}

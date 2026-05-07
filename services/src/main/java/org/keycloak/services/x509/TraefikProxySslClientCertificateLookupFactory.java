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

/**
 * Factory for the Traefik proxy SSL client certificate lookup provider.
 *
 * <p>This factory creates {@link TraefikProxySslClientCertificateLookup} instances
 * that extract X.509 client certificates forwarded by Traefik's PassTLSClientCert
 * middleware.
 *
 * @see TraefikProxySslClientCertificateLookup
 */
public class TraefikProxySslClientCertificateLookupFactory implements X509ClientCertificateLookupFactory {

    private static final String PROVIDER = "traefik";

    public static final String HTTP_HEADER_CLIENT_CERT = "X-Forwarded-Tls-Client-Cert";

    protected final static String HTTP_HEADER_CERT_CHAIN_LENGTH = "certificateChainLength";
    protected final static int HTTP_HEADER_CERT_CHAIN_LENGTH_DEFAULT = 1;

    protected int certificateChainLength;

    @Override
    public void init(Config.Scope config) {
        certificateChainLength = config.getInt(HTTP_HEADER_CERT_CHAIN_LENGTH, HTTP_HEADER_CERT_CHAIN_LENGTH_DEFAULT);
        if (certificateChainLength < 0) {
            throw new IllegalArgumentException("certificateChainLength must be greater or equal to zero");
        }
    }

    @Override
    public X509ClientCertificateLookup create(KeycloakSession session) {
        return new TraefikProxySslClientCertificateLookup(certificateChainLength);
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

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

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:brat000012001@gmail.com">Peter Nalyvayko</a>
 * @version $Revision: 1 $
 * @since 4/4/2017
 */

public class HaProxySslClientCertificateLookupFactory implements X509ClientCertificateLookupFactory {

    private final static Logger logger = Logger.getLogger(HaProxySslClientCertificateLookupFactory.class);
    private final static String PROVIDER = "haproxy";

    private final static String HTTP_HEADER_CLIENT_CERT = "sslClientCert";
    private final static String HTTP_HEADER_CLIENT_CERT_DEFAULT = "Client-Cert";
    private final static String HTTP_HEADER_CERT_CHAIN = "sslCertChain";
    private final static String HTTP_HEADER_CERT_CHAIN_DEFAULT = "Client-Cert-Chain";
    private final static String HTTP_HEADER_CERT_CHAIN_LENGTH = "certificateChainLength";
    private final static int HTTP_HEADER_CERT_CHAIN_LENGTH_DEFAULT = 1;

    private X509ClientCertificateLookup certLookup;

    @Override
    public void init(Config.Scope config) {
        int certificateChainLength = config.getInt(HTTP_HEADER_CERT_CHAIN_LENGTH, HTTP_HEADER_CERT_CHAIN_LENGTH_DEFAULT);
        String sslClientCertHttpHeader = config.get(HTTP_HEADER_CLIENT_CERT, HTTP_HEADER_CLIENT_CERT_DEFAULT);
        String sslChainHttpHeader = config.get(HTTP_HEADER_CERT_CHAIN, HTTP_HEADER_CERT_CHAIN_DEFAULT);

        logger.tracev("{0}:   ''{1}''", HTTP_HEADER_CLIENT_CERT, sslClientCertHttpHeader);
        logger.tracev("{0}:   ''{1}''", HTTP_HEADER_CERT_CHAIN, sslChainHttpHeader);
        logger.tracev("{0}:   ''{1}''", HTTP_HEADER_CERT_CHAIN_LENGTH, certificateChainLength);

        if (sslClientCertHttpHeader == null || sslClientCertHttpHeader.isEmpty()) {
            throw new IllegalArgumentException("sslClientCertHttpHeader cannot be null or empty");
        }

        if (certificateChainLength < 0) {
            throw new IllegalArgumentException("certificateChainLength must be greater or equal to zero");
        }
        certLookup = new HaProxySslClientCertificateLookup(sslClientCertHttpHeader, sslChainHttpHeader, certificateChainLength);
    }


    @Override
    public X509ClientCertificateLookup create(KeycloakSession session) {
        return certLookup;
    }

    @Override
    public String getId() {
        return PROVIDER;
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }
}

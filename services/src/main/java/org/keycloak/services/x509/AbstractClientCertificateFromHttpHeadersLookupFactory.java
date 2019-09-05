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
import org.keycloak.Config;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * @author <a href="mailto:brat000012001@gmail.com">Peter Nalyvayko</a>
 * @version $Revision: 1 $
 * @since 4/4/2017
 */

public abstract class AbstractClientCertificateFromHttpHeadersLookupFactory implements X509ClientCertificateLookupFactory {

    private final static Logger logger = Logger.getLogger(AbstractClientCertificateFromHttpHeadersLookupFactory.class);

    protected final static String CERTIFICATE_CHAIN_LENGTH = "certificateChainLength";
    protected final static String HTTP_HEADER_CLIENT_CERT = "sslClientCert";
    protected final static String HTTP_HEADER_CERT_CHAIN_PREFIX = "sslCertChainPrefix";

    protected String sslClientCertHttpHeader;
    protected String sslChainHttpHeaderPrefix;
    protected int certificateChainLength = 1;

    @Override
    public void init(Config.Scope config) {
        if (config != null) {
            certificateChainLength = config.getInt(CERTIFICATE_CHAIN_LENGTH, 1);
            logger.tracev("{0}: ''{1}''", CERTIFICATE_CHAIN_LENGTH, certificateChainLength);

            sslClientCertHttpHeader = config.get(HTTP_HEADER_CLIENT_CERT, "");
            logger.tracev("{0}:   ''{1}''", HTTP_HEADER_CLIENT_CERT, sslClientCertHttpHeader);

            sslChainHttpHeaderPrefix = config.get(HTTP_HEADER_CERT_CHAIN_PREFIX, null);
            if (sslChainHttpHeaderPrefix != null) {
                logger.tracev("{0}:  ''{1}''", HTTP_HEADER_CERT_CHAIN_PREFIX, sslChainHttpHeaderPrefix);
            } else {
                logger.tracev("{0} was not configured", HTTP_HEADER_CERT_CHAIN_PREFIX);
            }
        }
        else {
            logger.tracev("No configuration for ''{0}'' reverse proxy was found", this.getId());
            sslClientCertHttpHeader = "";
            sslChainHttpHeaderPrefix = "";
            certificateChainLength = 0;
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {

    }

}

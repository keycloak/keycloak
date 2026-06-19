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

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:brat000012001@gmail.com">Peter Nalyvayko</a>
 * @version $Revision: 1 $
 * @since 4/4/2017
 */
public class HaProxySslClientCertificateLookupFactory extends AbstractClientCertificateFromHttpHeadersLookupFactory {

    private static final Logger logger = Logger.getLogger(HaProxySslClientCertificateLookupFactory.class);
    private static final String PROVIDER = "haproxy";
    private static final String HTTP_HEADER_CERT_CHAIN = "sslCertChain";

    private X509ClientCertificateLookup certLookup;

    @Override
    public void init(Config.Scope config) {
        super.init(config);

        if (sslChainHttpHeaderPrefix != null) {
            logger.warnf("The '%s' option is deprecated and will be removed in a future release. Configure '%s' instead.",
                    HTTP_HEADER_CERT_CHAIN_PREFIX, HTTP_HEADER_CERT_CHAIN);
        }

        String sslCertChainHttpHeader = config.get(HTTP_HEADER_CERT_CHAIN, null);
        if (sslCertChainHttpHeader != null) {
            logger.tracev("{0}:  ''{1}''", HTTP_HEADER_CERT_CHAIN, sslCertChainHttpHeader);
        }

        certLookup = new HaProxySslClientCertificateLookup(sslClientCertHttpHeader,
                sslChainHttpHeaderPrefix, sslCertChainHttpHeader, certificateChainLength);
    }

    @Override
    public X509ClientCertificateLookup create(KeycloakSession session) {
        return certLookup;
    }

    @Override
    public String getId() {
        return PROVIDER;
    }
}

package org.keycloak.services.x509;
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

import org.keycloak.models.KeycloakSession;

/**
 * @author <a href="mailto:arnault.michel@toad-consulting.com">Arnault MICHEL</a>
 * @version $Revision: 1 $
 * @since 10/09/2018
 */

public class NginxProxySslClientCertificateLookupFactory extends AbstractClientCertificateFromHttpHeadersLookupFactory {

    private final static String PROVIDER = "nginx";

    @Override
    public X509ClientCertificateLookup create(KeycloakSession session) {
        return new NginxProxySslClientCertificateLookup(sslClientCertHttpHeader,
                sslChainHttpHeaderPrefix, certificateChainLength, session);
    }

    @Override
    public String getId() {
        return PROVIDER;
    }
}

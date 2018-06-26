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

import org.keycloak.common.util.PemException;
import org.keycloak.common.util.PemUtils;

import java.security.cert.X509Certificate;

/**
 * The provider allows to extract X.509 client certificate forwarded
 * to keycloak configured behind the Apache reverse proxy.
 *
 * @author <a href="mailto:brat000012001@gmail.com">Peter Nalyvayko</a>
 * @version $Revision: 1 $
 * @since 3/29/2017
 */

public class ApacheProxySslClientCertificateLookup extends AbstractClientCertificateFromHttpHeadersLookup {

    public ApacheProxySslClientCertificateLookup(String sslCientCertHttpHeader,
                                                 String sslCertChainHttpHeaderPrefix,
                                                 int certificateChainLength) {
        super(sslCientCertHttpHeader, sslCertChainHttpHeaderPrefix, certificateChainLength);
    }

    private static String removeBeginEnd(String pem) {
        pem = pem.replace("-----BEGIN CERTIFICATE-----", "");
        pem = pem.replace("-----END CERTIFICATE-----", "");
        pem = pem.replace("\r\n", "");
        pem = pem.replace("\n", "");
        return pem.trim();
    }

    @Override
    protected X509Certificate decodeCertificateFromPem(String pem) throws PemException {
        if (pem == null) {
            return null;
        }
        if (pem.startsWith("-----BEGIN CERTIFICATE-----")) {
            pem = removeBeginEnd(pem);
        }
        return PemUtils.decodeCertificate(pem);
    }
}

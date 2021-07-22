/*
 * Copyright 2020 Skeeter Health Sagl and/or its affiliates
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
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.common.util.PemUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The Envoy provider extracts the Certificate Chain from the x-forwarded-client-cert header,
 * as explained here:
 * https://www.envoyproxy.io/docs/envoy/v1.15.0/configuration/http/http_conn_man/headers.html?highlight=Chain#x-forwarded-client-cert
 *
 * @author <a href="mailto:giorgio.azzinnaro@skeeterhealth.com">Giorgio Azzinnaro</a>
 */
class EnvoyProxySslClientCertificateLookup implements X509ClientCertificateLookup {

    private static final Logger log = Logger.getLogger(EnvoyProxySslClientCertificateLookup.class);

    public EnvoyProxySslClientCertificateLookup() {
    }

    @Override
    public X509Certificate[] getCertificateChain(HttpRequest httpRequest) throws GeneralSecurityException {
        String header = httpRequest.getHttpHeaders().getRequestHeaders().getFirst("x-forwarded-client-cert");

        if (header == null) {
            log.warn("Header x-forwarded-client-cert is empty");
            return null;
        }

        // Split the x-forwarded-client-cert header and create a map with its values
        Map<String, String> xfcc = Arrays.stream(header.split(";"))
                .map(s -> s.split("="))
                .filter(strings -> strings.length == 2)
                .collect(Collectors.toMap(strings -> strings[0], strings -> strings[1]));

        if (!xfcc.containsKey("Chain")) {
            log.warn("Chain is not present in x-forwarded-client-cert header");
            return null;
        }

        String urlEncodedChain = xfcc.get("Chain");
        String decodedChain;

        // The certificate chain is URL encoded, decode it here
        try {
            decodedChain = URLDecoder.decode(urlEncodedChain, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.errorf(e, "Cannot URL decode the certificate chain: %s", urlEncodedChain);
            return null;
        }

        // Get rid of newlines and quotes here not to break split later
        decodedChain = decodedChain.replaceAll("[\"\r\n]", "");

        log.tracef("URL decoded certificate chain: %s", decodedChain);

        // Finally, split the certificates in the chain, and decode each of them
        return Arrays.stream(decodedChain.split("(?<=-----END CERTIFICATE-----)"))
                .map(PemUtils::decodeCertificate)
                .toArray(X509Certificate[]::new);
    }

    @Override
    public void close() {

    }
}

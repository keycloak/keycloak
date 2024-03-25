/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.services.util;

import com.fasterxml.jackson.core.type.TypeReference;
import org.jboss.logging.Logger;
import org.keycloak.models.CrossDomainTrust;
import org.keycloak.models.RealmModel;
import org.keycloak.util.JsonSerialization;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;

/**
 * @author Ben Cresitello-Dittmar
 *
 * This utility class is for parsing the realm cross-domain trust configuration.
 */
public class CrossDomainTrustUtil {
    private static final Logger logger = Logger.getLogger(CrossDomainTrustUtil.class);

    /**
     * Get the cross-domain trust configuration with the specified issuer. Returns null if no configuration is found with the
     * specified issuer.
     * @param realm the realm
     * @param issuer the issuer to search for
     * @return the cross-domain trust configuration or null if the issuer is not found
     */
    public static CrossDomainTrust getCrossDomainTrust(RealmModel realm, String issuer){
        List<CrossDomainTrust> trustedDomains = getCrossDomainTrusts(realm);
        if (trustedDomains == null) return null;

        return trustedDomains.stream().filter(c -> c.getIssuer().equals(issuer)).findFirst().orElse(null);
    }

    /**
     * Get the cross-domain trust configuration for the realm. Returns an empty list if no configuration is found or null
     * if the cross-domain trust configuration cannot be parsed.
     * @param realm the realm
     * @return the cross-domain trusts configured on the realm
     */
    public static List<CrossDomainTrust> getCrossDomainTrusts(RealmModel realm){
        String configsJson = realm.getAttribute(CrossDomainTrust.REALM_CROSS_DOMAIN_TRUST_ATTRIBUTE);
        if (configsJson == null){
            configsJson = "[]";
        }

        // deserialize the config
        try {
            List<CrossDomainTrust> trustedDomains = JsonSerialization.readValue(configsJson, new TypeReference<List<CrossDomainTrust>>() {});
            enrichCrossDomainTrustConfigs(trustedDomains);
            return trustedDomains;
        } catch (IOException ex){
            logger.warnf("Failed to parse cross domain trust for realm '%s'", realm.getName());
            return null;
        }
    }

    /**
     * Helper function to enrich the cross domain trust configurations by parsing the provided certificate and extracting the
     * public key.
     * @param trustedDomains the configurations to enrich
     */
    private static void enrichCrossDomainTrustConfigs(List<CrossDomainTrust> trustedDomains){
        trustedDomains.forEach(config -> {
            X509Certificate cert = parseCertificate(config.getCertificate());
            config.setDecodedCertificate(cert);
            if (cert != null) {
                config.setPublicKey(cert.getPublicKey());
            } else {
                logger.warnf("failed to parse certificate in assertion grant configuration with issuer %s", config.getIssuer());
            }
        });
    }


    /**
     * Parse the base64 encoded certificate from the provided configuration into an X509Certificate object. Returns null
     * if the certificate fails to parse
     * @param encodedCert the base64 encoded certificate
     * @return The X509Certificate object or null if the certificate cannot be parsed
     */
    private static X509Certificate parseCertificate(String encodedCert){
        // attempt to decode the certificate and parse the certificate
        try {
            byte[] certByteArr = Base64.getDecoder().decode(encodedCert.replace("\n", "").replace("\r", "").getBytes());
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(certByteArr));
        } catch (CertificateException | IllegalArgumentException e){
            return null;
        }
    }
}

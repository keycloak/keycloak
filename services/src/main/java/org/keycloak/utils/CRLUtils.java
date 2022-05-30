/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.jboss.logging.Logger;
import org.keycloak.common.util.BouncyIntegration;
import org.keycloak.models.KeycloakSession;
import org.keycloak.truststore.TruststoreProvider;

/**
 * @author <a href="mailto:brat000012001@gmail.com">Peter Nalyvayko</a>
 * @version $Revision: 1 $
 * @since 10/31/2016
 */

public final class CRLUtils {

    private static final Logger log = Logger.getLogger(CRLUtils.class);

    private static final String CRL_DISTRIBUTION_POINTS_OID = "2.5.29.31";

    /**
     * Retrieves a list of CRL distribution points from CRLDP v3 certificate extension
     * See <a href="www.nakov.com/blog/2009/12/01/x509-certificate-validation-in-java-build-and-verify-cchain-and-verify-clr-with-bouncy-castle/">CRL validation</a>
     * @param cert
     * @return
     * @throws IOException
     */
    public static List<String> getCRLDistributionPoints(X509Certificate cert) throws IOException {
        byte[] data = cert.getExtensionValue(CRL_DISTRIBUTION_POINTS_OID);
        if (data == null) {
            return Collections.emptyList();
        }

        List<String> distributionPointUrls = new LinkedList<>();
        DEROctetString octetString;
        try (ASN1InputStream crldpExtensionInputStream = new ASN1InputStream(new ByteArrayInputStream(data))) {
            octetString = (DEROctetString)crldpExtensionInputStream.readObject();
        }
        byte[] octets = octetString.getOctets();

        CRLDistPoint crlDP;
        try (ASN1InputStream crldpInputStream = new ASN1InputStream(new ByteArrayInputStream(octets))) {
            crlDP = CRLDistPoint.getInstance(crldpInputStream.readObject());
        }

        for (DistributionPoint dp : crlDP.getDistributionPoints()) {
            DistributionPointName dpn = dp.getDistributionPoint();
            if (dpn != null && dpn.getType() == DistributionPointName.FULL_NAME) {
                GeneralName[] names = GeneralNames.getInstance(dpn.getName()).getNames();
                for (GeneralName gn : names) {
                    if (gn.getTagNo() == GeneralName.uniformResourceIdentifier) {
                        String url = DERIA5String.getInstance(gn.getName()).getString();
                        distributionPointUrls.add(url);
                    }
                }
            }
        }

        return distributionPointUrls;
    }


    /**
     * Check the signature on CRL and check if 1st certificate from the chain ((The actual certificate from the client)) is valid and not available on CRL.
     *
     * @param certs The 1st certificate is the actual certificate of the user. The other certificates represents the certificate chain
     * @param crl Given CRL
     * @throws GeneralSecurityException if some error in validation happens. Typically certificate not valid, or CRL signature not valid
     */
    public static void check(X509Certificate[] certs, X509CRL crl, KeycloakSession session) throws GeneralSecurityException {
        if (certs.length < 2) {
            throw new GeneralSecurityException("Not possible to verify signature on CRL. X509 certificate doesn't have CA chain available on it");
        }

        X500Principal crlIssuerPrincipal = crl.getIssuerX500Principal();
        X509Certificate crlSignatureCertificate = null;

        // Try to find the certificate in the CA chain, which was used to sign the CRL
        for (int i=1 ; i<certs.length ; i++) {
            X509Certificate currentCACert = certs[i];
            if (crlIssuerPrincipal.equals(currentCACert.getSubjectX500Principal())) {
                crlSignatureCertificate = currentCACert;

                log.tracef("Found certificate used to sign CRL in the CA chain of the certificate. CRL issuer: %s", crlIssuerPrincipal);
                break;
            }
        }

        // Try to find the CRL issuer certificate in the truststore
        if (crlSignatureCertificate == null) {
            log.tracef("Not found CRL issuer '%s' in the CA chain of the certificate. Fallback to lookup CRL issuer in the truststore", crlIssuerPrincipal);
            crlSignatureCertificate = findCRLSignatureCertificateInTruststore(session, certs, crlIssuerPrincipal);
        }

        // Verify signature on CRL
        // TODO: It will be nice to cache CRLs and also verify their signatures just once at the time when CRL is loaded, rather than in every request
        crl.verify(crlSignatureCertificate.getPublicKey());

        // Finally check if
        if (crl.isRevoked(certs[0])) {
            String message = String.format("Certificate has been revoked, certificate's subject: %s", certs[0].getSubjectDN().getName());
            log.debug(message);
            throw new GeneralSecurityException(message);
        }
    }


    private static X509Certificate findCRLSignatureCertificateInTruststore(KeycloakSession session, X509Certificate[] certs, X500Principal crlIssuerPrincipal) throws GeneralSecurityException {
        TruststoreProvider truststoreProvider = session.getProvider(TruststoreProvider.class);
        if (truststoreProvider == null || truststoreProvider.getTruststore() == null) {
            throw new GeneralSecurityException("Truststore not available");
        }

        Map<X500Principal, X509Certificate> rootCerts = truststoreProvider.getRootCertificates();
        Map<X500Principal, X509Certificate> intermediateCerts = truststoreProvider.getIntermediateCertificates();

        X509Certificate crlSignatureCertificate = intermediateCerts.get(crlIssuerPrincipal);
        if (crlSignatureCertificate == null) {
            crlSignatureCertificate = rootCerts.get(crlIssuerPrincipal);
        }

        if (crlSignatureCertificate == null) {
            throw new GeneralSecurityException("Not available certificate for CRL issuer '" + crlIssuerPrincipal + "' in the truststore, nor in the CA chain");
        } else {
            log.tracef("Found CRL issuer certificate with subject '%s' in the truststore. Verifying trust anchor", crlIssuerPrincipal);
        }

        // Check if CRL issuer has trust anchor with the checked certificate (See https://tools.ietf.org/html/rfc5280#section-6.3.3 , paragraph (f))
        Set<X500Principal> certificateCAPrincipals = Arrays.asList(certs).stream()
                .map(X509Certificate::getSubjectX500Principal)
                .collect(Collectors.toSet());

        // Remove the checked certificate itself
        certificateCAPrincipals.remove(certs[0].getSubjectX500Principal());

        X509Certificate currentCRLAnchorCertificate = crlSignatureCertificate;
        X500Principal currentCRLAnchorPrincipal = crlIssuerPrincipal;
        while (true) {
            if (certificateCAPrincipals.contains(currentCRLAnchorPrincipal)) {
                log.tracef("Found trust anchor of the CRL issuer '%s' in the CA chain. Anchor is '%s'", crlIssuerPrincipal, currentCRLAnchorPrincipal);
                break;
            }

            // Try to see the anchor
            currentCRLAnchorPrincipal = currentCRLAnchorCertificate.getIssuerX500Principal();

            currentCRLAnchorCertificate = intermediateCerts.get(currentCRLAnchorPrincipal);
            if (currentCRLAnchorCertificate == null) {
                currentCRLAnchorCertificate = rootCerts.get(currentCRLAnchorPrincipal);
            }
            if (currentCRLAnchorCertificate == null) {
                throw new GeneralSecurityException("Certificate for CRL issuer '" + crlIssuerPrincipal + "' available in the truststore, but doesn't have trust anchors with the CA chain.");
            }
        }

        return crlSignatureCertificate;
    }

}

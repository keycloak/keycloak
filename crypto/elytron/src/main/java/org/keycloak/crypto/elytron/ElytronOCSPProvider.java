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
package org.keycloak.crypto.elytron;

import java.io.IOException;
import java.net.URI;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CRLReason;
import java.security.cert.CertPath;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.PKIXParameters;
import java.security.cert.PKIXRevocationChecker;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import org.keycloak.models.KeycloakSession;
import org.keycloak.utils.OCSPProvider;

import org.jboss.logging.Logger;
import org.wildfly.security.asn1.ASN1;
import org.wildfly.security.asn1.DERDecoder;
import org.wildfly.security.x500.X500;


/**
 * @author <a href="mailto:david.anderson@redhat.com">David Anderson</a>
 */
public class ElytronOCSPProvider extends OCSPProvider {

    private final static Logger logger = Logger.getLogger(ElytronOCSPProvider.class.getName());

    /**
     * Requests certificate revocation status using OCSP.
     * 
     * @param cert the certificate to be checked
     * @param issuerCertificate the issuer certificate
     * @param responderURIs the OCSP responder URIs
     * @param responderCert the OCSP responder certificate
     * @param date if null, the current time is used.
     * @return a revocation status
     * @throws CertPathValidatorException
     */
    @Override
    protected OCSPRevocationStatus check(KeycloakSession session, X509Certificate cert,
            X509Certificate issuerCertificate, List<URI> responderURIs, X509Certificate responderCert, Date date)
            throws CertPathValidatorException {
        if (responderURIs == null || responderURIs.size() == 0)
            throw new IllegalArgumentException("Need at least one responder");

        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, "pass".toCharArray());
            trustStore.setCertificateEntry("trust", issuerCertificate);

            CertPathBuilder cpb = CertPathBuilder.getInstance("PKIX");
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            CertPathValidator cpv = CertPathValidator.getInstance("PKIX");
            PKIXRevocationChecker rc = (PKIXRevocationChecker) cpb.getRevocationChecker();
            X509CertSelector certSelector = new X509CertSelector();

            X509Certificate[] certs = { cert };
            certSelector.setCertificate(cert);
            certSelector.setCertificateValid(date);

            CertPath cp = cf.generateCertPath(Arrays.asList(certs));

            PKIXParameters params = new PKIXBuilderParameters(trustStore, certSelector);

            rc.setOcspResponder(responderURIs.get(0));
            rc.setOcspResponderCert(responderCert);
            rc.setOptions(EnumSet.noneOf(PKIXRevocationChecker.Option.class));
            params.setRevocationEnabled(false);
            params.addCertPathChecker(rc);

            PKIXCertPathValidatorResult result = (PKIXCertPathValidatorResult) cpv.validate(cp, params);
            logger.debug("Certificate validated by CA: " + result.getTrustAnchor().getCAName());

        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException | CertificateException | IOException
                | KeyStoreException e) {
            logger.warn("OSCP Response check failed.", e);
            return unknownStatus();
        }

        return new OCSPRevocationStatus() {

            @Override
            public RevocationStatus getRevocationStatus() {
                return RevocationStatus.GOOD;
            }

            @Override
            public Date getRevocationTime() {
                return null;
            }

            @Override
            public CRLReason getRevocationReason() {
                return null;
            }

        };
    }

    /**
     * Extracts OCSP responder URI from X509 AIA v3 extension, if available. There can be
     * multiple responder URIs encoded in the certificate.
     * 
     * @param cert
     * @return a list of available responder URIs.
     * @throws CertificateEncodingException
     */
    @Override
    protected List<String> getResponderURIs(X509Certificate cert) throws CertificateEncodingException {

        LinkedList<String> responderURIs = new LinkedList<>();

        byte[] authinfob = cert.getExtensionValue(X500.OID_PE_AUTHORITY_INFO_ACCESS);
        DERDecoder der = new DERDecoder(authinfob);

        der = new DERDecoder(der.decodeOctetString());

        while ( der.hasNextElement() ) {
            switch (der.peekType()) {
                case ASN1.SEQUENCE_TYPE: 
                   der.startSequence();
                   break;
                case ASN1.OBJECT_IDENTIFIER_TYPE:
                   String oid = der.decodeObjectIdentifier();
                   if ("1.3.6.1.5.5.7.48.1".equals(oid)) {
                     byte[] uri = der.drainElementValue();
                     responderURIs.add(new String(uri));
                   }
                   break;
                case ASN1.IA5_STRING_TYPE:
                   break;
                case ASN1.UTF8_STRING_TYPE:
                   responderURIs.add(der.decodeUtf8String());
                   break;
                case 0xa0:
                   der.decodeImplicit(0xa0);
                   byte[] edata = der.decodeOctetString();
                   while(!Character.isLetterOrDigit(edata[0])) {
                    edata = Arrays.copyOfRange(edata, 1, edata.length);
                }
                   responderURIs.add(new String(edata));
                   break;
                default:
                   der.skipElement();

            }
        }
        
        logger.warn("OCSP Responder URIs" + Arrays.toString(responderURIs.toArray()));
        return responderURIs;
    }
}

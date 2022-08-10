/*
 * Copyright 2016 Analytical Graphics, Inc. and/or its affiliates
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

package org.keycloak.crypto.fips;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.ocsp.OCSPObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.AuthorityInformationAccess;
import org.bouncycastle.asn1.x509.AccessDescription;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.ocsp.BasicOCSPResp;
import org.bouncycastle.cert.ocsp.CertificateID;
import org.bouncycastle.cert.ocsp.CertificateStatus;
import org.bouncycastle.cert.ocsp.OCSPException;
import org.bouncycastle.cert.ocsp.OCSPReq;
import org.bouncycastle.cert.ocsp.OCSPReqBuilder;
import org.bouncycastle.cert.ocsp.OCSPResp;
import org.bouncycastle.cert.ocsp.RevokedStatus;
import org.bouncycastle.cert.ocsp.SingleResp;
import org.bouncycastle.cert.ocsp.UnknownStatus;
import org.bouncycastle.cert.ocsp.jcajce.JcaCertificateID;
import org.bouncycastle.operator.ContentVerifierProvider;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.keycloak.common.util.BouncyIntegration;
import org.keycloak.models.KeycloakSession;
import org.keycloak.utils.OCSPProvider;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.cert.CRLReason;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author <a href="mailto:brat000012001@gmail.com">Peter Nalyvayko</a>
 * @version $Revision: 1 $
 * @since 10/29/2016
 */

public class BCFIPSOCSPProvider extends OCSPProvider {

    private final static Logger logger = Logger.getLogger(BCFIPSOCSPProvider.class.getName());

    protected OCSPResp getResponse(KeycloakSession session, OCSPReq ocspReq, URI responderUri) throws IOException, InterruptedException {
        byte[] data = getEncodedOCSPResponse(session, ocspReq.getEncoded(), responderUri);
        return new OCSPResp(data);
    }

    /**
     * Requests certificate revocation status using OCSP.
     * @param cert the certificate to be checked
     * @param issuerCertificate the issuer certificate
     * @param responderURIs the OCSP responder URIs
     * @param responderCert the OCSP responder certificate
     * @param date if null, the current time is used.
     * @return a revocation status
     * @throws CertPathValidatorException
     */
    @Override
    protected OCSPRevocationStatus check(KeycloakSession session, X509Certificate cert, X509Certificate issuerCertificate, List<URI> responderURIs, X509Certificate responderCert, Date date) throws CertPathValidatorException {
        if (responderURIs == null || responderURIs.size() == 0)
            throw new IllegalArgumentException("Need at least one responder");
        try {

            DigestCalculatorProvider dcp = new JcaDigestCalculatorProviderBuilder().build();

            DigestCalculator digCalc = dcp.get(CertificateID.HASH_SHA1);
            JcaCertificateID certificateID = new JcaCertificateID(digCalc, issuerCertificate, cert.getSerialNumber());

            // Create a nounce extension to protect against replay attacks
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            BigInteger nounce = BigInteger.valueOf(Math.abs(random.nextInt()));

            DEROctetString derString = new DEROctetString(nounce.toByteArray());
            Extension nounceExtension = new Extension(OCSPObjectIdentifiers.id_pkix_ocsp_nonce, false, derString);
            Extensions extensions = new Extensions(nounceExtension);

            OCSPReq ocspReq = new OCSPReqBuilder().addRequest(certificateID, extensions).build();

            URI responderURI = responderURIs.get(0);
            logger.log(Level.INFO, "OCSP Responder {0}", responderURI);

            try {
                OCSPResp resp = getResponse(session, ocspReq, responderURI);
                logger.log(Level.FINE, "Received a response from OCSP responder {0}, the response status is {1}", new Object[]{responderURI, resp.getStatus()});
                switch (resp.getStatus()) {
                    case OCSPResp.SUCCESSFUL:
                        if (resp.getResponseObject() instanceof BasicOCSPResp) {
                            return processBasicOCSPResponse(issuerCertificate, responderCert, date, certificateID, nounce, (BasicOCSPResp)resp.getResponseObject());
                        } else {
                            throw new CertPathValidatorException("OCSP responder returned an invalid or unknown OCSP response.");
                        }

                    case OCSPResp.INTERNAL_ERROR:
                    case OCSPResp.TRY_LATER:
                        throw new CertPathValidatorException("Internal error/try later. OCSP response error: " + resp.getStatus(), (Throwable) null, (CertPath) null, -1, CertPathValidatorException.BasicReason.UNDETERMINED_REVOCATION_STATUS);

                    case OCSPResp.SIG_REQUIRED:
                        throw new CertPathValidatorException("Invalid or missing signature. OCSP response error: " + resp.getStatus(), (Throwable) null, (CertPath) null, -1, CertPathValidatorException.BasicReason.INVALID_SIGNATURE);

                    case OCSPResp.UNAUTHORIZED:
                        throw new CertPathValidatorException("Unauthorized request. OCSP response error: " + resp.getStatus(), (Throwable) null, (CertPath) null, -1, CertPathValidatorException.BasicReason.UNSPECIFIED);

                    case OCSPResp.MALFORMED_REQUEST:
                    default:
                        throw new CertPathValidatorException("OCSP request is malformed. OCSP response error: " + resp.getStatus(), (Throwable) null, (CertPath) null, -1, CertPathValidatorException.BasicReason.UNSPECIFIED);
                }
            }
            catch(IOException | InterruptedException e) {
                logger.log(Level.FINE, "OCSP Responder \"{0}\" failed to return a valid OCSP response\n{1}",
                        new Object[] {responderURI, e.getMessage()});
                throw new CertPathValidatorException("OCSP check failed", e);
            }
        }
        catch(CertificateNotYetValidException | CertificateExpiredException | OperatorCreationException | OCSPException |
              CertificateEncodingException | NoSuchAlgorithmException | NoSuchProviderException e) {
            logger.log(Level.FINE, e.getMessage());
            throw new CertPathValidatorException(e.getMessage(), e);
        }
    }

    private OCSPRevocationStatus processBasicOCSPResponse(X509Certificate issuerCertificate, X509Certificate responderCertificate, Date date, JcaCertificateID certificateID, BigInteger nounce, BasicOCSPResp basicOcspResponse)
            throws OCSPException, NoSuchProviderException, NoSuchAlgorithmException, CertificateNotYetValidException, CertificateExpiredException, CertPathValidatorException {
        SingleResp expectedResponse = null;
        for (SingleResp singleResponse : basicOcspResponse.getResponses()) {
            if (compareCertIDs(certificateID, singleResponse.getCertID())) {
                expectedResponse = singleResponse;
                break;
            }
        }

        if (expectedResponse != null) {
            verifyResponse(basicOcspResponse, issuerCertificate, responderCertificate, nounce.toByteArray(), date);
            return singleResponseToRevocationStatus(expectedResponse);
        } else {
            throw new CertPathValidatorException("OCSP response does not include a response for a certificate supplied in the OCSP request");
        }
    }

    private boolean compareCertIDs(JcaCertificateID idLeft, CertificateID idRight) {
        if (idLeft == idRight)
            return true;
        if (idLeft == null || idRight == null)
            return false;

        return Arrays.equals(idLeft.getIssuerKeyHash(), idRight.getIssuerKeyHash()) &&
                Arrays.equals(idLeft.getIssuerNameHash(), idRight.getIssuerNameHash()) &&
                idLeft.getSerialNumber().equals(idRight.getSerialNumber());
    }

    private void verifyResponse(BasicOCSPResp basicOcspResponse, X509Certificate issuerCertificate, X509Certificate responderCertificate, byte[] requestNonce, Date date) throws NoSuchProviderException, NoSuchAlgorithmException, CertificateNotYetValidException, CertificateExpiredException, CertPathValidatorException {

        List<X509CertificateHolder> certs = new ArrayList<>(Arrays.asList(basicOcspResponse.getCerts()));
        X509Certificate signingCert = null;

        try {
            certs.add(new JcaX509CertificateHolder(issuerCertificate));
            if (responderCertificate != null) {
                certs.add(new JcaX509CertificateHolder(responderCertificate));
            }
        } catch (CertificateEncodingException e) {
            e.printStackTrace();
        }
        if (certs.size() > 0) {

            X500Name responderName = basicOcspResponse.getResponderId().toASN1Primitive().getName();
            byte[] responderKey = basicOcspResponse.getResponderId().toASN1Primitive().getKeyHash();

            if (responderName != null) {
                logger.log(Level.INFO, "Responder Name: {0}", responderName.toString());
                for (X509CertificateHolder certHolder : certs) {
                    try {
                        X509Certificate tempCert = new JcaX509CertificateConverter()
                                .setProvider(BouncyIntegration.PROVIDER).getCertificate(certHolder);
                        X500Name respName = new X500Name(tempCert.getSubjectX500Principal().getName());
                        if (responderName.equals(respName)) {
                            signingCert = tempCert;
                            logger.log(Level.INFO, "Found a certificate whose principal \"{0}\" matches the responder name \"{1}\"",
                                    new Object[] {tempCert.getSubjectDN().getName(), responderName.toString()});
                            break;
                        }
                    } catch (CertificateException e) {
                        logger.log(Level.FINE, e.getMessage());
                    }
                }
            } else if (responderKey != null) {
                SubjectKeyIdentifier responderSubjectKey = new SubjectKeyIdentifier(responderKey);
                logger.log(Level.INFO, "Responder Key: {0}", Arrays.toString(responderKey));
                for (X509CertificateHolder certHolder : certs) {
                    try {
                        X509Certificate tempCert = new JcaX509CertificateConverter()
                                .setProvider(BouncyIntegration.PROVIDER).getCertificate(certHolder);

                        SubjectKeyIdentifier subjectKeyIdentifier = null;
                        if (certHolder.getExtensions() != null) {
                            subjectKeyIdentifier = SubjectKeyIdentifier.fromExtensions(certHolder.getExtensions());
                        }

                        if (subjectKeyIdentifier != null) {
                            logger.log(Level.INFO, "Certificate: {0}\nSubject Key Id: {1}",
                                    new Object[] {tempCert.getSubjectDN().getName(), Arrays.toString(subjectKeyIdentifier.getKeyIdentifier())});
                        }

                        if (subjectKeyIdentifier != null && responderSubjectKey.equals(subjectKeyIdentifier)) {
                            signingCert = tempCert;
                            logger.log(Level.INFO, "Found a signer certificate \"{0}\" with the subject key extension value matching the responder key",
                                    signingCert.getSubjectDN().getName());

                            break;
                        }

                        subjectKeyIdentifier = new JcaX509ExtensionUtils().createSubjectKeyIdentifier(tempCert.getPublicKey());
                        if (responderSubjectKey.equals(subjectKeyIdentifier)) {
                            signingCert = tempCert;
                            logger.log(Level.INFO, "Found a certificate \"{0}\" with the subject key matching the OCSP responder key", signingCert.getSubjectDN().getName());
                            break;
                        }

                    } catch (CertificateException e) {
                        logger.log(Level.FINE, e.getMessage());
                    }
                }
            }
        }
        if (signingCert != null) {
            if (signingCert.equals(issuerCertificate)) {
                logger.log(Level.INFO, "OCSP response is signed by the target''s Issuing CA");
            } else if (responderCertificate != null && signingCert.equals(responderCertificate)) {
                // https://www.ietf.org/rfc/rfc2560.txt
                // 2.6  OCSP Signature Authority Delegation
                // - The responder certificate is issued to the responder by CA
                logger.log(Level.INFO, "OCSP response is signed by an authorized responder certificate");
            } else {
                // 4.2.2.2  Authorized Responders
                // 3. Includes a value of id-ad-ocspSigning in an ExtendedKeyUsage
                // extension and is issued by the CA that issued the certificate in
                // question."
                if (!signingCert.getIssuerX500Principal().equals(issuerCertificate.getSubjectX500Principal())) {
                    logger.log(Level.INFO, "Signer certificate''s Issuer: {0}\nIssuer certificate''s Subject: {1}",
                            new Object[] {signingCert.getIssuerX500Principal().getName(), issuerCertificate.getSubjectX500Principal().getName()});
                    throw new CertPathValidatorException("Responder\'s certificate is not authorized to sign OCSP responses");
                }
                try {
                    List<String> purposes = signingCert.getExtendedKeyUsage();
                    if (purposes == null || !purposes.contains(KeyPurposeId.id_kp_OCSPSigning.getId())) {
                        logger.log(Level.INFO, "OCSPSigning extended usage is not set");
                        throw new CertPathValidatorException("Responder\'s certificate not valid for signing OCSP responses");
                    }
                } catch (CertificateParsingException e) {
                    logger.log(Level.FINE, "Failed to get certificate''s extended key usage extension\n{0}", e.getMessage());
                }
                if (date == null) {
                    signingCert.checkValidity();
                } else {
                    signingCert.checkValidity(date);
                }
                try {
                    Extension noOCSPCheck = new JcaX509CertificateHolder(signingCert).getExtension(OCSPObjectIdentifiers.id_pkix_ocsp_nocheck);
                    // TODO If the extension is present, the OCSP client can trust the
                    // responder's certificate for the lifetime of the certificate.
                    logger.log(Level.INFO, "OCSP no-check extension is {0} present", noOCSPCheck == null ? "not" : "");
                } catch (CertificateEncodingException e) {
                    logger.log(Level.FINE, "Certificate encoding exception: {0}", e.getMessage());
                }

                try {
                    signingCert.verify(issuerCertificate.getPublicKey());
                    logger.log(Level.INFO, "OCSP response is signed by an Authorized Responder");

                } catch (GeneralSecurityException ex) {
                    signingCert = null;
                }
            }
        }
        if (signingCert == null) {
            throw new CertPathValidatorException("Unable to verify OCSP Response\'s signature");
        } else {
            if (!verifySignature(basicOcspResponse, signingCert)) {
                throw new CertPathValidatorException("Error verifying OCSP Response\'s signature");
            } else {
                Extension responseNonce = basicOcspResponse.getExtension(OCSPObjectIdentifiers.id_pkix_ocsp_nonce);
                if (responseNonce != null && requestNonce != null && !Arrays.equals(requestNonce, responseNonce.getExtnValue().getOctets())) {
                    throw new CertPathValidatorException("Nonces do not match.");
                } else {
                    // See Sun's OCSP implementation.
                    // https://www.ietf.org/rfc/rfc2560.txt, if nextUpdate is not set,
                    // the responder is indicating that newer update is avilable all the time
                    long current = date == null ? System.currentTimeMillis() : date.getTime();
                    Date stop = new Date(current + (long) TIME_SKEW);
                    Date start = new Date(current - (long) TIME_SKEW);

                    Iterator<SingleResp> iter = Arrays.asList(basicOcspResponse.getResponses()).iterator();
                    SingleResp singleRes = null;
                    do {
                        if (!iter.hasNext()) {
                            return;
                        }
                        singleRes = iter.next();
                    }
                    while (!stop.before(singleRes.getThisUpdate()) &&
                            !start.after(singleRes.getNextUpdate() != null ? singleRes.getNextUpdate() : singleRes.getThisUpdate()));

                    throw new CertPathValidatorException("Response is unreliable: its validity interval is out-of-date");
                }
            }
        }
    }

    private boolean verifySignature(BasicOCSPResp basicOcspResponse, X509Certificate cert) {
        try {
            ContentVerifierProvider contentVerifier = new JcaContentVerifierProviderBuilder()
                    .setProvider(BouncyIntegration.PROVIDER).build(cert.getPublicKey());
            return basicOcspResponse.isSignatureValid(contentVerifier);
        } catch (OperatorCreationException e) {
            logger.log(Level.FINE, "Unable to construct OCSP content signature verifier\n{0}", e.getMessage());
        } catch (OCSPException e) {
            logger.log(Level.FINE, "Unable to validate OCSP response signature\n{0}", e.getMessage());
        }
        return false;
    }

    private OCSPRevocationStatus singleResponseToRevocationStatus(final SingleResp singleResponse) throws CertPathValidatorException {
        final CertificateStatus certStatus = singleResponse.getCertStatus();

        CRLReason revocationReason = CRLReason.UNSPECIFIED;
        Date revocationTime = null;
        RevocationStatus status = RevocationStatus.UNKNOWN;
        if (certStatus == CertificateStatus.GOOD) {
            status = RevocationStatus.GOOD;
        } else if (certStatus instanceof RevokedStatus) {
            RevokedStatus revoked = (RevokedStatus)certStatus;
            revocationTime = revoked.getRevocationTime();
            status = RevocationStatus.REVOKED;
            if (revoked.hasRevocationReason()) {
                revocationReason = CRLReason.values()[revoked.getRevocationReason()];
            }
        } else if (certStatus instanceof UnknownStatus) {
            status = RevocationStatus.UNKNOWN;
        } else {
            throw new CertPathValidatorException("Unrecognized revocation status received from OCSP.");
        }

        final RevocationStatus finalStatus = status;
        final Date finalRevocationTime = revocationTime;
        final CRLReason finalRevocationReason = revocationReason;
        return new OCSPRevocationStatus() {
            @Override
            public RevocationStatus getRevocationStatus() {
                return finalStatus;
            }

            @Override
            public Date getRevocationTime() {
                return finalRevocationTime;
            }

            @Override
            public CRLReason getRevocationReason() {
                return finalRevocationReason;
            }
        };
    }


    /**
     * Extracts OCSP responder URI from X509 AIA v3 extension, if available. There can be
     * multiple responder URIs encoded in the certificate.
     * @param cert
     * @return a list of available responder URIs.
     * @throws CertificateEncodingException
     */
    @Override
    protected List<String> getResponderURIs(X509Certificate cert) throws CertificateEncodingException {

        LinkedList<String> responderURIs = new LinkedList<>();
        JcaX509CertificateHolder holder = new JcaX509CertificateHolder(cert);
        Extension aia = holder.getExtension(Extension.authorityInfoAccess);
        if (aia != null) {
            try {
                ASN1InputStream in = new ASN1InputStream(aia.getExtnValue().getOctetStream());
                ASN1Sequence seq = (ASN1Sequence)in.readObject();
                AuthorityInformationAccess authorityInfoAccess = AuthorityInformationAccess.getInstance(seq);
                for (AccessDescription ad : authorityInfoAccess.getAccessDescriptions()) {
                    if (ad.getAccessMethod().equals(AccessDescription.id_ad_ocsp)) {
                        // See https://www.ietf.org/rfc/rfc2560.txt, 3.1 Certificate Content
                        if (ad.getAccessLocation().getTagNo() == GeneralName.uniformResourceIdentifier) {
                            DERIA5String value = DERIA5String.getInstance(ad.getAccessLocation().getName());
                            responderURIs.add(value.getString());
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return responderURIs;
    }
}

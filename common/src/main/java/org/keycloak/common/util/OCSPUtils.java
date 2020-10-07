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

package org.keycloak.common.util;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.ocsp.OCSPObjectIdentifiers;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.CRLReason;
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
import org.bouncycastle.cert.ocsp.*;
import org.bouncycastle.cert.ocsp.jcajce.JcaCertificateID;
import org.bouncycastle.operator.ContentVerifierProvider;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcDigestCalculatorProvider;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;

import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.cert.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:brat000012001@gmail.com">Peter Nalyvayko</a>
 * @version $Revision: 1 $
 * @since 10/29/2016
 */

public final class OCSPUtils {


    static {
        BouncyIntegration.init();
    }

    private final static Logger logger = Logger.getLogger(""+OCSPUtils.class);

    private static int OCSP_CONNECT_TIMEOUT = 10000; // 10 sec
    private static final int TIME_SKEW = 900000;

    public enum RevocationStatus {
        GOOD,
        REVOKED,
        UNKNOWN
    }

    public interface OCSPRevocationStatus {
        RevocationStatus getRevocationStatus();
        Date getRevocationTime();
        CRLReason getRevocationReason();
    }

    /**
     * Requests certificate revocation status using OCSP.
     * @param cert the certificate to be checked
     * @param issuerCertificate The issuer certificate
     * @param responderURI an address of OCSP responder. Overrides any OCSP responder URIs stored in certificate's AIA extension
     * @param date
     * @param responderCert a certificate that OCSP responder uses to sign OCSP responses
     * @return revocation status
     */
    public static OCSPRevocationStatus check(X509Certificate cert, X509Certificate issuerCertificate, URI responderURI, X509Certificate responderCert, Date date) throws CertPathValidatorException {
        if (cert == null)
            throw new IllegalArgumentException("cert cannot be null");
        if (issuerCertificate == null)
            throw new IllegalArgumentException("issuerCertificate cannot be null");
        if (responderURI == null)
            throw new IllegalArgumentException("responderURI cannot be null");

        return check(cert, issuerCertificate, Collections.singletonList(responderURI), responderCert, date);
    }
    /**
     * Requests certificate revocation status using OCSP. The OCSP responder URI
     * is obtained from the certificate's AIA extension.
     * @param cert the certificate to be checked
     * @param issuerCertificate The issuer certificate
     * @param date
     * @return revocation status
     */
    public static OCSPRevocationStatus check(X509Certificate cert, X509Certificate issuerCertificate, Date date, X509Certificate responderCert) throws CertPathValidatorException {
        List<String> responderURIs = null;
        try {
            responderURIs = getResponderURIs(cert);
        } catch (CertificateEncodingException e) {
            logger.log(Level.FINE, "CertificateEncodingException: {0}", e.getMessage());
            throw new CertPathValidatorException(e.getMessage(), e);
        }
        if (responderURIs.size() == 0) {
            logger.log(Level.INFO, "No OCSP responders in the specified certificate");
            throw new CertPathValidatorException("No OCSP Responder URI in certificate");
        }

        List<URI> uris = new LinkedList<>();
        for (String value : responderURIs) {
            try {
                URI responderURI = URI.create(value);
                uris.add(responderURI);
            } catch (IllegalArgumentException ex) {
                logger.log(Level.FINE, "Malformed responder URI {0}", value);
            }
        }
        return check(cert, issuerCertificate, Collections.unmodifiableList(uris), responderCert, date);
    }
    /**
     * Requests certificate revocation status using OCSP. The OCSP responder URI
     * is obtained from the certificate's AIA extension.
     * @param cert the certificate to be checked
     * @param issuerCertificate The issuer certificate
     * @return revocation status
     */
    public static OCSPRevocationStatus check(X509Certificate cert, X509Certificate issuerCertificate) throws CertPathValidatorException {
        return check(cert, issuerCertificate, null, null);
    }

    private static OCSPResp getResponse(OCSPReq ocspReq, URI responderUri) throws IOException {
        DataOutputStream dataOut = null;
        InputStream in = null;
        try {
            byte[] array = ocspReq.getEncoded();
            URL urlt = responderUri.toURL();
            HttpURLConnection con = (HttpURLConnection) urlt.openConnection();
            con.setRequestMethod("POST");
            con.setConnectTimeout(OCSP_CONNECT_TIMEOUT);
            con.setReadTimeout(OCSP_CONNECT_TIMEOUT);
            con.setRequestProperty("Content-type", "application/ocsp-request");
            con.setRequestProperty("Content-length", String.valueOf(array.length));
//        con.setRequestProperty("Accept", "application/ocsp-response");

            con.setDoOutput(true);
            con.setDoInput(true);
            OutputStream out = con.getOutputStream();
            dataOut = new DataOutputStream(new BufferedOutputStream(out));
            dataOut.write(array);
            dataOut.flush();

            if (con.getResponseCode() / 100 != 2) {
                String errorMessage = String.format("Connection error, unable to obtain certificate revocation status using OCSP responder \"%s\", code \"%d\"",
                        responderUri.toString(), con.getResponseCode());
                throw new IOException(errorMessage);
            }
            //Get Response
            in = (InputStream) con.getInputStream();
            int contentLen = con.getContentLength();
            if (contentLen == -1) {
                contentLen = Integer.MAX_VALUE;
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int bytesRead = 0;
            byte[] buffer = new byte[2048];
            while ((bytesRead = in.read(buffer, 0, buffer.length)) >= 0) {
                baos.write(buffer, 0, bytesRead);
            }
            baos.flush();
            byte[] data = baos.toByteArray();
            return new OCSPResp(data);
        } finally {
            if (dataOut != null) {
                dataOut.close();
            }
            if (in != null) {
                in.close();
            }
        }
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
    private static OCSPRevocationStatus check(X509Certificate cert, X509Certificate issuerCertificate, List<URI> responderURIs, X509Certificate responderCert, Date date) throws CertPathValidatorException {
        if (responderURIs == null || responderURIs.size() == 0)
            throw new IllegalArgumentException("Need at least one responder");
        try {
            DigestCalculator digCalc = new BcDigestCalculatorProvider()
                    .get(new AlgorithmIdentifier(OIWObjectIdentifiers.idSHA1));

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
                OCSPResp resp = getResponse(ocspReq, responderURI);
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
            catch(IOException e) {
                logger.log(Level.FINE, "OCSP Responder \"{0}\" failed to return a valid OCSP response\n{1}",
                        new Object[] {responderURI, e.getMessage()});
                throw new CertPathValidatorException("OCSP check failed", e);
            }
        }
        catch(CertificateNotYetValidException | CertificateExpiredException | OperatorCreationException | OCSPException | CertificateEncodingException | NoSuchAlgorithmException | NoSuchProviderException e) {
            logger.log(Level.FINE, e.getMessage());
            throw new CertPathValidatorException(e.getMessage(), e);
        }
    }

    private static OCSPRevocationStatus processBasicOCSPResponse(X509Certificate issuerCertificate, X509Certificate responderCertificate, Date date, JcaCertificateID certificateID, BigInteger nounce, BasicOCSPResp basicOcspResponse)
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

    private static boolean compareCertIDs(JcaCertificateID idLeft, CertificateID idRight) {
        if (idLeft == idRight)
            return true;
        if (idLeft == null || idRight == null)
            return false;

        return Arrays.equals(idLeft.getIssuerKeyHash(), idRight.getIssuerKeyHash()) &&
                Arrays.equals(idLeft.getIssuerNameHash(), idRight.getIssuerNameHash()) &&
                idLeft.getSerialNumber().equals(idRight.getSerialNumber());
    }

    private static void verifyResponse(BasicOCSPResp basicOcspResponse, X509Certificate issuerCertificate, X509Certificate responderCertificate, byte[] requestNonce, Date date) throws NoSuchProviderException, NoSuchAlgorithmException, CertificateNotYetValidException, CertificateExpiredException, CertPathValidatorException {

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
                                .setProvider("BC").getCertificate(certHolder);
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
                                .setProvider("BC").getCertificate(certHolder);

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

    private static boolean verifySignature(BasicOCSPResp basicOcspResponse, X509Certificate cert) {
        try {
            ContentVerifierProvider contentVerifier = new JcaContentVerifierProviderBuilder()
                    .setProvider("BC").build(cert.getPublicKey());
            return basicOcspResponse.isSignatureValid(contentVerifier);
        } catch (OperatorCreationException e) {
            logger.log(Level.FINE, "Unable to construct OCSP content signature verifier\n{0}", e.getMessage());
        } catch (OCSPException e) {
            logger.log(Level.FINE, "Unable to validate OCSP response signature\n{0}", e.getMessage());
        }
        return false;
    }

    private static OCSPRevocationStatus unknownStatus() {
        return new OCSPRevocationStatus() {
            @Override
            public RevocationStatus getRevocationStatus() {
                return RevocationStatus.UNKNOWN;
            }

            @Override
            public Date getRevocationTime() {
                return new Date(System.currentTimeMillis());
            }

            @Override
            public CRLReason getRevocationReason() {
                return CRLReason.lookup(CRLReason.unspecified);
            }
        };
    }

    private static OCSPRevocationStatus singleResponseToRevocationStatus(final SingleResp singleResponse) throws CertPathValidatorException {
        final CertificateStatus certStatus = singleResponse.getCertStatus();

        int revocationReason = CRLReason.unspecified;
        Date revocationTime = null;
        RevocationStatus status = RevocationStatus.UNKNOWN;
        if (certStatus == CertificateStatus.GOOD) {
            status = RevocationStatus.GOOD;
        } else if (certStatus instanceof RevokedStatus) {
            RevokedStatus revoked = (RevokedStatus)certStatus;
            revocationTime = revoked.getRevocationTime();
            status = RevocationStatus.REVOKED;
            if (revoked.hasRevocationReason()) {
                revocationReason = revoked.getRevocationReason();
            }
        } else if (certStatus instanceof UnknownStatus) {
            status = RevocationStatus.UNKNOWN;
        } else {
            throw new CertPathValidatorException("Unrecognized revocation status received from OCSP.");
        }

        final RevocationStatus finalStatus = status;
        final Date finalRevocationTime = revocationTime;
        final int finalRevocationReason = revocationReason;
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
                return CRLReason.lookup(finalRevocationReason);
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
    private static List<String> getResponderURIs(X509Certificate cert) throws CertificateEncodingException {

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

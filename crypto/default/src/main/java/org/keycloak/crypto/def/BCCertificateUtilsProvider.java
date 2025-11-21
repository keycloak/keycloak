/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.crypto.def;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.keycloak.common.crypto.CertificateUtilsProvider;
import org.keycloak.common.util.BouncyIntegration;
import org.keycloak.crypto.JavaAlgorithm;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.CertificatePolicies;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.PolicyInformation;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v1CertificateBuilder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 * The Class CertificateUtils provides utility functions for generation of V1 and V3 {@link X509Certificate}
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author <a href="mailto:giriraj.sharma27@gmail.com">Giriraj Sharma</a>
 * @version $Revision: 2 $
 */
public class BCCertificateUtilsProvider implements CertificateUtilsProvider {

    /**
     * Generates version 3 {@link X509Certificate}.
     *
     * @param keyPair      the key pair
     * @param caPrivateKey the CA private key
     * @param caCert       the CA certificate
     * @param subject      the subject name
     * @return the x509 certificate
     */
    public X509Certificate generateV3Certificate(KeyPair keyPair, PrivateKey caPrivateKey, X509Certificate caCert,
                                                 String subject) {
        try {
            X500Name subjectDN = new X500Name("CN=" + subject);

            // Serial Number
            SecureRandom random = new SecureRandom();
            BigInteger serialNumber = BigInteger.valueOf(Math.abs(random.nextInt()));

            // Validity
            Date notBefore = new Date(System.currentTimeMillis());
            Date notAfter = new Date(System.currentTimeMillis() + (((1000L * 60 * 60 * 24 * 30)) * 12) * 3);

            // SubjectPublicKeyInfo
            SubjectPublicKeyInfo subjPubKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());

            X509v3CertificateBuilder certGen = new X509v3CertificateBuilder(new X500Name(caCert.getSubjectDN().getName()),
                    serialNumber, notBefore, notAfter, subjectDN, subjPubKeyInfo);

            JcaX509ExtensionUtils x509ExtensionUtils = new JcaX509ExtensionUtils();

            // Subject Key Identifier
            certGen.addExtension(Extension.subjectKeyIdentifier, false,
                    x509ExtensionUtils.createSubjectKeyIdentifier(subjPubKeyInfo));

            // Authority Key Identifier
            certGen.addExtension(Extension.authorityKeyIdentifier, false,
                    x509ExtensionUtils.createAuthorityKeyIdentifier(caCert));

            // Key Usage
            certGen.addExtension(Extension.keyUsage, false, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyCertSign
                    | KeyUsage.cRLSign));

            // Extended Key Usage
            KeyPurposeId[] EKU = new KeyPurposeId[2];
            EKU[0] = KeyPurposeId.id_kp_emailProtection;
            EKU[1] = KeyPurposeId.id_kp_serverAuth;

            certGen.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(EKU));

            // Basic Constraints
            certGen.addExtension(Extension.basicConstraints, true, new BasicConstraints(0));

            // Content Signer
            ContentSigner sigGen;
            switch (caCert.getPublicKey().getAlgorithm())
            {
                case "EC":
                    sigGen = new JcaContentSignerBuilder("SHA256WithECDSA").setProvider(BouncyIntegration.PROVIDER)
                                                                               .build(caPrivateKey);
                    break;
                default:
                    sigGen = new JcaContentSignerBuilder("SHA256WithRSAEncryption").setProvider(BouncyIntegration.PROVIDER)
                                                                                       .build(caPrivateKey);
            }

            // Certificate
            return new JcaX509CertificateConverter().setProvider(BouncyIntegration.PROVIDER).getCertificate(certGen.build(sigGen));
        } catch (Exception e) {
            throw new RuntimeException("Error creating X509v3Certificate.", e);
        }
    }

    /**
     * Generate version 1 self signed {@link X509Certificate}..
     *
     * @param caKeyPair the CA key pair
     * @param subject   the subject name
     * @return the x509 certificate
     * @throws Exception the exception
     */
    public X509Certificate generateV1SelfSignedCertificate(KeyPair caKeyPair, String subject) {
        return generateV1SelfSignedCertificate(caKeyPair, subject, BigInteger.valueOf(System.currentTimeMillis()));
    }

    public X509Certificate generateV1SelfSignedCertificate(KeyPair caKeyPair, String subject, BigInteger serialNumber) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 10);
        return generateV1SelfSignedCertificate(caKeyPair, subject, serialNumber, calendar.getTime());
    }

    @Override
    public X509Certificate generateV1SelfSignedCertificate(KeyPair caKeyPair, String subject, BigInteger serialNumber, Date validityEndDate) {
        try {
            X500Name subjectDN = new X500Name("CN=" + subject);
            Date validityStartDate = new Date(System.currentTimeMillis() - 100000);
            SubjectPublicKeyInfo subPubKeyInfo = SubjectPublicKeyInfo.getInstance(caKeyPair.getPublic().getEncoded());

            X509v1CertificateBuilder builder = new X509v1CertificateBuilder(subjectDN, serialNumber, validityStartDate,
                    validityEndDate, subjectDN, subPubKeyInfo);
            X509CertificateHolder holder = builder.build(createSigner(caKeyPair.getPrivate()));

            return new JcaX509CertificateConverter().getCertificate(holder);
        } catch (Exception e) {
            throw new RuntimeException("Error creating X509v1Certificate.", e);
        }
    }

    /**
     * Creates the content signer for generation of Version 1 {@link X509Certificate}.
     *
     * @param privateKey the private key
     * @return the content signer
     */
    private ContentSigner createSigner(PrivateKey privateKey) {
        try {
            JcaContentSignerBuilder signerBuilder;
            switch (privateKey.getAlgorithm()) {
                case "RSA": {
                    signerBuilder = new JcaContentSignerBuilder("SHA256WithRSAEncryption")
                            .setProvider(BouncyIntegration.PROVIDER);
                    break;
                }
                case "EC":
                case "ECDSA": {
                    signerBuilder = new JcaContentSignerBuilder("SHA256WithECDSA")
                            .setProvider(BouncyIntegration.PROVIDER);
                    break;
                }
                case JavaAlgorithm.Ed25519:
                case JavaAlgorithm.Ed448: {
                    signerBuilder = new JcaContentSignerBuilder(privateKey.getAlgorithm())
                            .setProvider(BouncyIntegration.PROVIDER);
                    break;
                }
                default: {
                    throw new RuntimeException(String.format("Keytype %s is not supported.", privateKey.getAlgorithm()));
                }
            }
            return signerBuilder.build(privateKey);
        } catch (Exception e) {
            throw new RuntimeException("Could not create content signer.", e);
        }
    }

    @Override
    public List<String> getCertificatePolicyList(X509Certificate cert) throws GeneralSecurityException {

        Extensions certExtensions = new JcaX509CertificateHolder(cert).getExtensions();
        if (certExtensions == null)
            throw new GeneralSecurityException("Certificate Policy validation was expected, but no certificate extensions were found");

        CertificatePolicies policies = CertificatePolicies.fromExtensions(certExtensions);

        if (policies == null)
            throw new GeneralSecurityException("Certificate Policy validation was expected, but no certificate policy extensions were found");

        List<String> policyList = new LinkedList<>();
        Arrays.stream(policies.getPolicyInformation()).forEach(p -> policyList.add(p.getPolicyIdentifier().toString().toLowerCase()));

        return policyList;
    }


    /**
     * Retrieves a list of CRL distribution points from CRLDP v3 certificate extension
     * See <a href="www.nakov.com/blog/2009/12/01/x509-certificate-validation-in-java-build-and-verify-cchain-and-verify-clr-with-bouncy-castle/">CRL validation</a>
     *
     * @param cert
     * @return
     * @throws IOException
     */
    public List<String> getCRLDistributionPoints(X509Certificate cert) throws IOException {
        byte[] data = cert.getExtensionValue(CRL_DISTRIBUTION_POINTS_OID);
        if (data == null) {
            return Collections.emptyList();
        }

        List<String> distributionPointUrls = new LinkedList<>();
        DEROctetString octetString;
        try (ASN1InputStream crldpExtensionInputStream = new ASN1InputStream(new ByteArrayInputStream(data))) {
            octetString = (DEROctetString) crldpExtensionInputStream.readObject();
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

    public X509Certificate createServicesTestCertificate(String dn,
                                                         Date startDate,
                                                         Date expiryDate,
                                                         KeyPair keyPair,
                                                         String... certificatePolicyOid) {
        // Cert data
        X500Name subjectDN = new X500Name(dn);
        X500Name issuerDN = new X500Name(dn);

        SubjectPublicKeyInfo subjPubKeyInfo = SubjectPublicKeyInfo.getInstance(
                ASN1Sequence.getInstance(keyPair.getPublic().getEncoded()));

        BigInteger serialNumber = new BigInteger(130, new SecureRandom());

        // Build the certificate
        X509v3CertificateBuilder certGen = new X509v3CertificateBuilder(issuerDN, serialNumber, startDate, expiryDate,
                subjectDN, subjPubKeyInfo);

        if (certificatePolicyOid != null) {
            try {
                for (Extension certExtension : certPolicyExtensions(certificatePolicyOid))
                    certGen.addExtension(certExtension);
            } catch (CertIOException e) {
                throw new IllegalStateException(e);
            }
        }

        // Sign the cert with the private key
        try {
            ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256withRSA")
                    .setProvider(BouncyIntegration.PROVIDER)
                    .build(keyPair.getPrivate());
            X509Certificate x509Certificate = new JcaX509CertificateConverter()
                    .setProvider(BouncyIntegration.PROVIDER)
                    .getCertificate(certGen.build(contentSigner));

            return x509Certificate;
        } catch (CertificateException | OperatorCreationException e) {
            throw new IllegalStateException(e);
        }
    }

    private List<Extension> certPolicyExtensions(String... certificatePolicyOid) {
        List<Extension> certificatePolicies = new LinkedList<>();

        if (certificatePolicyOid != null && certificatePolicyOid.length > 0) {
            List<PolicyInformation> policyInfoList = new LinkedList<>();
            for (String oid : certificatePolicyOid) {
                policyInfoList.add(new PolicyInformation(new ASN1ObjectIdentifier(oid)));
            }

            CertificatePolicies policies = new CertificatePolicies(policyInfoList.toArray(new PolicyInformation[0]));

            try {
                boolean isCritical = false;
                Extension extension = new Extension(Extension.certificatePolicies, isCritical, policies.getEncoded());
                certificatePolicies.add(extension);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return certificatePolicies;
    }

}

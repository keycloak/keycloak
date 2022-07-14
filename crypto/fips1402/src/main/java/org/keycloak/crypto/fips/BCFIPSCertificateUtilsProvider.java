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

package org.keycloak.crypto.fips;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v1CertificateBuilder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.keycloak.common.util.BouncyIntegration;
import org.keycloak.common.crypto.CertificateUtilsProvider;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;

/**
 * The Class CertificateUtils provides utility functions for generation of V1 and V3 {@link java.security.cert.X509Certificate}
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author <a href="mailto:giriraj.sharma27@gmail.com">Giriraj Sharma</a>
 * @version $Revision: 2 $
 */
public class BCFIPSCertificateUtilsProvider implements CertificateUtilsProvider{

    /**
     * Generates version 3 {@link java.security.cert.X509Certificate}.
     *
     * @param keyPair the key pair
     * @param caPrivateKey the CA private key
     * @param caCert the CA certificate
     * @param subject the subject name
     * 
     * @return the x509 certificate
     * 
     * @throws Exception the exception
     */
    public X509Certificate generateV3Certificate(KeyPair keyPair, PrivateKey caPrivateKey, X509Certificate caCert,
            String subject) throws Exception {
        try {
            X500Name subjectDN = new X500Name("CN=" + subject);

            // Serial Number
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
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
                    x509ExtensionUtils.createAuthorityKeyIdentifier(subjPubKeyInfo));

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
            ContentSigner sigGen = new JcaContentSignerBuilder("SHA1WithRSAEncryption").setProvider(BouncyIntegration.PROVIDER).build(caPrivateKey);

            // Certificate
            return new JcaX509CertificateConverter().setProvider(BouncyIntegration.PROVIDER).getCertificate(certGen.build(sigGen));
        } catch (Exception e) {
            throw new RuntimeException("Error creating X509v3Certificate.", e);
        }
    }

    /**
     * Generate version 1 self signed {@link java.security.cert.X509Certificate}..
     *
     * @param caKeyPair the CA key pair
     * @param subject the subject name
     * 
     * @return the x509 certificate
     * 
     * @throws Exception the exception
     */
    public X509Certificate generateV1SelfSignedCertificate(KeyPair caKeyPair, String subject) {
        return generateV1SelfSignedCertificate(caKeyPair, subject, BigInteger.valueOf(System.currentTimeMillis()));
    }

    public X509Certificate generateV1SelfSignedCertificate(KeyPair caKeyPair, String subject, BigInteger serialNumber) {
        try {
            X500Name subjectDN = new X500Name("CN=" + subject);
            Date validityStartDate = new Date(System.currentTimeMillis() - 100000);
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.YEAR, 10);
            Date validityEndDate = new Date(calendar.getTime().getTime());
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
     * Creates the content signer for generation of Version 1 {@link java.security.cert.X509Certificate}.
     *
     * @param privateKey the private key
     *
     * @return the content signer
     */
    private ContentSigner createSigner(PrivateKey privateKey) {
        try {
            JcaContentSignerBuilder signerBuilder = new JcaContentSignerBuilder("SHA256WithRSAEncryption")
                    .setProvider(BouncyIntegration.PROVIDER);
            return signerBuilder.build(privateKey);
        } catch (Exception e) {
            throw new RuntimeException("Could not create content signer.", e);
        }
    }
}

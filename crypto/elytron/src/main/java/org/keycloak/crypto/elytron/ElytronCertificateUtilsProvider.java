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

package org.keycloak.crypto.elytron;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.security.auth.x500.X500Principal;

import org.keycloak.common.crypto.CertificateUtilsProvider;

import org.jboss.logging.Logger;
import org.wildfly.security.asn1.ASN1;
import org.wildfly.security.asn1.DERDecoder;
import org.wildfly.security.x500.GeneralName;
import org.wildfly.security.x500.X500;
import org.wildfly.security.x500.cert.AuthorityKeyIdentifierExtension;
import org.wildfly.security.x500.cert.BasicConstraintsExtension;
import org.wildfly.security.x500.cert.CertificatePoliciesExtension;
import org.wildfly.security.x500.cert.CertificatePoliciesExtension.PolicyInformation;
import org.wildfly.security.x500.cert.ExtendedKeyUsageExtension;
import org.wildfly.security.x500.cert.KeyUsage;
import org.wildfly.security.x500.cert.KeyUsageExtension;
import org.wildfly.security.x500.cert.SubjectKeyIdentifierExtension;
import org.wildfly.security.x500.cert.X509CertificateBuilder;
import org.wildfly.security.x500.cert.X509CertificateExtension;
import org.wildfly.security.x500.cert.util.KeyUtil;

/**
 * The Class CertificateUtils provides utility functions for generation
 * and usage of X.509 certificates
 *
 * @author <a href="mailto:david.anderson@redhat.com">David Anderson</a>
 */
public class ElytronCertificateUtilsProvider implements CertificateUtilsProvider {

    Logger log = Logger.getLogger(getClass());

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
    @Override
    public X509Certificate generateV3Certificate(KeyPair keyPair, PrivateKey caPrivateKey,
            X509Certificate caCert,
            String subject) throws Exception {
        try {

            X500Principal subjectdn = subjectToX500Principle(subject);
            X500Principal issuerdn = caCert.getSubjectX500Principal();

            // Validity
            ZonedDateTime notBefore = ZonedDateTime.ofInstant(new Date(System.currentTimeMillis()).toInstant(),
                    ZoneId.systemDefault());
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.YEAR, 3);
            Date validityEndDate = new Date(calendar.getTime().getTime());
            ZonedDateTime notAfter = ZonedDateTime.ofInstant(validityEndDate.toInstant(),
                    ZoneId.systemDefault());
            // Serial Number
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            BigInteger serialNumber = BigInteger.valueOf(Math.abs(random.nextInt()));
            // Extended Key Usage
            ArrayList<String> ekuList = new ArrayList<String>();
            ekuList.add(X500.OID_KP_EMAIL_PROTECTION);
            ekuList.add(X500.OID_KP_SERVER_AUTH);

            X509CertificateBuilder cbuilder = new X509CertificateBuilder()
                    .setSubjectDn(subjectdn)
                    .setIssuerDn(issuerdn)

                    .setNotValidBefore(notBefore)
                    .setNotValidAfter(notAfter)

                    .setPublicKey(keyPair.getPublic())

                    .setSerialNumber(serialNumber)

                    .setSigningKey(caPrivateKey)

                    // Subject Key Identifier Extension
                    .addExtension(new SubjectKeyIdentifierExtension(KeyUtil.getKeyIdentifier(keyPair.getPublic())))

                    // Authority Key Identifier
                    .addExtension(new AuthorityKeyIdentifierExtension(
                            KeyUtil.getKeyIdentifier(caCert.getPublicKey()),
                            Collections.singletonList(new GeneralName.DirectoryName(caCert.getIssuerX500Principal().getName())),
                            caCert.getSerialNumber()
                    ))

                    // Key Usage
                    .addExtension(
                            new KeyUsageExtension(KeyUsage.digitalSignature, KeyUsage.keyCertSign, KeyUsage.cRLSign))

                    .addExtension(new ExtendedKeyUsageExtension(false, ekuList))

                    // Basic Constraints
                    .addExtension(new BasicConstraintsExtension(true, true, 0));

            switch (caPrivateKey.getAlgorithm()){
                case "EC":
                    cbuilder.setSignatureAlgorithmName("SHA256withECDSA");
                    break;
                default:
                    cbuilder.setSignatureAlgorithmName("SHA256withRSA");
            }

            return cbuilder.build();

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
    @Override
    public X509Certificate generateV1SelfSignedCertificate(KeyPair caKeyPair, String subject) {
        return generateV1SelfSignedCertificate(caKeyPair, subject, BigInteger.valueOf(System.currentTimeMillis()));
    }

    @Override
    public X509Certificate generateV1SelfSignedCertificate(KeyPair caKeyPair, String subject,
            BigInteger serialNumber) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 10);
        return generateV1SelfSignedCertificate(caKeyPair, subject, serialNumber, calendar.getTime());
    }

    @Override
    public X509Certificate generateV1SelfSignedCertificate(KeyPair caKeyPair, String subject, BigInteger serialNumber, Date validityEndDate) {
        try {

            X500Principal subjectdn = subjectToX500Principle(subject);

            ZonedDateTime notBefore = ZonedDateTime.ofInstant(
                    (new Date(System.currentTimeMillis() - 100000)).toInstant(),
                    ZoneId.systemDefault());

            ZonedDateTime notAfter = ZonedDateTime.ofInstant(validityEndDate.toInstant(),
                    ZoneId.systemDefault());

            X509CertificateBuilder cbuilder = new X509CertificateBuilder()
                    .setSubjectDn(subjectdn)
                    .setIssuerDn(subjectdn)
                    .setNotValidBefore(notBefore)
                    .setNotValidAfter(notAfter)

                    .setSigningKey(caKeyPair.getPrivate())
                    .setPublicKey(caKeyPair.getPublic())

                    .setSerialNumber(serialNumber);

            switch (caKeyPair.getPrivate().getAlgorithm()){
                case "EC":
                    cbuilder.setSignatureAlgorithmName("SHA256withECDSA");
                    break;
                default:
                    cbuilder.setSignatureAlgorithmName("SHA256withRSA");
            }

            return cbuilder.build();

        } catch (Exception e) {
            throw new RuntimeException("Error creating X509v1Certificate.", e);
        }
    }


    // Some subject names will not conform to the RFC format
    private static X500Principal subjectToX500Principle(String subject) {
        if(!subject.startsWith("CN=")) {
            subject = "CN="+subject;
        }
        return new X500Principal(subject);
    }

    @Override
    public List<String> getCertificatePolicyList(X509Certificate cert) throws GeneralSecurityException {
        byte[] policy = cert.getExtensionValue("2.5.29.32");

        System.out.println("Policy: " + new String(policy));
        DERDecoder decPolicy = new DERDecoder(policy);

        int type = decPolicy.peekType();
        System.out.println("type " + type);

        DERDecoder der = new DERDecoder(decPolicy.decodeOctetString());

        List<String> policyList =new ArrayList<>();

        while (der.hasNextElement()) {
            switch (der.peekType()) {
                case ASN1.SEQUENCE_TYPE:
                   der.startSequence();
                   break;
                case ASN1.OBJECT_IDENTIFIER_TYPE:
                   policyList.add(der.decodeObjectIdentifier());
                   der.endSequence();
                   break;
                default:
                   der.skipElement();

            }
        }

        return policyList;
    }

    @Override
    public List<String> getCRLDistributionPoints(X509Certificate cert) throws IOException {
        byte[] data = cert.getExtensionValue(CRL_DISTRIBUTION_POINTS_OID);
        if (data == null) {
            return Collections.emptyList();
        }
        List<String> distPointUrls = new ArrayList<>();
        DERDecoder der = new DERDecoder(data);

        der = new DERDecoder(der.decodeOctetString());

        while ( der.hasNextElement() ) {
            switch (der.peekType()) {
                case ASN1.SEQUENCE_TYPE:
                   der.startSequence();
                   break;
                case ASN1.UTF8_STRING_TYPE:
                   distPointUrls.add(der.decodeUtf8String());
                   break;
                case 0xa0: // Decode CRLDistributionPoint FullName list
                   der.startExplicit(0xa0);
                   break;
                case 0x86: // Decode CRLDistributionPoint FullName
                   der.decodeImplicit(0x86);
                   distPointUrls.add(der.decodeOctetStringAsString());
                   log.debug("Adding Dist point name: " + distPointUrls.get(distPointUrls.size()-1));
                   break;
                default:
                   der.skipElement();
            }
            // Check to see if there is another sequence to process
            try {
                if(!der.hasNextElement() && der.peekType() == ASN1.SEQUENCE_TYPE) {
                    der.startSequence();
                } else if (!der.hasNextElement() && der.peekType() == 0xa0) {
                    der.startExplicit(0xa0);
                }

            } catch(Exception e) {
                // Just log this error. Likely the Dist points have been parsed, but
                // the end of the cert is failing to parse.
                log.warn("There is an issue parsing the certificate for Distribution Points", e);

            }
        }

        return distPointUrls;
    }

    @Override
    public X509Certificate createServicesTestCertificate(String dn, Date startDate, Date expiryDate, KeyPair keyPair,
            String... certificatePolicyOid) {

        try {
            X500Principal subjectdn = subjectToX500Principle(dn);
            X500Principal issuerdn = subjectToX500Principle(dn);

            ZonedDateTime notValidBefore = ZonedDateTime.ofInstant(startDate.toInstant(), ZoneId.systemDefault());
            ZonedDateTime notValidAfter = ZonedDateTime.ofInstant(expiryDate.toInstant(), ZoneId.systemDefault());

            X509CertificateBuilder cbuilder = new X509CertificateBuilder()
                        .setSubjectDn(subjectdn)
                        .setIssuerDn(issuerdn)

                        .setNotValidBefore(notValidBefore)
                        .setNotValidAfter(notValidAfter)

                        .setSigningKey(keyPair.getPrivate())
                        .setPublicKey(keyPair.getPublic())

                        .addExtension(createPoliciesExtension(certificatePolicyOid))

                        .setSignatureAlgorithmName("SHA256withRSA");

                        return cbuilder.build();
        } catch ( DateTimeException | CertificateException e ) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private X509CertificateExtension createPoliciesExtension(String[] certificatePolicyOid) {

        List<PolicyInformation> policyList = new ArrayList<>();
        for(String policyOid : certificatePolicyOid) {
            policyList.add(new PolicyInformation(policyOid));

        }

        return new CertificatePoliciesExtension(false, policyList);

    }

}

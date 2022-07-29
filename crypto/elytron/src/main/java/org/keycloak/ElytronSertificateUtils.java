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

package org.keycloak.common.util;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import javax.security.auth.x500.X500Principal;

import org.wildfly.security.x500.X500;
import org.wildfly.security.x500.cert.AuthorityKeyIdentifierExtension;
import org.wildfly.security.x500.cert.BasicConstraintsExtension;
import org.wildfly.security.x500.cert.ExtendedKeyUsageExtension;
import org.wildfly.security.x500.cert.KeyUsage;
import org.wildfly.security.x500.cert.KeyUsageExtension;
import org.wildfly.security.x500.cert.SubjectKeyIdentifierExtension;
import org.wildfly.security.x500.cert.X509CertificateBuilder;

/**
 * The Class CertificateUtils provides utility functions for generation of V1 and V3 {@link java.security.cert.X509Certificate}
 *
 * @author <a href="mailto:david.anderson@redhat.com">David Anderson</a>
 * @version $Revision: 3 $
 */
public class ElytronCertificateUtils  extends CertificateUtils {

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
    public static X509Certificate generateV3Certificate(KeyPair keyPair, PrivateKey caPrivateKey,
            X509Certificate caCert,
            String subject) throws Exception {
        try {

            X500Principal subjectdn = subjectToX500Principle(subject);
            X500Principal issuerdn = subjectdn;
            if (caCert != null) {
                issuerdn = caCert.getSubjectX500Principal();
            }

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

                    .setSigningKey(keyPair.getPrivate())
                    .setPublicKey(keyPair.getPublic())

                    .setSerialNumber(serialNumber)

                    .setSignatureAlgorithmName("SHA256withRSA")

                    .setSigningKey(caPrivateKey)

                    // Subject Key Identifier Extension
                    .addExtension(new SubjectKeyIdentifierExtension(keyPair.getPublic().getEncoded()))

                    // Authority Key Identifier
                    .addExtension(new AuthorityKeyIdentifierExtension(keyPair.getPublic().getEncoded(), null, null))

                    // Key Usage
                    .addExtension(
                            new KeyUsageExtension(KeyUsage.digitalSignature, KeyUsage.keyCertSign, KeyUsage.cRLSign))

                    .addExtension(new ExtendedKeyUsageExtension(false, ekuList))

                    // Basic Constraints
                    .addExtension(new BasicConstraintsExtension(true, true, 0));

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
    public static X509Certificate generateV1SelfSignedCertificate(KeyPair caKeyPair, String subject) {
        return generateV1SelfSignedCertificate(caKeyPair, subject, BigInteger.valueOf(System.currentTimeMillis()));
    }

    public static X509Certificate generateV1SelfSignedCertificate(KeyPair caKeyPair, String subject,
            BigInteger serialNumber) {
        try {

            X500Principal subjectdn = subjectToX500Principle(subject);

            ZonedDateTime notBefore = ZonedDateTime.ofInstant(
                    (new Date(System.currentTimeMillis() - 100000)).toInstant(),
                    ZoneId.systemDefault());
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.YEAR, 10);
            Date validityEndDate = new Date(calendar.getTime().getTime());
            ZonedDateTime notAfter = ZonedDateTime.ofInstant(validityEndDate.toInstant(),
                    ZoneId.systemDefault());

            X509CertificateBuilder cbuilder = new X509CertificateBuilder()
                    .setSubjectDn(subjectdn)
                    .setIssuerDn(subjectdn)
                    .setNotValidBefore(notBefore)
                    .setNotValidAfter(notAfter)

                    .setSigningKey(caKeyPair.getPrivate())
                    .setPublicKey(caKeyPair.getPublic())

                    .setSerialNumber(serialNumber)

                    .setSignatureAlgorithmName("SHA256withRSA");

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

}

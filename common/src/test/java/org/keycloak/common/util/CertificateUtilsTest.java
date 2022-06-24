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

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:david.anderson@redhat.com">David Anderson</a>
 */
public class CertificateUtilsTest {

    @Test
    public void testV3SelfSignedCertificate() throws Exception {

        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        String subject = "Test";
        
        X509Certificate caCert = null;
        PrivateKey caPrivateKey = keyPair.getPrivate();

        X509Certificate cert = CertificateUtils.generateV3Certificate(keyPair, caPrivateKey, caCert, subject);

        cert.checkValidity();

    }

    @Test
    public void testV3SignedCertificate() throws Exception {

        KeyPair cakeyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        String casubject = "CATest";
        PrivateKey caPrivateKey = cakeyPair.getPrivate();
        
        X509Certificate caCert = CertificateUtils.generateV3Certificate(cakeyPair, caPrivateKey, null, casubject);

        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        String subject = "Test";

        X509Certificate cert = CertificateUtils.generateV3Certificate(keyPair, caPrivateKey, caCert, subject);

        cert.checkValidity();
        Assert.assertEquals("CN="+casubject,cert.getIssuerDN().getName());
        Assert.assertEquals("CN="+subject, cert.getSubjectDN().getName());

    }

    @Test
    public void testV1SelfSignedCertificate() throws NoSuchAlgorithmException, CertificateExpiredException, CertificateNotYetValidException {

        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        String subject = "CN=Test";

        X509Certificate cert = CertificateUtils.generateV1SelfSignedCertificate(keyPair, subject);

        cert.checkValidity();
    }
    
}

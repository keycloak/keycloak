/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.authentication.x509;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.cert.X509Certificate;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.UserIdentityExtractor;
import org.keycloak.common.util.PemUtils;
import org.keycloak.common.util.StreamUtil;
import org.keycloak.rule.CryptoInitRule;

import static org.junit.Assert.assertEquals;

/** This is not tested in keycloak-core. The subclasses should be created in the crypto modules to make sure it is tested with corresponding modules (bouncycastle VS bouncycastle-fips) */
public abstract class CertificateIdentityExtractorTest {

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    @Test
    public void testExtractsCertInPemFormat() throws Exception {
        X509Certificate x509Certificate = getCertificate();

        String certificatePem = PemUtils.encodeCertificate(x509Certificate);

        //X509AuthenticatorConfigModel config = new X509AuthenticatorConfigModel();
        UserIdentityExtractor extractor = CryptoIntegration.getProvider().getIdentityExtractorProvider().getCertificatePemIdentityExtractor();

        String userIdentity = (String) extractor.extractUserIdentity(new X509Certificate[]{x509Certificate});

        assertEquals(certificatePem, userIdentity);
    }

    @Test
    public void testExtractsCertInSubjectDNFormat() throws Exception {
        X509Certificate x509Certificate = getCertificate();

        UserIdentityExtractor extractor = CryptoIntegration.getProvider().getIdentityExtractorProvider().getX500NameExtractor("CN", certs -> {
            return certs[0].getSubjectX500Principal();
        });
        String userIdentity = (String) extractor.extractUserIdentity(new X509Certificate[]{x509Certificate});
        assertEquals("Test User", userIdentity);
    }

    @Test
    public void testX509SubjectAltName_otherName() throws Exception {
        UserIdentityExtractor extractor = CryptoIntegration.getProvider().getIdentityExtractorProvider().getSubjectAltNameExtractor(0);

        X509Certificate cert = getCertificate();

        Object upn = extractor.extractUserIdentity(new X509Certificate[] { cert});
        Assert.assertEquals("test-user@some-company-domain", upn);
    }


    @Test
    public void testX509SubjectAltName_email() throws Exception {
        UserIdentityExtractor extractor = CryptoIntegration.getProvider().getIdentityExtractorProvider().getSubjectAltNameExtractor(1);

        X509Certificate cert = getCertificate();

        Object upn = extractor.extractUserIdentity(new X509Certificate[] { cert});
        Assert.assertEquals("test@somecompany.com", upn);
    }


    private X509Certificate getCertificate() throws Exception {
        InputStream is = getClass().getResourceAsStream("/certs/UPN-cert.pem");

        String s = StreamUtil.readString(is, Charset.defaultCharset());

        return PemUtils.decodeCertificate(s);
    }
}

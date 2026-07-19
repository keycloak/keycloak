/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.crypto.def.test;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.rule.CryptoInitRule;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1String;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * Regression test for https://github.com/keycloak/keycloak/issues/50177 - the subject used to build the
 * certificate must not be parsed as an RFC2253 DN string, since values such as client IDs may contain
 * characters (e.g. '=', ',') that are not valid there.
 */
public class BCCertificateUtilsProviderTest {

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    @Test
    public void testV1SelfSignedCertificateWithSpecialCharactersInSubject() {
        String subject = "https://example.com/?action=sso_id";
        KeyPair keyPair = KeyUtils.generateRsaKeyPair(2048);

        X509Certificate certificate = CertificateUtils.generateV1SelfSignedCertificate(keyPair, subject);

        Assert.assertEquals(subject, extractCN(certificate));
    }

    @Test
    public void testV3CertificateWithSpecialCharactersInSubject() throws Exception {
        String subject = "https://example.com/?action=sso_id";
        KeyPair caKeyPair = KeyUtils.generateRsaKeyPair(2048);
        X509Certificate caCert = CertificateUtils.generateV1SelfSignedCertificate(caKeyPair, "ca");

        KeyPair keyPair = KeyUtils.generateRsaKeyPair(2048);
        X509Certificate certificate = CertificateUtils.generateV3Certificate(keyPair, caKeyPair.getPrivate(), caCert, subject);

        Assert.assertEquals(subject, extractCN(certificate));
    }

    private static String extractCN(X509Certificate certificate) {
        try {
            ASN1Encodable value = new JcaX509CertificateHolder(certificate).getSubject().getRDNs(BCStyle.CN)[0].getFirst().getValue();
            return ((ASN1String) value).getString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

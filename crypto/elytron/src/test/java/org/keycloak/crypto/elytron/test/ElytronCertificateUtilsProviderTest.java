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

package org.keycloak.crypto.elytron.test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import javax.naming.ldap.LdapName;
import javax.security.auth.x500.X500Principal;

import org.keycloak.crypto.elytron.ElytronCertificateUtilsProvider;

import org.junit.Assert;
import org.junit.Test;

/**
 * Regression test for https://github.com/keycloak/keycloak/issues/50177 - the subject used to build a
 * certificate (e.g. a client ID) must not be parsed as an RFC 2253 DN string, since it may contain
 * reserved characters (e.g. '=', ',', '+', '"').
 */
public class ElytronCertificateUtilsProviderTest {

    private final ElytronCertificateUtilsProvider provider = new ElytronCertificateUtilsProvider();

    @Test
    public void testV1SelfSignedCertificateWithQueryStringSubject() throws Exception {
        assertSubjectRoundTrips("https://example.com/?action=sso_id");
    }

    @Test
    public void testV1SelfSignedCertificateWithCommaInSubject() throws Exception {
        assertSubjectRoundTrips("client,id");
    }

    @Test
    public void testV1SelfSignedCertificateWithPlusAndQuoteInSubject() throws Exception {
        assertSubjectRoundTrips("client+id\"with\"quotes");
    }

    @Test
    public void testV3CertificateWithQueryStringSubject() throws Exception {
        String subject = "https://example.com/?action=sso_id";
        KeyPair caKeyPair = generateRsaKeyPair();
        X509Certificate caCert = provider.generateV1SelfSignedCertificate(caKeyPair, "ca");

        X509Certificate certificate = provider.generateV3Certificate(generateRsaKeyPair(), caKeyPair.getPrivate(), caCert, subject);

        Assert.assertEquals(subject, extractCN(certificate));
    }

    private void assertSubjectRoundTrips(String subject) throws Exception {
        X509Certificate certificate = provider.generateV1SelfSignedCertificate(generateRsaKeyPair(), subject);

        Assert.assertEquals(subject, extractCN(certificate));
    }

    private static KeyPair generateRsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static String extractCN(X509Certificate certificate) throws Exception {
        X500Principal principal = certificate.getSubjectX500Principal();
        LdapName ldapName = new LdapName(principal.getName());
        return (String) ldapName.getRdn(0).toAttributes().get("CN").get();
    }
}

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
package org.keycloak.crypto.def.test;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.crypto.def.StreamingCrlParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class StreamingCrlParserTest {

    @Test
    public void parseCrlWithRevokedCerts() throws IOException, CertificateException {
        X509Certificate issuer = getCaCert();

        StreamingCrlParser streamingCrlParser = new StreamingCrlParser(Paths.get("src/test/resources/crls/revoked.crl").toFile());

        Assert.assertEquals("version", 1, streamingCrlParser.getVersion());
        Assert.assertEquals("number of certs", 1, streamingCrlParser.getRevokedCertificates().size());
        boolean signatureVerified = true;
        try {
            streamingCrlParser.verify(issuer.getPublicKey());
        } catch (Exception e) {
            signatureVerified = false;
        }

        Assert.assertTrue(signatureVerified);
    }

    @Test
    public void parseCrlWithoutRevokedCerts() throws CertificateException, IOException {
        X509Certificate issuer = getCaCert();

        StreamingCrlParser streamingCrlParser = new StreamingCrlParser(Paths.get("src/test/resources/crls/no-revoked.crl").toFile());

        Assert.assertEquals("version", 1, streamingCrlParser.getVersion());
        Assert.assertEquals("number of certs", 0, streamingCrlParser.getRevokedCertificates().size());
        boolean signatureVerified = true;
        try {
            streamingCrlParser.verify(issuer.getPublicKey());
        } catch (Exception e) {
            signatureVerified = false;
        }

        Assert.assertTrue(signatureVerified);
    }

    private X509Certificate getCaCert() throws IOException, CertificateException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) certificateFactory.generateCertificate(Files.newInputStream(Paths.get("src/test/resources/crls/ca.crt")));
    }
}

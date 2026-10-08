/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.x509;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.keycloak.common.util.BouncyIntegration;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

final class CrlGenerator {

    private static final long ONE_DAY_MILLIS = 86_400_000L;

    private CrlGenerator() {
    }

    static byte[] generateCrl(Date nextUpdate) throws Exception {
        KeyPair keyPair = KeyUtils.generateRsaKeyPair(2048);
        X509Certificate caCert = CertificateUtils.generateV1SelfSignedCertificate(keyPair, "KC CRL Test CA");
        Date thisUpdate = new Date(nextUpdate.getTime() - ONE_DAY_MILLIS);
        X509v2CRLBuilder builder = new X509v2CRLBuilder(X500Name.getInstance(caCert.getSubjectX500Principal().getEncoded()), thisUpdate);
        builder.setNextUpdate(nextUpdate);
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").setProvider(BouncyIntegration.PROVIDER).build(keyPair.getPrivate());
        return builder.build(signer).getEncoded();
    }

    static byte[] generateValidCrl() throws Exception {
        return generateCrl(new Date(System.currentTimeMillis() + ONE_DAY_MILLIS));
    }

    static byte[] generateStaleCrl() throws Exception {
        return generateCrl(new Date(System.currentTimeMillis() - ONE_DAY_MILLIS));
    }
}

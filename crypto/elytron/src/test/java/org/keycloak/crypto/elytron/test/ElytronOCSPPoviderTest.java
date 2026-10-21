/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.crypto.elytron.test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import javax.security.auth.x500.X500Principal;

import org.keycloak.crypto.elytron.ElytronOCSPProvider;

import org.junit.Test;
import org.wildfly.security.x500.GeneralName;
import org.wildfly.security.x500.cert.AccessDescription;
import org.wildfly.security.x500.cert.AuthorityInformationAccessExtension;
import org.wildfly.security.x500.cert.X509CertificateBuilder;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:david.anderson@redhat.com">David Anderson</a>
 */
public class ElytronOCSPPoviderTest  extends ElytronOCSPProvider {

    @Test
    public void responderURITest() throws NoSuchAlgorithmException, CertificateException, URISyntaxException, KeyStoreException, IOException, CertPathValidatorException, InvalidAlgorithmParameterException {
        X509Certificate cert = createCert();

        List<String> luri = getResponderURIs(cert);

        assertEquals(1, luri.size());
        assertEquals("http://test.localhost/check", luri.get(0));

    }

    private X509Certificate createCert() throws NoSuchAlgorithmException, CertificateException {
        X500Principal dn = new X500Principal("CN=testuser,OU=UNIT,O=TST");
        
        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").genKeyPair();
        List<AccessDescription> accessDescriptions = new ArrayList<>();
        String accessMethodId = "1.3.6.1.5.5.7.48.1";
        GeneralName accessLocation = new GeneralName.URIName("http://test.localhost/check");
        AccessDescription adesc = new AccessDescription(accessMethodId, accessLocation);
        accessDescriptions.add(adesc);


        X509CertificateBuilder cbuilder = new X509CertificateBuilder()
        .setSubjectDn(dn)
        .setIssuerDn(dn)
        
        .setSigningKey(keyPair.getPrivate())
        .setPublicKey(keyPair.getPublic())

        .addExtension(new AuthorityInformationAccessExtension(accessDescriptions))
        
        .setSignatureAlgorithmName("SHA256withRSA");

        return cbuilder.build();
    }
    
}

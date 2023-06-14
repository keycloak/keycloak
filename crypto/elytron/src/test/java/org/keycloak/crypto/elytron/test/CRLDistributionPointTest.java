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

import static org.junit.Assert.assertArrayEquals;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.security.auth.x500.X500Principal;

import org.junit.Test;
import org.keycloak.crypto.elytron.ElytronCertificateUtils;
import org.wildfly.security.x500.GeneralName;
import org.wildfly.security.x500.cert.CRLDistributionPoint;
import org.wildfly.security.x500.cert.CRLDistributionPoint.DistributionPointName;
import org.wildfly.security.x500.cert.CRLDistributionPoint.FullNameDistributionPointName;
import org.wildfly.security.x500.cert.CRLDistributionPointsExtension;
import org.wildfly.security.x500.cert.X509CertificateBuilder;

/**
 * @author <a href="mailto:david.anderson@redhat.com">David Anderson</a>
 */
public class CRLDistributionPointTest {

     @Test
     public void getCrlDistPoint() throws CertificateException, NoSuchAlgorithmException, IOException {

         X509Certificate cert = createCRLcert();
         List<String> expect = new ArrayList<>();
         expect.add("http://crl.test.com");
         

         ElytronCertificateUtils bcutil = new ElytronCertificateUtils();
          List<String> crldp = bcutil.getCRLDistributionPoints(cert);

          assertArrayEquals(expect.toArray(), crldp.toArray());
         
     }

     private X509Certificate createCRLcert() throws CertificateException, NoSuchAlgorithmException {

        X500Principal dn = new X500Principal("CN=testuser,OU=UNIT,O=TST");
        List<CRLDistributionPoint> distributionPoints = new ArrayList<>();

        List<GeneralName> fullName = new ArrayList<>();
        fullName.add(new GeneralName.URIName("http://crl.test.com"));
        DistributionPointName distributionPoint = new FullNameDistributionPointName(fullName);
        CRLDistributionPoint arg0 = new CRLDistributionPoint(distributionPoint, null, null);
        distributionPoints.add(arg0);
        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").genKeyPair();
        X509CertificateBuilder cbuilder = new X509CertificateBuilder()
        .setSubjectDn(dn)
        .setIssuerDn(dn)
        
        .setSigningKey(keyPair.getPrivate())
        .setPublicKey(keyPair.getPublic())

        .addExtension(new CRLDistributionPointsExtension(false, distributionPoints))
        
        .setSignatureAlgorithmName("SHA256withRSA");

        return cbuilder.build();
     }


    
}

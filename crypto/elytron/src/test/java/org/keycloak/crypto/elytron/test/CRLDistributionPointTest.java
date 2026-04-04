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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import javax.security.auth.x500.X500Principal;

import org.keycloak.crypto.elytron.ElytronCertificateUtilsProvider;

import org.junit.Test;
import org.wildfly.security.x500.GeneralName;
import org.wildfly.security.x500.cert.CRLDistributionPoint;
import org.wildfly.security.x500.cert.CRLDistributionPoint.DistributionPointName;
import org.wildfly.security.x500.cert.CRLDistributionPoint.FullNameDistributionPointName;
import org.wildfly.security.x500.cert.CRLDistributionPointsExtension;
import org.wildfly.security.x500.cert.X509CertificateBuilder;

import static org.junit.Assert.assertArrayEquals;

/**
 * @author <a href="mailto:david.anderson@redhat.com">David Anderson</a>
 */
public class CRLDistributionPointTest {

     @Test
     public void getCrlDistPoint() throws CertificateException, NoSuchAlgorithmException, IOException {

         X509Certificate cert = createCRLcert(1,1);
         List<String> expect = new ArrayList<>();
         expect.add("http://crl0.test0.com");
         

         ElytronCertificateUtilsProvider bcutil = new ElytronCertificateUtilsProvider();
          List<String> crldp = bcutil.getCRLDistributionPoints(cert);

          assertArrayEquals(expect.toArray(), crldp.toArray());
         
     }

     @Test
     public void getCrlDistPointMultiNames() throws CertificateException, NoSuchAlgorithmException, IOException {

         X509Certificate cert = createCRLcert(1,2);
         List<String> expect = new ArrayList<>();
         expect.add("http://crl0.test0.com");
         expect.add("http://crl0.test1.com");
         
         ElytronCertificateUtilsProvider bcutil = new ElytronCertificateUtilsProvider();
          List<String> crldp = bcutil.getCRLDistributionPoints(cert);

          assertArrayEquals(expect.toArray(), crldp.toArray());
         
     }

     @Test
     public void getMultiCrlDistPointMultiNames() throws CertificateException, NoSuchAlgorithmException, IOException {

         X509Certificate cert = createCRLcert(2,2);
         List<String> expect = new ArrayList<>();
         expect.add("http://crl0.test0.com");
         expect.add("http://crl0.test1.com");
         expect.add("http://crl1.test0.com");
         expect.add("http://crl1.test1.com");
         
         ElytronCertificateUtilsProvider bcutil = new ElytronCertificateUtilsProvider();
          List<String> crldp = bcutil.getCRLDistributionPoints(cert);

          assertArrayEquals(expect.toArray(), crldp.toArray());
         
     }

     @Test
     public void revokedCertCRLDistTest() throws CertificateException, IOException {
          X509Certificate cert = revokedCert();
          List<String> expect = new ArrayList<>();
         expect.add("http://localhost:8889/empty.crl");
         expect.add("http://localhost:8889/intermediate-ca.crl");
         
         ElytronCertificateUtilsProvider bcutil = new ElytronCertificateUtilsProvider();
          List<String> crldp = bcutil.getCRLDistributionPoints(cert);

          assertArrayEquals(expect.toArray(), crldp.toArray());
          
     }

     private X509Certificate createCRLcert(int crldistcount, int namecount) throws CertificateException, NoSuchAlgorithmException {

        X500Principal dn = new X500Principal("CN=testuser,OU=UNIT,O=TST");
        List<CRLDistributionPoint> distributionPoints = new ArrayList<>();

        for(int x = 0; x<crldistcount;x++) {
             List<GeneralName> fullName = new ArrayList<>();
             for(int y = 0; y<namecount; y++) {
                  fullName.add(new GeneralName.URIName("http://crl"+x+".test"+y+".com"));
             }
             DistributionPointName distributionPoint = new FullNameDistributionPointName(fullName);
             CRLDistributionPoint arg0 = new CRLDistributionPoint(distributionPoint, null, null);
             distributionPoints.add(arg0);

        }
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

     X509Certificate revokedCert() throws CertificateException {
          String certStr = "-----BEGIN CERTIFICATE-----\n" + 
            "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"
            + "\n-----END CERTIFICATE-----";
          CertificateFactory cf = CertificateFactory.getInstance("X.509");

          return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certStr.getBytes()));

     }


    
}

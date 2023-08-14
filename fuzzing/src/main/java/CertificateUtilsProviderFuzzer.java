/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.io.ByteArrayInputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;
import org.keycloak.common.crypto.CertificateUtilsProvider;
import org.keycloak.crypto.def.BCCertificateUtilsProvider;
import org.keycloak.crypto.elytron.ElytronCertificateUtils;
import org.keycloak.crypto.fips.BCFIPSCertificateUtilsProvider;

/**
 * This fuzzer targets the methods in different
 * Certificate Utils Provider implementation classes
 * in the crypto package.
 */
public class CertificateUtilsProviderFuzzer {
  private static Boolean initSuccess;
  private static CertificateFactory cf;
  private static KeyPair keyPair;

  public static void fuzzerInitialize() {
    try {
      // Initialize certificate factory
      cf = CertificateFactory.getInstance("X.509");

      // Initialize key pair
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      keyPair = generator.generateKeyPair();
    } catch (CertificateException | NoSuchAlgorithmException e) {
      // Directly exit if initialisation fails
      throw new RuntimeException(e);
    }
  }

  public static void fuzzerTestOneInput(FuzzedDataProvider data) {
    // Initialise base certificate related object
    CertificateUtilsProvider provider = null;
    X509Certificate cert = null;

    try {
      // Randomly create a certificate utils provider instance
      switch (data.consumeInt(1, 3)) {
        case 1:
          provider = new BCCertificateUtilsProvider();
          break;
        case 2:
          provider = new ElytronCertificateUtils();
          break;
        case 3:
          provider = new BCFIPSCertificateUtilsProvider();
          break;
      }

      // Randomly choose which method to invoke
      Integer choice = data.consumeInt(1, 5);
      switch (choice) {
        case 1:
          cert = (X509Certificate) cf.generateCertificate(
              new ByteArrayInputStream(data.consumeBytes(data.remainingBytes() / 2)));
          provider.generateV3Certificate(
              keyPair, keyPair.getPrivate(), cert, data.consumeRemainingAsString());
          break;
        case 2:
          provider.generateV1SelfSignedCertificate(keyPair, data.consumeRemainingAsString());
          break;
        case 3:
          cert = (X509Certificate) cf.generateCertificate(
              new ByteArrayInputStream(data.consumeBytes(data.remainingBytes() / 2)));
          provider.getCertificatePolicyList(cert);
          break;
        case 4:
          cert = (X509Certificate) cf.generateCertificate(
              new ByteArrayInputStream(data.consumeBytes(data.remainingBytes() / 2)));
          provider.getCRLDistributionPoints(cert);
          break;
        case 5:
          Date startDate = new Date(data.consumeLong());
          Date expiryDate = new Date(data.consumeLong());
          provider.createServicesTestCertificate(data.consumeString(data.remainingBytes() / 2),
              startDate, expiryDate, keyPair, data.consumeRemainingAsString());
          break;
      }
    } catch (Exception | NoSuchMethodError | ExceptionInInitializerError | NoClassDefFoundError e) {
      // Known exception and errors directly thrown from the above methods
      // Some methods above capture all exception and throw the general
      // Exception explicitly, thus it need to be catch.
      // ExceptionInInitializerError and NoClassDefFoundError is caught to
      // ensure BouncyIntegration fail because of missing providers won't stop
      // the fuzzer.
    }
  }
}

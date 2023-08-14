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
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.EnumSet;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.DerUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.common.util.PemUtils;

/**
 * This fuzzer targets the methods in different crypto related
 * util classes in the common package.
 */
public class CommonCryptoUtilsFuzzer {
  public static void fuzzerTestOneInput(FuzzedDataProvider data) {
    X509Certificate cert = null;
    KeyPair keyPair = null;

    try {
      CertificateFactory cf = CertificateFactory.getInstance("X.509");

      // Randomly choose which utils method to invoke
      Integer choice = data.consumeInt(1, 22);
      switch (choice) {
        case 1:
          try {
            cert = (X509Certificate) cf.generateCertificate(
                new ByteArrayInputStream(data.consumeBytes(data.remainingBytes() / 2)));
            keyPair = KeyUtils.generateRsaKeyPair(2048);
            CertificateUtils.generateV3Certificate(
                keyPair, keyPair.getPrivate(), cert, data.consumeRemainingAsString());
          } catch (Exception e) {
            // Known exception
          }
          break;
        case 2:
          keyPair = KeyUtils.generateRsaKeyPair(2048);
          CertificateUtils.generateV1SelfSignedCertificate(
              keyPair, data.consumeRemainingAsString());
          break;
        case 3:
          keyPair = KeyUtils.generateRsaKeyPair(2048);
          BigInteger serial = BigInteger.valueOf(data.consumeLong());
          CertificateUtils.generateV1SelfSignedCertificate(
              keyPair, data.consumeRemainingAsString(), serial);
          break;
        case 4:
          try {
            DerUtils.decodePrivateKey(new ByteArrayInputStream(data.consumeRemainingAsBytes()));
          } catch (Exception e) {
            // Known exception
          }
          break;
        case 5:
          DerUtils.decodePrivateKey(data.consumeRemainingAsBytes());
          break;
        case 6:
          DerUtils.decodePublicKey(
              data.consumeBytes(data.remainingBytes() / 2), data.consumeRemainingAsString());
          break;
        case 7:
          try {
            DerUtils.decodeCertificate(new ByteArrayInputStream(data.consumeRemainingAsBytes()));
          } catch (Exception e) {
            // Known exception
          }
          break;
        case 8:
          keyPair = KeyUtils.generateRsaKeyPair(2048);
          KeyUtils.createKeyId(keyPair.getPrivate());
          break;
        case 9:
          try {
            KeystoreUtil.loadKeyStore(
                data.consumeString(data.remainingBytes() / 2), data.consumeRemainingAsString());
          } catch (Exception e) {
            // Known exception
          }
          break;
        case 10:
          KeystoreUtil.loadKeyPairFromKeystore(data.consumeString(data.remainingBytes() / 2),
              data.consumeString(data.remainingBytes() / 2),
              data.consumeString(data.remainingBytes() / 2),
              data.consumeString(data.remainingBytes() / 2),
              data.pickValue(EnumSet.allOf(KeystoreUtil.KeystoreFormat.class)));
          break;
        case 11:
          KeystoreUtil.getKeystoreType(data.consumeString(data.remainingBytes() / 2),
              data.consumeString(data.remainingBytes() / 2), data.consumeRemainingAsString());
          break;
        case 12:
          PemUtils.decodeCertificate(data.consumeRemainingAsString());
          break;
        case 13:
          PemUtils.decodePublicKey(data.consumeRemainingAsString());
          break;
        case 14:
          PemUtils.decodePublicKey(
              data.consumeString(data.remainingBytes() / 2), data.consumeRemainingAsString());
          break;
        case 15:
          PemUtils.decodePrivateKey(data.consumeRemainingAsString());
          break;
        case 16:
          keyPair = KeyUtils.generateRsaKeyPair(2048);
          PemUtils.encodeKey(keyPair.getPrivate());
          break;
        case 17:
          cert = (X509Certificate) cf.generateCertificate(
              new ByteArrayInputStream(data.consumeBytes(data.remainingBytes() / 2)));
          PemUtils.encodeCertificate(cert);
          break;
        case 18:
          PemUtils.pemToDer(data.consumeRemainingAsString());
          break;
        case 19:
          PemUtils.removeBeginEnd(data.consumeRemainingAsString());
          break;
        case 20:
          PemUtils.addPrivateKeyBeginEnd(data.consumeRemainingAsString());
          break;
        case 21:
          PemUtils.addRsaPrivateKeyBeginEnd(data.consumeRemainingAsString());
          break;
        case 22:
          String encoding = data.consumeString(data.remainingBytes() / 2);
          String[] chain = {data.consumeRemainingAsString()};
          PemUtils.generateThumbprint(chain, encoding);
          break;
      }
    } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchProviderException
        | CertificateException | RuntimeException e) {
      // Known exception
    }
  }
}

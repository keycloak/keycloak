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
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.util.JWKSUtils;

/**
  This fuzzer targets the JWKBuilder and JWKSUtils class.
  It generate a random set of JWK keys for creating
  a JSONWebKeySet object. It then call random methods
  in JWKSUtils on that random JWK key set with random
  JWK use choice.
  */
public class JwksUtilsFuzzer {
  public static void fuzzerTestOneInput(FuzzedDataProvider data) {
    try {
      // Prepare the set of algorithm and key use for random choice
      String[] algorithm = {Algorithm.HS256, Algorithm.HS384, Algorithm.HS512, Algorithm.RS256,
          Algorithm.RS384, Algorithm.RS512, Algorithm.ES256, Algorithm.ES384, Algorithm.ES512,
          Algorithm.PS256, Algorithm.PS384, Algorithm.PS512, Algorithm.RSA1_5, Algorithm.RSA_OAEP,
          Algorithm.RSA_OAEP_256, Algorithm.AES};
      EnumSet<JWK.Use> jwkUse = EnumSet.allOf(JWK.Use.class);
      EnumSet<KeyUse> keyUse = EnumSet.allOf(KeyUse.class);

      // Initialise the JWKBuilder and a JWK array with random size
      JWKBuilder builder = JWKBuilder.create();
      boolean[] choices = data.consumeBooleans(data.consumeInt(1, 10));
      byte[] byteArray = data.consumeBytes(data.remainingBytes() / 2);
      JWK[] keys = new JWK[choices.length];

      // Generate a set of JWK keys with random algorithm
      for (int i = 0; i < choices.length; i++) {
        builder.algorithm(data.pickValue(algorithm));
        if (choices[i]) {
          // Generate random X509 certificate
          CertificateFactory cf = CertificateFactory.getInstance("X.509");
          X509Certificate cert =
              (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(byteArray));
          List<X509Certificate> certList = Arrays.asList(cert);

          // Generate random RSA keypair
          KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
          generator.initialize(2048);
          KeyPair keyPair = generator.generateKeyPair();

          // Generate JWK key with the random RSA public key, random X509 certificate and random
          // choice of key use
          keys[i] = builder.rsa(keyPair.getPublic(), certList, data.pickValue(keyUse));
        } else {
          // Generate random EC keypair
          KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
          KeyPair keyPair = generator.generateKeyPair();

          // Generate JWK key with the random EC public key and random choice of key use
          keys[i] = builder.ec(keyPair.getPublic(), data.pickValue(keyUse));
        }
      }

      // Create JSONWebKeySet object with the random set of JWK key
      JSONWebKeySet keySet = new JSONWebKeySet();
      keySet.setKeys(keys);

      // Fuzz method in JWKSUtils with the random JWK key set and JWK Use choice
      if (data.consumeBoolean()) {
        JWKSUtils.getKeyWrappersForUse(keySet, data.pickValue(jwkUse));
      } else {
        JWKSUtils.getKeyForUse(keySet, data.pickValue(jwkUse));
      }
    } catch (CertificateException | NoSuchAlgorithmException e) {
      // Known exception
    }
  }
}

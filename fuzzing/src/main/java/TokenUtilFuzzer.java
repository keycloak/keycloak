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
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import javax.crypto.KeyGenerator;
import org.keycloak.jose.jwe.JWEConstants;
import org.keycloak.jose.jwe.JWEException;
import org.keycloak.jose.jwe.alg.DirectAlgorithmProvider;
import org.keycloak.jose.jwe.alg.JWEAlgorithmProvider;
import org.keycloak.jose.jwe.enc.AesCbcHmacShaJWEEncryptionProvider;
import org.keycloak.jose.jwe.enc.AesGcmJWEEncryptionProvider;
import org.keycloak.jose.jwe.enc.JWEEncryptionProvider;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.util.TokenUtil;

/**
  This fuzzer targets the methods in TokenUtil class.
  It passes random string for choosing algorithm,
  key creation and method call.
  */
public class TokenUtilFuzzer {
  // Set up a list of valid algorithm for the JWE object
  private static String[] alg = {JWEConstants.DIRECT, JWEConstants.A128KW, JWEConstants.RSA1_5,
      JWEConstants.RSA_OAEP, JWEConstants.RSA_OAEP_256};

  // Set up a list of valid encryption / compression
  // algorithm for the JWE object
  private static String[] enc = {JWEConstants.A128CBC_HS256, JWEConstants.A192CBC_HS384,
      JWEConstants.A256CBC_HS512, JWEConstants.A128GCM, JWEConstants.A192GCM, JWEConstants.A256GCM};

  public static void fuzzerTestOneInput(FuzzedDataProvider data) {
    try {
      // Create base object for method call
      KeyGenerator generator = null;
      Key aesKey = null;
      Key hmacKey = null;

      // Randomly choose which method to invoke
      Integer choice = data.consumeInt(1, 6);
      switch (choice) {
        case 1:
          // Call isOfflineToken method with random string
          TokenUtil.isOfflineToken(data.consumeRemainingAsString());
          break;
        case 2:
          // Call getRefreshToken method with random string
          TokenUtil.getRefreshToken(data.consumeRemainingAsBytes());
          break;
        case 3:
          // Generate AES key for method call
          generator = KeyGenerator.getInstance("AES");
          generator.init(128);
          aesKey = generator.generateKey();

          // Generate Hmac key for method call
          generator = KeyGenerator.getInstance("HmacSHA256");
          hmacKey = generator.generateKey();

          // Call jweDirectEncode method with the created keys and random byte array
          TokenUtil.jweDirectEncode(aesKey, hmacKey, data.consumeRemainingAsBytes());
          break;
        case 4:
          // Generate AES key for method call
          generator = KeyGenerator.getInstance("AES");
          generator.init(128);
          aesKey = generator.generateKey();

          // Generate Hmac key for method call
          generator = KeyGenerator.getInstance("HmacSHA256");
          hmacKey = generator.generateKey();

          // Call jweDirectVerifyAndDecode method with the created keys and random byte array
          TokenUtil.jweDirectVerifyAndDecode(aesKey, hmacKey, data.consumeRemainingAsString());
          break;
        case 5:
          // Generate AES key for method call
          generator = KeyGenerator.getInstance("AES");
          generator.init(128);
          aesKey = generator.generateKey();

          // Call jweKeyEncryptionVerifyAndDecode method with the created key and random string
          TokenUtil.jweKeyEncryptionVerifyAndDecode(aesKey, data.consumeRemainingAsString());
          break;
        case 6:
          // Generate AES key for method call
          generator = KeyGenerator.getInstance("AES");
          generator.init(128);
          aesKey = generator.generateKey();

          // Initialize the jweAlgorithmProvider object for method call
          JWEAlgorithmProvider jweAlgorithmProvider = new DirectAlgorithmProvider();

          // Randomly initialize a JWEEncryptionProvider object with randomly chosen encryption
          // algorithm
          JWEEncryptionProvider jweEncryptionProvider;
          if (data.consumeBoolean()) {
            jweEncryptionProvider =
                new AesCbcHmacShaJWEEncryptionProvider(data.pickValue(TokenUtilFuzzer.enc));
          } else {
            jweEncryptionProvider =
                new AesGcmJWEEncryptionProvider(data.pickValue(TokenUtilFuzzer.enc));
          }

          // Randomly choose which method to invoke
          if (data.consumeBoolean()) {
            // Choose random algorithm and initialize a key id with random string
            String algAlgorithm = data.pickValue(TokenUtilFuzzer.alg);
            String encAlgorithm = data.pickValue(TokenUtilFuzzer.enc);
            String kid = data.consumeString(data.remainingBytes() / 2);

            // Call jweKeyEncryptionEncode method with created object and random byte array
            TokenUtil.jweKeyEncryptionEncode(aesKey, data.consumeRemainingAsBytes(), algAlgorithm,
                encAlgorithm, kid, jweAlgorithmProvider, jweEncryptionProvider);
          } else {
            // Call jweKeyEncryptionVerifyAndDecode method with created object and random byte array
            TokenUtil.jweKeyEncryptionVerifyAndDecode(aesKey, data.consumeRemainingAsString(),
                jweAlgorithmProvider, jweEncryptionProvider);
          }
          break;
      }
    } catch (JWSInputException | JWEException | NoSuchAlgorithmException e) {
      // Known exception
    }
  }
}

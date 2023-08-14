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
import org.keycloak.jose.jwe.JWE;
import org.keycloak.jose.jwe.JWEConstants;
import org.keycloak.jose.jwe.JWEException;
import org.keycloak.jose.jwe.JWEHeader;

/**
  This fuzzer targets the encodeJwe method of the JWE class.
  It creates and initialize a JWE object with random string
  or JWEHeader instance for further encoding process using
  the stored JWEHeader object.
  */
public class JweFuzzer {
  public static void fuzzerTestOneInput(FuzzedDataProvider data) {
    try {
      // Determine how to create and initialize the JWE object
      Boolean choice = data.consumeBoolean();

      // Set up a list of valid algorithm for the JWE object
      String[] alg = {JWEConstants.DIRECT, JWEConstants.A128KW, JWEConstants.RSA1_5,
          JWEConstants.RSA_OAEP, JWEConstants.RSA_OAEP_256};

      // Set up a list of valid encryption / compression
      // algorithm for the JWE object
      String[] enc = {JWEConstants.A128CBC_HS256, JWEConstants.A192CBC_HS384,
          JWEConstants.A256CBC_HS512, JWEConstants.A128GCM, JWEConstants.A192GCM,
          JWEConstants.A256GCM};

      // Creates and initializes a JWEHeader object with random
      // pick of algorithms and encryption / compression algorithms
      JWEHeader header =
          new JWEHeader(data.pickValue(alg), data.pickValue(enc), data.pickValue(enc));
      JWE jwe;

      if (choice) {
        // Creates and initializes a JWE object with random
        // JWEHeader string
        jwe = new JWE(data.consumeRemainingAsString());
      } else {
        // Creates and initializes a JWE object with the
        // JWEHeader object created above
        jwe = new JWE();
        jwe.header(header);
      }

      // Call the encodeJwe method which performs some
      // operations depennding on its JWEHeader configurations
      jwe.encodeJwe();
    } catch (RuntimeException | JWEException e) {
      // Known exception
    }
  }
}

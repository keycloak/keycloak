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
import java.io.IOException;
import org.keycloak.common.crypto.ECDSACryptoProvider;
import org.keycloak.crypto.def.BCECDSACryptoProvider;
import org.keycloak.crypto.elytron.ElytronECDSACryptoProvider;
import org.keycloak.crypto.fips.BCFIPSECDSACryptoProvider;

/**
 * This fuzzer targets the methods in different
 * ECDSA Crypto Provider implementation classes
 * in the crypto package.
 */
public class ECDSACryptoProviderFuzzer {
  private static BCECDSACryptoProvider becProvider;
  private static ElytronECDSACryptoProvider eecProvider;
  private static BCFIPSECDSACryptoProvider bfecProvider;

  public static void fuzzerInitialize() {
    // Initialize base providers
    becProvider = new BCECDSACryptoProvider();
    eecProvider = new ElytronECDSACryptoProvider();
    bfecProvider = new BCFIPSECDSACryptoProvider();
  }

  public static void fuzzerTestOneInput(FuzzedDataProvider data) {
    try {
      // Randomly create an instance of ECDSACryptoProvider
      ECDSACryptoProvider provider = null;
      switch (data.consumeInt(1, 3)) {
        case 1:
          provider = becProvider;
          break;
        case 2:
          provider = eecProvider;
          break;
        case 3:
          provider = bfecProvider;
          break;
      }

      // Randomly call method from the ECDSACryptoProvider instance
      byte[] bytes = data.consumeRemainingAsBytes();
      if (data.consumeBoolean()) {
        provider.concatenatedRSToASN1DER(bytes, bytes.length);
      } else {
        provider.asn1derToConcatenatedRS(bytes, bytes.length);
      }
    } catch (IOException | IllegalArgumentException e) {
      // Known exception
    }
  }
}

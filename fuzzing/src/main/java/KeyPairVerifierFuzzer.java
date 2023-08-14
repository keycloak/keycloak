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
import org.keycloak.KeyPairVerifier;
import org.keycloak.common.VerificationException;

/**
  This fuzzer targets the verify method of the KeyPairVerifier
  class. It calls the verify methods with two random string.
  */
public class KeyPairVerifierFuzzer {
  public static void fuzzerTestOneInput(FuzzedDataProvider data) {
    try {
      // Retrieves two random strings from the FuzzedDataProvider object
      String str1 = data.consumeString(data.remainingBytes() / 2);
      String str2 = data.consumeRemainingAsString();

      // Call the verify method of KeyPairVerifier class
      // with the retrieved random string
      KeyPairVerifier.verify(str1, str2);
    } catch (VerificationException e) {
      // Known exception
    }
  }
}

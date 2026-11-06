/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.crypto;

import java.util.Set;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;

public final class MLDSAUtils {

    private static final Set<String> ALGORITHMS = Set.of(
            Algorithm.ML_DSA_44, Algorithm.ML_DSA_65, Algorithm.ML_DSA_87);

    private MLDSAUtils() {
    }

    public static boolean isSignatureSupported(String algorithm) {
        if (!ALGORITHMS.contains(algorithm) || !CryptoIntegration.isInitialised()) {
            return false;
        }

        try {
            CryptoProvider provider = CryptoIntegration.getProvider();
            return provider.getSignature(algorithm) != null && provider.getKeyFactory(algorithm) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isKeyGenerationSupported(String algorithm) {
        if (!isSignatureSupported(algorithm)) {
            return false;
        }

        try {
            return CryptoIntegration.getProvider().getKeyPairGen(algorithm) != null;
        } catch (Exception e) {
            return false;
        }
    }
}

/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.jgroups.certificates;

import java.security.KeyPair;
import java.util.Locale;
import java.util.Set;

import org.keycloak.common.util.KeyUtils;

/**
 * Describes the key pair of an automatically generated certificate.
 */
public sealed interface KeySpec {

    KeyPair generate();

    String description();

    /**
     * @param keySize RSA modulus length in bits
     */
    record Rsa(int keySize) implements KeySpec {

        public static final int DEFAULT_KEY_SIZE = 3072;
        private static final int MIN_KEY_SIZE = 2048;

        public Rsa {
            if (keySize < MIN_KEY_SIZE) {
                throw new IllegalArgumentException("RSA key size must be at least %d but was %d.".formatted(MIN_KEY_SIZE, keySize));
            }
        }

        @Override
        public KeyPair generate() {
            return KeyUtils.generateRsaKeyPair(keySize);
        }

        @Override
        public String description() {
            return "RSA-" + keySize;
        }
    }

    /**
     * @param curve the JDK name of the named curve
     */
    record Ecdsa(String curve) implements KeySpec {

        public static final String DEFAULT_CURVE = "secp256r1";
        private static final Set<String> SUPPORTED_CURVES = Set.of("secp256r1", "secp384r1", "secp521r1");

        public Ecdsa {
            if (!SUPPORTED_CURVES.contains(curve)) {
                throw new IllegalArgumentException("Unsupported ECDSA curve '%s', supported: %s.".formatted(curve, new java.util.TreeSet<>(SUPPORTED_CURVES)));
            }
        }

        @Override
        public KeyPair generate() {
            return KeyUtils.generateEcKeyPair(curve);
        }

        @Override
        public String description() {
            return "ECDSA-" + curve;
        }
    }

    static KeySpec of(String algorithm, Integer keySize, String curve) {
        return switch (algorithm.toUpperCase(Locale.ROOT)) {
            case "RSA" -> new Rsa(keySize == null ? Rsa.DEFAULT_KEY_SIZE : keySize);
            case "ECDSA" -> new Ecdsa(curve == null ? Ecdsa.DEFAULT_CURVE : curve);
            default -> throw new IllegalArgumentException("Unsupported algorithm '%s', supported: RSA, ECDSA.".formatted(algorithm));
        };
    }
}

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

package org.keycloak.authentication.authenticators.util;

import java.nio.charset.StandardCharsets;
import java.util.function.IntSupplier;

import org.keycloak.common.util.SecretGenerator;
import org.keycloak.jose.jws.crypto.HashUtils;

public final class PhoneOtpUtils {

    private PhoneOtpUtils() {
    }

    public static String generateCode(int length) {
        return generateCode(length, () -> SecretGenerator.nextInt(getBound(length)));
    }

    static String generateCode(int length, IntSupplier supplier) {
        int safeLength = length <= 0 ? 1 : length;
        int bound = getBound(safeLength);
        int value = Math.floorMod(supplier.getAsInt(), bound);
        return String.format("%0" + safeLength + "d", value);
    }

    public static String hashCode(String code, String salt) {
        return HashUtils.sha256UrlEncodedHash(code + ":" + salt, StandardCharsets.UTF_8);
    }

    public static boolean isExpired(int createdAt, int ttlSeconds, int now) {
        return now - createdAt > ttlSeconds;
    }

    public static Long cooldownRemaining(int lastSentAt, int cooldownSeconds, int now) {
        if (cooldownSeconds <= 0) {
            return null;
        }
        int elapsed = now - lastSentAt;
        if (elapsed >= cooldownSeconds) {
            return null;
        }
        return (long) (cooldownSeconds - elapsed);
    }

    private static int getBound(int length) {
        return (int) Math.pow(10, length);
    }
}

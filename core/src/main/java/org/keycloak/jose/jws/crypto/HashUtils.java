/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.jose.jws.crypto;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

import org.keycloak.common.util.Base64Url;
import org.keycloak.crypto.HashException;
import org.keycloak.crypto.JavaAlgorithm;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class HashUtils {

    // See:
    // - "at_hash" and "c_hash" in OIDC specification (full = false)
    // - "ath" in DPoP specification (full = true)
    public static String accessTokenHash(String jwtAlgorithmName, String input, boolean full) {
        return accessTokenHash(jwtAlgorithmName, null, input, full);
    }

    public static String accessTokenHash(String jwtAlgorithmName, String curve, String input, boolean full) {
        byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
        String javaAlgName = JavaAlgorithm.getJavaAlgorithmForHash(jwtAlgorithmName, curve);
        byte[] hash = hash(javaAlgName, inputBytes);

        return encodeHashToOIDC(hash, full);
    }

    public static String accessTokenHash(String jwtAlgorithmName, String input) {
        return HashUtils.accessTokenHash(jwtAlgorithmName, null, input, false);
    }

    public static String accessTokenHash(String jwtAlgorithmName, String curve, String input) {
        return HashUtils.accessTokenHash(jwtAlgorithmName, curve, input, false);
    }

    public static byte[] hash(String javaAlgorithmName, byte[] inputBytes) {
        try {
            MessageDigest md = MessageDigest.getInstance(javaAlgorithmName);
            md.update(inputBytes);
            return md.digest();
        } catch (Exception e) {
            throw new HashException("Error when creating token hash", e);
        }
    }

    public static String encodeHashToOIDC(byte[] hash) {
        return encodeHashToOIDC(hash, false);
    }

    public static String encodeHashToOIDC(byte[] hash, boolean full) {
        int hashLength = full ? hash.length : hash.length / 2;
        byte[] hashInput = Arrays.copyOf(hash, hashLength);

        return Base64Url.encode(hashInput);
    }

    public static String sha256UrlEncodedHash(String input, Charset charset) {
        byte[] inputBytes = input.getBytes(charset);
        byte[] hashedOutput = hash(JavaAlgorithm.SHA256, inputBytes);
        return Base64Url.encode(hashedOutput);
    }

}

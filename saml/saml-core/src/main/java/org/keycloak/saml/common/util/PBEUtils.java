/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.saml.common.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

/**
 * Utility dealing with Password Based Encryption (Code is ripped off of the PBEUtils class in JBossSecurity/PicketBox)
 *
 * @author Scott.Stark@jboss.org
 * @author Anil.Saldhana@redhat.com
 * @since May 25, 2010
 */
public class PBEUtils {
    public static byte[] encode(byte[] secret, String cipherAlgorithm, SecretKey cipherKey, PBEParameterSpec cipherSpec)
            throws Exception {
        Cipher cipher = Cipher.getInstance(cipherAlgorithm);
        cipher.init(Cipher.ENCRYPT_MODE, cipherKey, cipherSpec);
        byte[] encoding = cipher.doFinal(secret);
        return encoding;
    }

    public static String encode64(byte[] secret, String cipherAlgorithm, SecretKey cipherKey, PBEParameterSpec cipherSpec)
            throws Exception {
        byte[] encoding = encode(secret, cipherAlgorithm, cipherKey, cipherSpec);
        String b64 = Base64.encodeBytes(encoding);
        return b64;
    }

    public static byte[] decode(byte[] secret, String cipherAlgorithm, SecretKey cipherKey, PBEParameterSpec cipherSpec)
            throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(cipherAlgorithm);
        cipher.init(Cipher.DECRYPT_MODE, cipherKey, cipherSpec);
        byte[] decode = cipher.doFinal(secret);
        return decode;
    }

    public static String decode64(String secret, String cipherAlgorithm, SecretKey cipherKey, PBEParameterSpec cipherSpec)
            throws GeneralSecurityException, UnsupportedEncodingException {
        byte[] encoding = Base64.decode(secret);
        byte[] decode = decode(encoding, cipherAlgorithm, cipherKey, cipherSpec);
        return new String(decode, "UTF-8");
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Encrypt a password" + "Usage: PBEUtils salt count domain-password password"
                    + " salt : the Salt " + " count : the IterationCount "
                    + " password : the plaintext password that should be encrypted");
            throw new RuntimeException(" ERROR: please see format above");
        }

        byte[] salt = args[0].substring(0, 8).getBytes();
        int count = Integer.parseInt(args[1]);
        char[] password = "somearbitrarycrazystringthatdoesnotmatter".toCharArray();
        byte[] passwordToEncode = args[2].getBytes("UTF-8");
        PBEParameterSpec cipherSpec = new PBEParameterSpec(salt, count);
        PBEKeySpec keySpec = new PBEKeySpec(password);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBEwithMD5andDES");
        SecretKey cipherKey = factory.generateSecret(keySpec);
        String encodedPassword = encode64(passwordToEncode, "PBEwithMD5andDES", cipherKey, cipherSpec);
        System.err.println("Encoded password: MASK-" + encodedPassword);
    }
}
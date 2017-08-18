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

package org.keycloak.test;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.keycloak.common.util.PemUtils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmKeyGenerator {
    static {
        if (Security.getProvider("BC") == null) Security.addProvider(new BouncyCastleProvider());
    }
    public static void main(String[] args) throws Exception {
        KeyPair keyPair = null;
        try {
            keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        System.out.println("privateKey : " + PemUtils.encodeKey(keyPair.getPrivate()));
        System.out.println("publicKey : " + PemUtils.encodeKey(keyPair.getPublic()));
    }
}

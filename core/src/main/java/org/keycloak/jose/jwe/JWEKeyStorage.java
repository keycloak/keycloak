/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.jose.jwe;

import java.security.Key;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.jose.jwe.enc.JWEEncryptionProvider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JWEKeyStorage {

    private Key encryptionKey;
    private Key decryptionKey;

    private byte[] cekBytes;

    private Map<KeyUse, Key> decodedCEK = new HashMap<>();

    private JWEEncryptionProvider encryptionProvider;


    public Key getEncryptionKey() {
        return encryptionKey;
    }

    public JWEKeyStorage setEncryptionKey(Key encryptionKey) {
        this.encryptionKey = encryptionKey;
        return this;
    }

    public Key getDecryptionKey() {
        return decryptionKey;
    }

    public JWEKeyStorage setDecryptionKey(Key decryptionKey) {
        this.decryptionKey = decryptionKey;
        return this;
    }

    public void setCEKBytes(byte[] cekBytes) {
        this.cekBytes = cekBytes;
    }

    public byte[] getCekBytes() {
        if (cekBytes == null) {
            cekBytes = encryptionProvider.serializeCEK(this);
        }
        return cekBytes;
    }

    public JWEKeyStorage setCEKKey(Key key, KeyUse keyUse) {
        decodedCEK.put(keyUse, key);
        return this;
    }


    public Key getCEKKey(KeyUse keyUse, boolean generateIfNotPresent) {
        Key key = decodedCEK.get(keyUse);
        if (key == null) {
            if (encryptionProvider != null) {

                if (cekBytes == null && generateIfNotPresent) {
                    generateCekBytes();
                }

                if (cekBytes != null) {
                    encryptionProvider.deserializeCEK(this);
                }
            } else {
                throw new IllegalStateException("encryptionProvider needs to be set");
            }
        }

        return decodedCEK.get(keyUse);
    }


    private void generateCekBytes() {
        int cekLength = encryptionProvider.getExpectedCEKLength();
        cekBytes = JWEUtils.generateSecret(cekLength);
    }


    public void setEncryptionProvider(JWEEncryptionProvider encryptionProvider) {
        this.encryptionProvider = encryptionProvider;
    }


    public enum KeyUse {
        ENCRYPTION,
        SIGNATURE
    }

}

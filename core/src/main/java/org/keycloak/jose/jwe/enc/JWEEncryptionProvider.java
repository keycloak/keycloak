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

package org.keycloak.jose.jwe.enc;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;

import org.keycloak.jose.jwe.JWE;
import org.keycloak.jose.jwe.JWEKeyStorage;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface JWEEncryptionProvider {

    /**
     * This method usually has 3 outputs:
     * - generated initialization vector
     * - encrypted content
     * - authenticationTag for MAC validation
     *
     * It is supposed to call {@link JWE#setEncryptedContentInfo(byte[], byte[], byte[])} after it's finished
     *
     * @param jwe
     * @throws IOException
     * @throws GeneralSecurityException
     */
    void encodeJwe(JWE jwe) throws Exception;


    /**
     * This method is supposed to verify checksums and decrypt content. Then it needs to call {@link JWE#content(byte[])} after it's finished
     *
     * @param jwe
     * @throws IOException
     * @throws GeneralSecurityException
     */
    void verifyAndDecodeJwe(JWE jwe) throws Exception;


    /**
     * This method requires that decoded CEK keys are present in the keyStorage.decodedCEK map before it's called
     *
     * @param keyStorage
     * @return
     */
    byte[] serializeCEK(JWEKeyStorage keyStorage);

    /**
     * This method is supposed to deserialize keys. It requires that {@link JWEKeyStorage#getCekBytes()} is set. After keys are deserialized,
     * this method needs to call {@link JWEKeyStorage#setCEKKey(Key, JWEKeyStorage.KeyUse)} according to all uses, which this encryption algorithm requires.
     *
     * @param keyStorage
     */
    void deserializeCEK(JWEKeyStorage keyStorage);

    int getExpectedCEKLength();

}

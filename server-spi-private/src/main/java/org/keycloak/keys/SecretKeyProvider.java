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

package org.keycloak.keys;

import javax.crypto.SecretKey;

/**
 * Base for secret key providers (HMAC, AES)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface SecretKeyProvider extends KeyProvider<SecretKeyMetadata> {

    /**
     * Return the active secret key, or <code>null</code> if no active key is available.
     *
     * @return
     */
    SecretKey getSecretKey();

    /**
     * Return the secret key for the specified kid, or <code>null</code> if the kid is unknown.
     *
     * @param kid
     * @return
     */
    SecretKey getSecretKey(String kid);


    /**
     * Return name of Java (JCA) algorithm of the key. For example: HmacSHA256
     * @return
     */
    String getJavaAlgorithmName();
}

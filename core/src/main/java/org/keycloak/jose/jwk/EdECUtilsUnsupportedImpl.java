/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.jose.jwk;

import java.security.Key;
import java.security.PublicKey;

import org.keycloak.crypto.KeyUse;

/**
 * <p>Unsupported implementation for old jdk versions.</p>
 *
 * @author rmartinc
 */
class EdECUtilsUnsupportedImpl implements EdECUtils {

    @Override
    public boolean isEdECSupported() {
        return false;
    }

    @Override
    public JWK okp(String kid, String algorithm, Key key, KeyUse keyUse) {
        throw new UnsupportedOperationException("EdDSA algorithms not supported in this JDK version");
    }

    @Override
    public PublicKey createOKPPublicKey(JWK jwk) {
        throw new UnsupportedOperationException("EdDSA algorithms not supported in this JDK version");
    }
}

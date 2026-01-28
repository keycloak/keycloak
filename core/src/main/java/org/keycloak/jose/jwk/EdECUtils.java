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
 * <p>Interface for the EdECUtils that will be implemented only for JDK 15+.</p>
 *
 * @author rmartinc
 */
interface EdECUtils {

    boolean isEdECSupported();

    JWK okp(String kid, String algorithm, Key key, KeyUse keyUse);

    PublicKey createOKPPublicKey(JWK jwk);
}

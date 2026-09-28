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

package org.keycloak.broker.oid4vp;

import org.keycloak.jose.jwe.JWE;
import org.keycloak.jose.jwe.JWEHeader;

/**
 * A {@code direct_post.jwt} response JWE with its parsed header. The header carries the kid that
 * locates the ephemeral decryption key, so it must be readable before the response can be decrypted.
 */
public record ParsedResponse(JWE jwe, JWEHeader header) {

    public String keyId() {
        return header.getKeyId();
    }
}

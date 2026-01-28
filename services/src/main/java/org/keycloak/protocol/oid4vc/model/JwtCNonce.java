/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.protocol.oid4vc.model;

import org.keycloak.representations.JsonWebToken;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Pascal Knüppel
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JwtCNonce extends JsonWebToken {

    /**
     * cryptographically strong random string that serves as salt for the otherwise predictable c_nonce values
     */
    @JsonProperty("salt")
    private String salt;

    public String getSalt() {
        return salt;
    }

    public JwtCNonce salt(String salt) {
        this.salt = salt;
        return this;
    }
}

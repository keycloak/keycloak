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

package org.keycloak.protocol.oid4vc.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a pre-authorized grant, as used by the Credential Offer in OID4VCI
 * {@see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-credential-offer}
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PreAuthorizedCode {

    @JsonProperty("pre-authorized_code")
    private String preAuthorizedCode;
    @JsonProperty("pre-user_pin_required")
    private boolean userPinRequired;

    public String getPreAuthorizedCode() {
        return preAuthorizedCode;
    }

    public PreAuthorizedCode setPreAuthorizedCode(String preAuthorizedCode) {
        this.preAuthorizedCode = preAuthorizedCode;
        return this;
    }

    public boolean getUserPinRequired() {
        return userPinRequired;
    }

    public PreAuthorizedCode setUserPinRequired(boolean userPinRequired) {
        this.userPinRequired = userPinRequired;
        return this;
    }
}
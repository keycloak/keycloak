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
 * Represents a transaction code as used in the pre-authorized grant in the Credential Offer in OID4VCI
 * {@see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-credential-offer}
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TxCode {

    @JsonProperty("input_mode")
    private String inputMode;

    @JsonProperty("length")
    private int length;

    @JsonProperty("description")
    private String description;

    public String getInputMode() {
        return inputMode;
    }

    public TxCode setInputMode(String inputMode) {
        this.inputMode = inputMode;
        return this;
    }

    public int getLength() {
        return length;
    }

    public TxCode setLength(int length) {
        this.length = length;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public TxCode setDescription(String description) {
        this.description = description;
        return this;
    }
}

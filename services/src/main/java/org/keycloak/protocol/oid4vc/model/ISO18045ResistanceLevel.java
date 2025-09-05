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
 */

package org.keycloak.protocol.oid4vc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Attack Potential Resistance. Defined values for `key_storage` and `user_authentication`
 * in {@link KeyAttestationsRequired} as per ISO 18045.
 *
 * @author <a href="mailto:Ingrid.Kamga@adorsys.com">Ingrid Kamga</a>
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0-15.html#name-attack-potential-resistance">
 * OpenID4VCI Attack Potential Resistance</a>
 */
public enum ISO18045ResistanceLevel {

    HIGH("iso_18045_high"), // VAN.5
    MODERATE("iso_18045_moderate"), // VAN.4
    ENHANCED_BASIC("iso_18045_enhanced-basic"), // VAN.3
    BASIC("iso_18045_basic"); // VAN.2

    private final String value;

    ISO18045ResistanceLevel(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return getValue();
    }

    @JsonCreator
    public static ISO18045ResistanceLevel fromValue(String value) {
        for (ISO18045ResistanceLevel level : values()) {
            if (level.value.equals(value)) {
                return level;
            }
        }

        throw new IllegalArgumentException("Unknown ISO18045ResistanceLevel: " + value);
    }
}

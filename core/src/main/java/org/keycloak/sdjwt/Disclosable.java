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
package org.keycloak.sdjwt;

import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Handles undisclosed claims and array elements, providing functionality
 * to generate disclosure digests from Base64Url encoded strings.
 * 
 * Hiding claims and array elements occurs by including their digests
 * instead of plaintext in the signed verifiable credential.
 * 
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 * 
 */
public abstract class Disclosable {
    private final SdJwtSalt salt;

    /**
     * Returns the array of undisclosed value, for
     * encoding (disclosure string) and hashing (_sd digest array in the VC).
     */
    abstract Object[] toArray();

    protected Disclosable(SdJwtSalt salt) {
        this.salt = Objects.requireNonNull(salt, "Disclosure always requires a salt must not be null");
    }

    public SdJwtSalt getSalt() {
        return salt;
    }

    public String getSaltAsString() {
        return salt.toString();
    }

    public String toJson() {
        try {
            return SdJwtUtils.printJsonArray(toArray());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public String getDisclosureString() {
        String json = toJson();
        return SdJwtUtils.encodeNoPad(json);
    }

    public String getDisclosureDigest(String hashAlg) {
        return SdJwtUtils.hashAndBase64EncodeNoPad(getDisclosureString(), hashAlg);
    }

    @Override
    public String toString() {
        return getDisclosureString();
    }
}

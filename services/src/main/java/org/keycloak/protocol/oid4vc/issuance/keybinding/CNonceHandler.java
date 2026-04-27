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

package org.keycloak.protocol.oid4vc.issuance.keybinding;

import java.util.List;
import java.util.Map;

import jakarta.annotation.Nullable;

import org.keycloak.common.VerificationException;
import org.keycloak.provider.Provider;

/**
 * @author Pascal Knüppel
 */
public interface CNonceHandler extends Provider {

    /**
     * used to build a cNonce in any style. For jwt-based cNonces we will additionally require the audience-values that
     * should be added into the cNonce
     *
     * @param audiences         the audiences for jwt-based cNonces
     * @param additionalDetails additional attributes that might be required to build the cNonce and that are handler
     *                          specific
     * @return the cNonce in string representation
     */
    public String buildCNonce(List<String> audiences, @Nullable Map<String, Object> additionalDetails);

    /**
     * must verify the validity of a cNonce value that has been issued by the {@link #buildCNonce(List, Map)} method.
     *
     * @param cNonce            the cNonce to validate
     * @param audiences         the expected audiences for jwt-based cNonces
     * @param additionalDetails additional attributes that might be required to build the cNonce and that are handler
     *                          specific
     */
    public void verifyCNonce(String cNonce, List<String> audiences, @Nullable Map<String, Object> additionalDetails) throws VerificationException;

    @Override
    default void close() {
        // do nothing
    }
}

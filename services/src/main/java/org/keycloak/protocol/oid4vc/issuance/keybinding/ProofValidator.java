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

package org.keycloak.protocol.oid4vc.issuance.keybinding;

import java.util.List;

import org.keycloak.jose.jwk.JWK;
import org.keycloak.protocol.oid4vc.issuance.VCIssuanceContext;
import org.keycloak.protocol.oid4vc.issuance.VCIssuerException;
import org.keycloak.provider.Provider;

public interface ProofValidator extends Provider {

    @Override
    default void close() {
    }

    String getProofType();

    /**
     * Validates client-provided key binding proofs.
     *
     * @param vcIssuanceContext the issuance context with credential request and config
     * @return the list of JWKs to bind to credentials (one JWK per credential)
     */
    List<JWK> validateProof(VCIssuanceContext vcIssuanceContext) throws VCIssuerException;
}

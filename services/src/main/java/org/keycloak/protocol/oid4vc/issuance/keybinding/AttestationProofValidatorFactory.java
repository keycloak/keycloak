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

package org.keycloak.protocol.oid4vc.issuance.keybinding;

import org.keycloak.jose.jwk.JWK;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.model.ProofType;

import java.util.Map;

/**
 * @author <a href="mailto:Rodrick.Awambeng@adorsys.com">Rodrick Awambeng</a>
 */
public class AttestationProofValidatorFactory implements ProofValidatorFactory {

    @Override
    public String getId() {
        return ProofType.ATTESTATION;
    }

    @Override
    public ProofValidator create(KeycloakSession session) {
        // TODO: Load trusted keys from config, DB, or env
        Map<String, JWK> trustedKeys = Map.of(); // empty for now

        AttestationKeyResolver resolver = new StaticAttestationKeyResolver(trustedKeys);
        return new AttestationProofValidator(session, resolver);
    }
}

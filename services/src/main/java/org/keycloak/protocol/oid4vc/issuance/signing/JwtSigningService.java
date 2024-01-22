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

package org.keycloak.protocol.oid4vc.issuance.signing;

import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;

import java.time.Clock;

/**
 * {@link VerifiableCredentialsSigningService} implementing the JWT_VC format. It returns a string, containing the
 * Signed JWT-Credential
 * {@see https://identity.foundation/jwt-vc-presentation-profile/}
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class JwtSigningService extends SigningService<String> {

    public JwtSigningService(KeycloakSession keycloakSession, String keyId, Clock clock, String algorithmType) {
        super(keycloakSession, keyId, clock, algorithmType);
    }

    @Override
    public String signCredential(VerifiableCredential verifiableCredential) {

        throw new UnsupportedOperationException("JWT-VC Credentials Signing is not yet supported.");
    }

}
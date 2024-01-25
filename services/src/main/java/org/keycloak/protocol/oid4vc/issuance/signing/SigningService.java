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

import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.models.KeycloakSession;

/**
 * Abstract base class to provide the Signing Services common functionality
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public abstract class SigningService<T> implements VerifiableCredentialsSigningService<T> {

    protected final KeycloakSession keycloakSession;
    protected final String keyId;

    // values of the type field are defined by the implementing service. Could f.e. the security suite for ldp_vc or the algorithm to be used for jwt_vc
    protected final String type;

    protected SigningService(KeycloakSession keycloakSession, String keyId, String type) {
        this.keycloakSession = keycloakSession;
        this.keyId = keyId;
        this.type = type;
    }

    protected KeyWrapper getKey(String kid, String algorithm) {
        return keycloakSession.keys().getKey(keycloakSession.getContext().getRealm(), kid, KeyUse.SIG, algorithm);
    }

    @Override
    public void close() {
        // no-op
    }
}
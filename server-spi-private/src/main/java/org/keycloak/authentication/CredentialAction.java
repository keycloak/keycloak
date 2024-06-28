/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.authentication;

import org.keycloak.models.KeycloakSession;
import org.keycloak.sessions.AuthenticationSessionModel;

/**
 * Marking any required action implementation, that is supposed to work with user credentials
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface CredentialAction {

    /**
     * @return credential type, which this action is able to register. This should refer to the same value as returned by {@link org.keycloak.credential.CredentialProvider#getType} of the
     * corresponding credential provider and {@link AuthenticatorFactory#getReferenceCategory()} of the corresponding authenticator
     */
    String getCredentialType(KeycloakSession session, AuthenticationSessionModel authenticationSession);
}

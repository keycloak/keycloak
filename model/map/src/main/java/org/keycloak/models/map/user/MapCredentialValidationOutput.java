/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.user;

import org.keycloak.models.CredentialValidationOutput;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Output of a credential validation.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MapCredentialValidationOutput<V> {

    private final V authenticatedUser;
    private final CredentialValidationOutput.Status authStatus;           // status whether user is authenticated or more steps needed
    private final Map<String, String> state;   // Additional state related to authentication. It can contain data to be sent back to client or data about used credentials.

    public MapCredentialValidationOutput(V authenticatedUser, CredentialValidationOutput.Status authStatus, Map<String, String> state) {
        this.authenticatedUser = authenticatedUser;
        this.authStatus = authStatus;
        this.state = state;
    }

    public static MapCredentialValidationOutput<?> failed() {
        return new MapCredentialValidationOutput<Void>(null, CredentialValidationOutput.Status.FAILED, Collections.emptyMap());
    }

    public V getAuthenticatedUser() {
        return authenticatedUser;
    }

    public CredentialValidationOutput.Status getAuthStatus() {
        return authStatus;
    }

    /**
     * State that is passed back by provider
     */
    public Map<String, String> getState() {
        return state;
    }

}

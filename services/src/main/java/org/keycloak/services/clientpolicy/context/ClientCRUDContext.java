/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.clientpolicy.context;

import org.keycloak.models.ClientModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;

/**
 * Represents the context in the request to register/read/update/unregister client by Dynamic Client Registration or Admin REST API.
 */
public interface ClientCRUDContext extends ClientPolicyContext {

    /**
     * returns {@link ClientRepresentation} for creating the new client or updating the existing client.
     *
     * @return {@link ClientRepresentation}
     */
    default ClientRepresentation getProposedClientRepresentation() {
        return null;
    }

    /**
     * returns {@link ClientModel} of the existing client to be updated/read/updated/deleted.
     * on REGISTER event, it returns null.
     *
     * @return {@link ClientModel}
     */
    default ClientModel getTargetClient() {
        return null;
    }

    /**
     * returns {@link UserModel} of the authenticated user.
     *
     * @return {@link UserModel}
     */
    default UserModel getAuthenticatedUser() {
        return null;
    }

    /**
     * returns {@link UserModel} of the authenticated client.
     *
     * @return {@link UserModel}
     */
    default ClientModel getAuthenticatedClient() {
        return null;
    }

    /**
     * returns {@link JsonWebToken} of the token accompanied with the request to register/read/update/unregister client
     *
     * @return {@link JsonWebToken}
     */
    default JsonWebToken getToken() {
        return null;
    }
}

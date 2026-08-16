/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.services.clientpolicy.context.admin;

import org.keycloak.models.ClientModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.CertificateRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.context.ClientModelContext;

/**
 * Context fired by {@link org.keycloak.services.resources.admin.ClientAttributeCertificateResource}
 * on certificate mutation operations through the admin REST API.
 *
 * <p>Dispatched as {@link org.keycloak.services.clientpolicy.ClientPolicyEvent#UPDATE_CLIENT_CERTIFICATE}.
 */
public interface ClientCertificateContext extends ClientPolicyContext, ClientModelContext {

    /**
     * @return the client whose signing material is being updated.
     */
    default ClientModel getTargetClient() {
        return null;
    }

    /**
     * @return the client whose signing material is being updated.
     */
    @Override
    default ClientModel getClient() {
        return getTargetClient();
    }

    /**
     * @return the attribute prefix identifying which certificate slot is being updated.
     */
    default String getAttributePrefix() {
        return null;
    }

    /**
     * @return a defensive copy of the proposed certificate representation before persistence.
     *         The returned representation never exposes private-key material.
     */
    default CertificateRepresentation getProposedCertificate() {
        return null;
    }

    /**
     * @return the authenticated user.
     */
    default UserModel getAuthenticatedUser() {
        return null;
    }

    /**
     * @return the authenticated client.
     */
    default ClientModel getAuthenticatedClient() {
        return null;
    }

    /**
     * @return the access token.
     */
    default JsonWebToken getToken() {
        return null;
    }
}

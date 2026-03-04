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
package org.keycloak.validation.jakarta;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

/**
 * Immutable data record containing context information needed during validation.
 * <p>
 * This record is passed to the validation layer and made available to custom
 * {@link jakarta.validation.ConstraintValidator} implementations via the
 * {@link ValidationContextHolder}.
 *
 * @param session the current Keycloak session
 * @param realm the realm in which validation is performed
 * @param existingClient the existing client model (for update operations), or null for create
 */
public record ValidationContextData(
        @Nonnull KeycloakSession session,
        @Nonnull RealmModel realm,
        @Nullable ClientModel existingClient
) {

    /**
     * Creates a validation context for create operations.
     *
     * @param session the current Keycloak session
     * @param realm the realm in which validation is performed
     * @return a new ValidationContextData instance
     */
    public static ValidationContextData forCreate(@Nonnull KeycloakSession session, @Nonnull RealmModel realm) {
        return new ValidationContextData(session, realm, null);
    }

    /**
     * Creates a validation context for update operations.
     *
     * @param session the current Keycloak session
     * @param realm the realm in which validation is performed
     * @param existingClient the existing client being updated
     * @return a new ValidationContextData instance
     */
    public static ValidationContextData forUpdate(@Nonnull KeycloakSession session, @Nonnull RealmModel realm,
                                                  @Nonnull ClientModel existingClient) {
        return new ValidationContextData(session, realm, existingClient);
    }

    /**
     * @return true if this context represents an update operation
     */
    public boolean isUpdate() {
        return existingClient != null;
    }

    /**
     * @return true if this context represents a create operation
     */
    public boolean isCreate() {
        return existingClient == null;
    }
}

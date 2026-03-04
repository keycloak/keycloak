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

import jakarta.annotation.Nullable;
import jakarta.enterprise.context.RequestScoped;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

/**
 * Request-scoped holder for validation context data.
 * <p>
 * This bean is injected into custom {@link jakarta.validation.ConstraintValidator} implementations
 * to provide access to Keycloak-specific context (session, realm, existing models) during validation.
 * <p>
 * The context is set by the {@link JakartaValidatorProvider} before validation and cleared afterwards.
 * <p>
 * Example usage in a custom validator:
 * <pre>{@code
 * public class UniqueClientIdValidator implements ConstraintValidator<UniqueClientId, String> {
 *
 *     @Inject
 *     ValidationContextHolder contextHolder;
 *
 *     @Override
 *     public boolean isValid(String clientId, ConstraintValidatorContext ctx) {
 *         RealmModel realm = contextHolder.getRealm();
 *         ClientModel existing = contextHolder.getExistingClient();
 *
 *         // Skip check if updating the same client
 *         if (existing != null && clientId.equals(existing.getClientId())) {
 *             return true;
 *         }
 *
 *         return realm.getClientByClientId(clientId) == null;
 *     }
 * }
 * }</pre>
 */
@RequestScoped
public class ValidationContextHolder {

    private ValidationContextData contextData;

    /**
     * Sets the validation context data. Called by the validator provider before validation.
     *
     * @param contextData the context data to set
     */
    public void setContext(ValidationContextData contextData) {
        this.contextData = contextData;
    }

    /**
     * Clears the validation context. Called by the validator provider after validation completes.
     */
    public void clear() {
        this.contextData = null;
    }

    /**
     * @return the full context data, or null if not set
     */
    @Nullable
    public ValidationContextData getContextData() {
        return contextData;
    }

    /**
     * @return the current Keycloak session
     * @throws IllegalStateException if context is not set
     */
    public KeycloakSession getSession() {
        requireContext();
        return contextData.session();
    }

    /**
     * @return the realm in which validation is performed
     * @throws IllegalStateException if context is not set
     */
    public RealmModel getRealm() {
        requireContext();
        return contextData.realm();
    }

    /**
     * @return the existing client model (for update operations), or null for create
     * @throws IllegalStateException if context is not set
     */
    @Nullable
    public ClientModel getExistingClient() {
        requireContext();
        return contextData.existingClient();
    }

    /**
     * @return true if this is an update operation (existing client is present)
     * @throws IllegalStateException if context is not set
     */
    public boolean isUpdate() {
        requireContext();
        return contextData.isUpdate();
    }

    /**
     * @return true if this is a create operation (no existing client)
     * @throws IllegalStateException if context is not set
     */
    public boolean isCreate() {
        requireContext();
        return contextData.isCreate();
    }

    /**
     * @return true if validation context has been set
     */
    public boolean hasContext() {
        return contextData != null;
    }

    private void requireContext() {
        if (contextData == null) {
            throw new IllegalStateException(
                    "Validation context not set. Ensure validation is called through JakartaValidatorProvider " +
                    "with context, or that this validator does not require context.");
        }
    }
}

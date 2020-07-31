/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.validation;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Collections;
import java.util.Map;

/**
 * Denotes a context in which the Validation takes place. A {@link ValidationContext} should be created for a
 * batch of validation checks for a given entity.
 */
public class ValidationContext {

    private final RealmModel realm;

    // user registration, user profile update, client registration, realm creation
    private final ValidationContextKey contextKey;

    // additional context specific attributes
    private final Map<String, Object> attributes;

    /**
     * Holds the current {@link UserModel}
     */
    private final UserModel user;

    /**
     * Holds the current {@link ClientModel}
     */
    private final ClientModel client;

    /**
     * Reports all discovered problems if true, otherwise stop on first Problem.
     */
    private final boolean bulkMode;

    // TODO add support to skip certain validations for ValidationKey

    public ValidationContext(ValidationContext that) {
        this(that.getRealm(), that.getContextKey(), that.getAttributes(), that.getUser(), that.getClient(), that.isBulkMode());
    }

    public ValidationContext(RealmModel realm, ValidationContextKey contextKey) {
        this(realm, contextKey, Collections.emptyMap(), null, null, true);
    }

    public ValidationContext(RealmModel realm, ValidationContextKey contextKey, Map<String, Object> attributes) {
        this(realm, contextKey, attributes, null, null, true);
    }

    /**
     *
     * @param realm
     * @param contextKey
     * @param attributes
     * @param user
     * @param client
     * @param bulkMode
     */
    public ValidationContext(RealmModel realm, ValidationContextKey contextKey, Map<String, Object> attributes, UserModel user, ClientModel client, boolean bulkMode) {
        this.realm = realm;
        this.contextKey = contextKey;
        this.attributes = attributes;
        this.user = user;
        this.client = client;
        this.bulkMode = bulkMode;
    }

    public RealmModel getRealm() {
        return realm;
    }

    public ValidationContextKey getContextKey() {
        return contextKey;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public boolean getAttributeAsBoolean(String name) {
        return attributes.get(name) == Boolean.TRUE;
    }

    public String getAttributeAsString(String name) {
        Object value = attributes.get(name);
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    public UserModel getUser() {
        return user;
    }

    public ClientModel getClient() {
        return client;
    }

    public boolean isBulkMode() {
        return bulkMode;
    }

    public ValidationContext withUser(UserModel user) {
        return new ValidationContext(realm, contextKey, attributes, user, client, bulkMode);
    }

    public ValidationContext withClient(ClientModel client) {
        return new ValidationContext(realm, contextKey, attributes, user, client, bulkMode);
    }

    public ValidationContext withBulkMode(boolean bulkMode) {
        return new ValidationContext(realm, contextKey, attributes, user, client, bulkMode);
    }
}

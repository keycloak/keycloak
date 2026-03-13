/*
 * Copyright 2026 Capital One Financial Corporation and/or its affiliates
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

package org.keycloak.models.redis.session;

import org.keycloak.models.KeycloakTransaction;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Base class for Redis persistence transactions.
 * Provides common transaction lifecycle management for Redis-backed session adapters.
 */
public abstract class AbstractRedisPersistenceTransaction implements KeycloakTransaction {

    private final Consumer<Boolean> modifiedFlagSetter;
    private final BooleanSupplier modifiedFlagGetter;

    /**
     * @param modifiedFlagSetter Consumer to reset the modified flag (true = modified, false = not modified)
     * @param modifiedFlagGetter Supplier to check if the entity is modified
     */
    protected AbstractRedisPersistenceTransaction(Consumer<Boolean> modifiedFlagSetter,
                                                   BooleanSupplier modifiedFlagGetter) {
        this.modifiedFlagSetter = modifiedFlagSetter;
        this.modifiedFlagGetter = modifiedFlagGetter;
    }

    @Override
    public void begin() {
        // Nothing to do
    }

    @Override
    public void commit() {
        persist();
    }

    @Override
    public void rollback() {
        // Reset modified flag on rollback
        modifiedFlagSetter.accept(false);
    }

    @Override
    public void setRollbackOnly() {
        // Mark for rollback
    }

    @Override
    public boolean getRollbackOnly() {
        return false;
    }

    @Override
    public boolean isActive() {
        return modifiedFlagGetter.getAsBoolean();
    }

    /**
     * Subclasses must implement this to perform the actual persistence logic.
     */
    protected abstract void persist();
}
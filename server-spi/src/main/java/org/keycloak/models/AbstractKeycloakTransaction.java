/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models;

/**
 * Handles some common transaction logic related to start, rollback-only etc.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractKeycloakTransaction implements KeycloakTransaction {

    protected TransactionState state = TransactionState.NOT_STARTED;

    @Override
    public void begin() {
        if (state != TransactionState.NOT_STARTED) {
            throw new IllegalStateException("Transaction already started");
        }

        beginImpl();

        state = TransactionState.STARTED;
    }

    @Override
    public void commit() {
        if (state != TransactionState.STARTED) {
            throw new IllegalStateException("Transaction in illegal state for commit: " + state);
        }

        commitImpl();

        state = TransactionState.FINISHED;
    }

    @Override
    public void rollback() {
        if (state != TransactionState.STARTED && state != TransactionState.ROLLBACK_ONLY) {
            throw new IllegalStateException("Transaction in illegal state for rollback: " + state);
        }

        rollbackImpl();

        state = TransactionState.FINISHED;
    }

    @Override
    public void setRollbackOnly() {
        state = TransactionState.ROLLBACK_ONLY;
    }

    @Override
    public boolean getRollbackOnly() {
        return state == TransactionState.ROLLBACK_ONLY;
    }

    @Override
    public boolean isActive() {
        return state == TransactionState.STARTED || state == TransactionState.ROLLBACK_ONLY;
    }

    public TransactionState getState() {
        return state;
    }

    public enum TransactionState {
        NOT_STARTED, STARTED, ROLLBACK_ONLY, FINISHED
    }

    protected void beginImpl() {}

    protected abstract void commitImpl();

    protected abstract void rollbackImpl();
}

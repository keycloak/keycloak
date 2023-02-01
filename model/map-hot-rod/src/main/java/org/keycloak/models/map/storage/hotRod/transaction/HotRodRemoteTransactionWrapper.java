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

package org.keycloak.models.map.storage.hotRod.transaction;

import org.keycloak.models.KeycloakTransaction;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

/**
 * When no JTA transaction is present in the runtime this wrapper is used
 * to enlist HotRod client provided transaction to our
 * {@link KeycloakTransactionManager}. If JTA transaction is present this should
 * not be used.
 */
public class HotRodRemoteTransactionWrapper implements KeycloakTransaction {

    private final TransactionManager transactionManager;

    public HotRodRemoteTransactionWrapper(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Override
    public void begin() {
        try {
            if (transactionManager.getStatus() == Status.STATUS_NO_TRANSACTION) {
                transactionManager.begin();
            }
        } catch (NotSupportedException | SystemException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void commit() {
        try {
            if (transactionManager.getStatus() == Status.STATUS_ACTIVE) {
                transactionManager.commit();
            }
        } catch (HeuristicRollbackException | SystemException | HeuristicMixedException | RollbackException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void rollback() {
        try {
            if (transactionManager.getStatus() == Status.STATUS_ACTIVE) {
                transactionManager.rollback();
            }
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setRollbackOnly() {
        try {
            if (transactionManager.getStatus() == Status.STATUS_ACTIVE) {
                transactionManager.setRollbackOnly();
            }
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean getRollbackOnly() {
        try {
            return transactionManager.getStatus() == Status.STATUS_MARKED_ROLLBACK;
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isActive() {
        try {
            return transactionManager.getStatus() == Status.STATUS_ACTIVE;
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
    }
}

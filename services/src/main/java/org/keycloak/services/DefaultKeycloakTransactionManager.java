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
package org.keycloak.services;

import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.KeycloakTransactionManager;

import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultKeycloakTransactionManager implements KeycloakTransactionManager {

    public static final ServicesLogger logger = ServicesLogger.ROOT_LOGGER;

    private List<KeycloakTransaction> prepare = new LinkedList<KeycloakTransaction>();
    private List<KeycloakTransaction> transactions = new LinkedList<KeycloakTransaction>();
    private List<KeycloakTransaction> afterCompletion = new LinkedList<KeycloakTransaction>();
    private boolean active;
    private boolean rollback;

    @Override
    public void enlist(KeycloakTransaction transaction) {
        if (active && !transaction.isActive()) {
            transaction.begin();
        }

        transactions.add(transaction);
    }

    @Override
    public void enlistAfterCompletion(KeycloakTransaction transaction) {
        if (active && !transaction.isActive()) {
            transaction.begin();
        }

        afterCompletion.add(transaction);
    }

    @Override
    public void enlistPrepare(KeycloakTransaction transaction) {
        if (active && !transaction.isActive()) {
            transaction.begin();
        }

        prepare.add(transaction);
    }

    @Override
    public void begin() {
        if (active) {
             throw new IllegalStateException("Transaction already active");
        }

        for (KeycloakTransaction tx : transactions) {
            tx.begin();
        }

        active = true;
    }

    @Override
    public void commit() {
        RuntimeException exception = null;
        for (KeycloakTransaction tx : prepare) {
            try {
                tx.commit();
            } catch (RuntimeException e) {
                exception = exception == null ? e : exception;
            }
        }
        if (exception != null) {
            rollback(exception);
            return;
        }
        for (KeycloakTransaction tx : transactions) {
            try {
                tx.commit();
            } catch (RuntimeException e) {
                exception = exception == null ? e : exception;
            }
        }

        // Don't commit "afterCompletion" if commit of some main transaction failed
        if (exception == null) {
            for (KeycloakTransaction tx : afterCompletion) {
                try {
                    tx.commit();
                } catch (RuntimeException e) {
                    exception = exception == null ? e : exception;
                }
            }
        } else {
            for (KeycloakTransaction tx : afterCompletion) {
                try {
                    tx.rollback();
                } catch (RuntimeException e) {
                    logger.exceptionDuringRollback(e);
                }
            }
        }

        active = false;
        if (exception != null) {
            throw exception;
        }
    }

    @Override
    public void rollback() {
        RuntimeException exception = null;
        rollback(exception);
    }

    protected void rollback(RuntimeException exception) {
        for (KeycloakTransaction tx : transactions) {
            try {
                tx.rollback();
            } catch (RuntimeException e) {
                exception = exception != null ? e : exception;
            }
        }
        for (KeycloakTransaction tx : afterCompletion) {
            try {
                tx.rollback();
            } catch (RuntimeException e) {
                exception = exception != null ? e : exception;
            }
        }
        active = false;
        if (exception != null) {
            throw exception;
        }
    }

    @Override
    public void setRollbackOnly() {
        rollback = true;
    }

    @Override
    public boolean getRollbackOnly() {
        if (rollback) {
            return true;
        }

        for (KeycloakTransaction tx : transactions) {
            if (tx.getRollbackOnly()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isActive() {
        return active;
    }

}

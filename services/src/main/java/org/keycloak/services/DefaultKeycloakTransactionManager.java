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

import java.util.LinkedList;
import java.util.List;

import jakarta.transaction.TransactionManager;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.KeycloakTransactionManager;
import org.keycloak.tracing.TracingProvider;
import org.keycloak.transaction.JtaTransactionManagerLookup;
import org.keycloak.transaction.JtaTransactionWrapper;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultKeycloakTransactionManager implements KeycloakTransactionManager {

    private final List<KeycloakTransaction> prepare = new LinkedList<>();
    private final List<KeycloakTransaction> transactions = new LinkedList<>();
    private final List<KeycloakTransaction> afterCompletion = new LinkedList<>();
    private boolean active;
    private boolean rollback;
    private final KeycloakSession session;
    private JTAPolicy jtaPolicy = JTAPolicy.REQUIRES_NEW;
    // Used to prevent double committing/rollback if there is an uncaught exception
    protected boolean completed;

    public DefaultKeycloakTransactionManager(KeycloakSession session) {
        this.session = session;
    }

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
    public JTAPolicy getJTAPolicy() {
        return jtaPolicy;
    }

    @Override
    public void setJTAPolicy(JTAPolicy policy) {
        jtaPolicy = policy;

    }

    @Override
    public void begin() {
        if (active) {
             throw new IllegalStateException("Transaction already active");
        }

        completed = false;

        if (jtaPolicy == JTAPolicy.REQUIRES_NEW) {
            JtaTransactionManagerLookup jtaLookup = session.getProvider(JtaTransactionManagerLookup.class);
            if (jtaLookup != null) {
                TransactionManager tm = jtaLookup.getTransactionManager();
                if (tm != null) {
                   enlist(new JtaTransactionWrapper(session, tm));
                }
            }
        }

        for (KeycloakTransaction tx : transactions) {
            tx.begin();
        }

        active = true;
    }

    @Override
    public void commit() {
        if (completed) {
            return;
        } else {
            completed = true;
        }

        TracingProvider tracing = session.getProvider(TracingProvider.class);
        tracing.trace(DefaultKeycloakTransactionManager.class, "commit", span -> {
            RuntimeException exception = null;
            for (KeycloakTransaction tx : prepare) {
                try {
                    commitWithTracing(tx, tracing);
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
                    commitWithTracing(tx, tracing);
                } catch (RuntimeException e) {
                    exception = exception == null ? e : exception;
                }
            }

            // Don't commit "afterCompletion" if commit of some main transaction failed
            if (exception == null) {
                for (KeycloakTransaction tx : afterCompletion) {
                    try {
                        commitWithTracing(tx, tracing);
                    } catch (RuntimeException e) {
                        exception = exception == null ? e : exception;
                    }
                }
            } else {
                for (KeycloakTransaction tx : afterCompletion) {
                    try {
                        tx.rollback();
                    } catch (RuntimeException e) {
                        ServicesLogger.LOGGER.exceptionDuringRollback(e);
                    }
                }
            }

            active = false;
            if (exception != null) {
                throw exception;
            }
        });
    }

    private static void commitWithTracing(KeycloakTransaction tx, TracingProvider tracing) {
        tracing.trace(tx.getClass(), "commit", span -> {
            tx.commit();
        });
    }

    @Override
    public void rollback() {
        if (completed) {
            return;
        } else {
            completed = true;
        }

        RuntimeException exception = null;
        rollback(exception);
    }

    protected void rollback(RuntimeException exception) {
        TracingProvider tracing = session.getProvider(TracingProvider.class);

        for (KeycloakTransaction tx : transactions) {
            try {
                rollbackWithTracing(tx, tracing);
            } catch (RuntimeException e) {
                exception = exception != null ? e : exception;
            }
        }
        for (KeycloakTransaction tx : afterCompletion) {
            try {
                rollbackWithTracing(tx, tracing);
            } catch (RuntimeException e) {
                exception = exception != null ? e : exception;
            }
        }
        active = false;
        if (exception != null) {
            throw exception;
        }
    }

    private static void rollbackWithTracing(KeycloakTransaction tx, TracingProvider tracing) {
        tracing.trace(tx.getClass(), "rollback", span -> {
            tx.rollback();
        });
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

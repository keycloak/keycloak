package org.keycloak.services;

import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.KeycloakTransactionManager;

import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultKeycloakTransactionManager implements KeycloakTransactionManager {

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
        for (KeycloakTransaction tx : transactions) {
            try {
                tx.commit();
            } catch (RuntimeException e) {
                exception = exception == null ? e : exception;
            }
        }
        for (KeycloakTransaction tx : afterCompletion) {
            try {
                tx.commit();
            } catch (RuntimeException e) {
                exception = exception == null ? e : exception;
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

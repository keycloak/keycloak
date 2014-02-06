package org.keycloak.models.mongo.keycloak.adapters;

import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.mongo.api.context.MongoStoreInvocationContext;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoKeycloakTransaction implements KeycloakTransaction {

    private final MongoStoreInvocationContext invocationContext;

    private boolean started = false;
    private boolean rollbackOnly = false;

    public MongoKeycloakTransaction(MongoStoreInvocationContext invocationContext) {
        this.invocationContext = invocationContext;
    }

    @Override
    public void begin() {
        if (started) {
            throw new IllegalStateException("Transaction already started");
        }
        started = true;
        invocationContext.begin();
    }

    @Override
    public void commit() {
        if (!started) {
            throw new IllegalStateException("Transaction not yet started");
        }
        if (rollbackOnly) {
            throw new IllegalStateException("Can't commit as transaction marked for rollback");
        }

        invocationContext.commit();
    }

    @Override
    public void rollback() {
        invocationContext.rollback();
    }

    @Override
    public void setRollbackOnly() {
        this.rollbackOnly = true;
    }

    @Override
    public boolean getRollbackOnly() {
        return rollbackOnly;
    }

    @Override
    public boolean isActive() {
        return started;
    }
}

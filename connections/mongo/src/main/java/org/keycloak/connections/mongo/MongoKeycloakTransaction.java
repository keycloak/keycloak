package org.keycloak.connections.mongo;

import com.mongodb.MongoException;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.connections.mongo.impl.MongoStoreImpl;
import org.keycloak.models.KeycloakTransaction;

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

        try {
            invocationContext.commit();
        } catch (MongoException e) {
            throw MongoStoreImpl.convertException(e);
        }
        started = false;
    }

    @Override
    public void rollback() {
        invocationContext.rollback();
        started = false;
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

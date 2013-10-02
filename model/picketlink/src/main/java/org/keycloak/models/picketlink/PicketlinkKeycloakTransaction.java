package org.keycloak.models.picketlink;

import org.keycloak.models.KeycloakTransaction;

import javax.persistence.EntityTransaction;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class PicketlinkKeycloakTransaction implements KeycloakTransaction {
    protected EntityTransaction transaction;

    public PicketlinkKeycloakTransaction(EntityTransaction transaction) {
        this.transaction = transaction;
    }

    public void begin() {
        transaction.begin();
    }

    public void setRollbackOnly() {
        transaction.setRollbackOnly();
    }

    public boolean isActive() {
        return transaction.isActive();
    }

    public boolean getRollbackOnly() {
        return transaction.getRollbackOnly();
    }

    public void commit() {
        transaction.commit();
    }

    public void rollback() {
        transaction.rollback();
    }
}

package org.keycloak.services.models.picketlink;

import org.keycloak.services.models.KeycloakTransaction;
import org.picketlink.idm.IdentityTransaction;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class PicketlinkKeycloakTransaction implements KeycloakTransaction{
    protected IdentityTransaction transaction;

    public PicketlinkKeycloakTransaction(IdentityTransaction transaction) {
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

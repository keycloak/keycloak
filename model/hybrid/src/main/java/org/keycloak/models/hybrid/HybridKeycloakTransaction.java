package org.keycloak.models.hybrid;

import org.keycloak.models.KeycloakTransaction;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class HybridKeycloakTransaction implements KeycloakTransaction {

    private KeycloakTransaction[] txs;

    public HybridKeycloakTransaction(KeycloakTransaction... txs) {
        this.txs = txs;
    }

    @Override
    public void begin() {
        for (KeycloakTransaction tx : txs) {
            tx.begin();
        }
    }

    @Override
    public void commit() {
        // TODO What do we do if one tx fails?
        for (KeycloakTransaction tx : txs) {
            tx.commit();
        }
    }

    @Override
    public void rollback() {
        for (KeycloakTransaction tx : txs) {
            tx.rollback();
        }
    }

    @Override
    public void setRollbackOnly() {
        for (KeycloakTransaction tx : txs) {
            tx.setRollbackOnly();
        }
    }

    @Override
    public boolean getRollbackOnly() {
        for (KeycloakTransaction tx : txs) {
            if (tx.getRollbackOnly()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isActive() {
        for (KeycloakTransaction tx : txs) {
            if (tx.isActive()) {
                return true;
            }
        }
        return false;
    }

}

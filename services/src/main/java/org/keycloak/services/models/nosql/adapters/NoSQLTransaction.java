package org.keycloak.services.models.nosql.adapters;

import org.keycloak.services.models.KeycloakTransaction;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class NoSQLTransaction implements KeycloakTransaction {

    @Override
    public void begin() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void commit() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void rollback() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setRollbackOnly() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean getRollbackOnly() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isActive() {
        return true;
    }
}

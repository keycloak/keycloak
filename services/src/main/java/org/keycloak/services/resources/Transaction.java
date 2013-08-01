package org.keycloak.services.resources;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.services.models.KeycloakSession;
import org.keycloak.services.models.KeycloakTransaction;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class Transaction {
    protected KeycloakSession session;
    protected KeycloakTransaction transaction;
    protected boolean closeSession;

    public Transaction() {
        this(true);
    }

    public Transaction(boolean close) {
        this.session = ResteasyProviderFactory.getContextData(KeycloakSession.class);
        transaction = session.getTransaction();
        closeSession = close;

    }

    protected void runImpl() {

    }

    public void run() {
        boolean wasActive = transaction.isActive();
        if (!wasActive) transaction.begin();
        try {
            runImpl();
            if (!wasActive && transaction.isActive()) transaction.commit();
        } catch (RuntimeException e) {
            if (!wasActive && transaction.isActive()) transaction.rollback();
            throw e;
        } finally {
            if (!wasActive && closeSession) session.close();
        }
    }

    protected <T> T callImpl() {
        return null;
    }

    public <T> T call() {
        boolean wasActive = transaction.isActive();
        if (!wasActive) transaction.begin();
        try {
            T rtn = callImpl();
            if (!wasActive && transaction.isActive()) transaction.commit();
            return rtn;
        } catch (RuntimeException e) {
            if (!wasActive && transaction.isActive()) transaction.rollback();
            throw e;
        } finally {
            if (!wasActive && closeSession) session.close();
        }
    }
}

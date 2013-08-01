package org.keycloak.services.resources;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.services.models.KeycloakSession;
import org.keycloak.services.models.KeycloakSessionFactory;
import org.keycloak.services.models.KeycloakTransaction;

/**
 * Meant to be used as an inner class wrapper (I forget the pattern name, its been awhile).
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class Transaction {
    protected KeycloakSession session;
    protected KeycloakTransaction transaction;
    protected boolean closeSession;

    /**
     * Pull KeycloakSession from @Context
     *
     * Will close session after finished
     *
     */
    public Transaction() {
        this(true);
    }

    /**
     *  Pull KeycloakSession from @Context
     *
     * @param close whether to close the session or not after completion
     */
    public Transaction(boolean close) {
        this.session = ResteasyProviderFactory.getContextData(KeycloakSession.class);
        transaction = session.getTransaction();
        closeSession = close;

    }

    /**
     * Creates and manages its own session.
     *
     * @param factory
     */
    public Transaction(KeycloakSessionFactory factory) {
        this.closeSession = true;
        this.session = factory.createSession();
        this.transaction = session.getTransaction();
    }

    protected void runImpl() {

    }

    /**
     * Will not begin or end a transaction or close a session if the transaction was already active when called
     *
     */
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

    /**
     * Will not begin or end a transaction or close a session if the transaction was already active when called
     *
     */
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

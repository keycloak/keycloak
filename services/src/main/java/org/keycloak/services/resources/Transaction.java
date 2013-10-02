package org.keycloak.services.resources;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;

/**
 * Meant to be used as an inner class wrapper (I forget the pattern name, its been awhile).
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Deprecated
public class Transaction<T> {
    protected KeycloakSession session;
    protected KeycloakTransaction transaction;
    protected boolean closeSession;
    protected boolean created;

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
     * @param close whether to close the session or not after successful completion
     */
    public Transaction(boolean close) {
        this.session = ResteasyProviderFactory.getContextData(KeycloakSession.class);
        this.transaction = session.getTransaction();
        /*
        if (session == null) {
            KeycloakApplication app = (KeycloakApplication)ResteasyProviderFactory.getContextData(Application.class);
            session = app.getFactory().createSession();
            created = true;
            ResteasyProviderFactory.pushContext(KeycloakSession.class, session);
        }
        transaction = session.getTransaction();
        closeSession = close;
        */

    }

    protected void runImpl() {

    }

    /**
     * Will not begin or end a transaction or close a session if the transaction was already active when called
     *
     */
    public void run() {
//        boolean wasActive = transaction.isActive();
//        if (!wasActive) transaction.begin();
//        try {
            runImpl();
//            if (!wasActive && transaction.isActive()) transaction.commit();
//        } catch (RuntimeException e) {
//            if (!wasActive && transaction.isActive()) transaction.rollback();
//            if (created) closeSession = true;
//            throw e;
//        } finally {
//            if (!wasActive && closeSession) {
//                session.close();
//            }
//        }
    }

    protected T callImpl() {
        return null;
    }

    /**
     * Will not begin or end a transaction or close a session if the transaction was already active when called
     *
     */
    public T call() {
//        boolean wasActive = transaction.isActive();
//        if (!wasActive) transaction.begin();
//        try {
            T rtn = callImpl();
//            if (!wasActive && transaction.isActive()) transaction.commit();
            return rtn;
//        } catch (RuntimeException e) {
//            if (!wasActive && transaction.isActive()) transaction.rollback();
//            if (created) closeSession = true; // close if there was a failure
//            throw e;
//        } finally {
//            if (!wasActive && closeSession) session.close();
//        }
    }
}

package org.keycloak.exportimport.util;

import java.io.IOException;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.RealmModel;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ExportImportUtils {

    /**
     * Wrap given runnable job into KeycloakTransaction.
     *
     * @param factory
     * @param job
     */
    public static void runJobInTransaction(KeycloakSessionFactory factory, ExportImportJob job) throws IOException {
        KeycloakSession session = factory.create();
        KeycloakTransaction tx = session.getTransaction();
        try {
            tx.begin();
            job.run(session);

            if (tx.isActive()) {
                if (tx.getRollbackOnly()) {
                    tx.rollback();
                } else {
                    tx.commit();
                }
            }
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            session.close();
        }
    }

    public static String getMasterRealmAdminApplicationName(RealmModel realm) {
        return realm.getName() + "-realm";
    }
}

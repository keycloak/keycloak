package org.keycloak.exportimport.util;

import java.io.IOException;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.RealmModel;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ExportImportUtils {

    /**
     * Wrap given runnable job into KeycloakTransaction. Assumption is that session already exists and it doesn't need to be closed when finished
     *
     * @param session
     * @param job
     */
    public static void runJobInTransaction(KeycloakSession session, ExportImportJob job) throws IOException {
        KeycloakTransaction tx = session.getTransaction();
        try {
            tx.begin();

            job.run();

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
        }
    }

    public static String getMasterRealmAdminApplicationName(RealmModel realm) {
        return realm.getName() + "-realm";
    }
}

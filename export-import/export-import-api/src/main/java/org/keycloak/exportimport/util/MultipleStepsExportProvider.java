package org.keycloak.exportimport.util;

import java.io.IOException;
import java.util.List;

import org.jboss.logging.Logger;
import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.ExportProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class MultipleStepsExportProvider implements ExportProvider {

    protected final Logger logger = Logger.getLogger(getClass());

    @Override
    public void exportModel(final KeycloakSession session) throws IOException {
        final RealmsHolder holder = new RealmsHolder();

        // Import users into same file with realm
        ExportImportUtils.runJobInTransaction(session, new ExportImportJob() {

            @Override
            public void run() {
                List<RealmModel> realms = session.getModel().getRealms();
                holder.realms = realms;
            }

        });

        for (RealmModel realm : holder.realms) {
            exportRealm(session, realm.getName());
        }
    }

    @Override
    public void exportRealm(final KeycloakSession session, final String realmName) throws IOException {
        final int usersPerFile = ExportImportConfig.getUsersPerFile();
        final UsersHolder usersHolder = new UsersHolder();
        final boolean exportUsersIntoSameFile = usersPerFile < 0;

        ExportImportUtils.runJobInTransaction(session, new ExportImportJob() {

            @Override
            public void run() throws IOException {
                RealmModel realm = session.getModel().getRealmByName(realmName);
                RealmRepresentation rep = ExportUtils.exportRealm(realm, exportUsersIntoSameFile);
                writeRealm(realmName + "-realm.json", rep);
                logger.info("Realm '" + realmName + "' - data exported");

                // Count total number of users
                if (!exportUsersIntoSameFile) {
                    // TODO: getUsersCount method on model
                    usersHolder.totalCount = realm.getUsers().size();
                }
            }

        });

        if (!exportUsersIntoSameFile) {

            usersHolder.currentPageStart = 0;

            // usersPerFile==0 means exporting all users into single file (but separate to realm)
            final int countPerPage = usersPerFile == 0 ? usersHolder.totalCount : usersPerFile;

            while (usersHolder.currentPageStart < usersHolder.totalCount) {
                if (usersHolder.currentPageStart + countPerPage < usersHolder.totalCount) {
                    usersHolder.currentPageEnd = usersHolder.currentPageStart + countPerPage;
                } else {
                    usersHolder.currentPageEnd = usersHolder.totalCount;
                }

                ExportImportUtils.runJobInTransaction(session, new ExportImportJob() {

                    @Override
                    public void run() throws IOException {
                        RealmModel realm = session.getModel().getRealmByName(realmName);
                        // TODO: pagination
                        List<UserModel> users = realm.getUsers();
                        usersHolder.users = users.subList(usersHolder.currentPageStart, usersHolder.currentPageEnd);

                        writeUsers(realmName + "-users-" + (usersHolder.currentPageStart / countPerPage) + ".json", realm, usersHolder.users);

                        logger.info("Users " + usersHolder.currentPageStart + "-" + usersHolder.currentPageEnd + " exported");
                    }

                });

                usersHolder.currentPageStart = usersHolder.currentPageEnd;
            }
        }
    }

    protected abstract void writeRealm(String fileName, RealmRepresentation rep) throws IOException;

    protected abstract void writeUsers(String fileName, RealmModel realm, List<UserModel> users) throws IOException;

    public static class RealmsHolder {
        List<RealmModel> realms;

    }

    public static class UsersHolder {
        List<UserModel> users;
        int totalCount;
        int currentPageStart;
        int currentPageEnd;
    }
}

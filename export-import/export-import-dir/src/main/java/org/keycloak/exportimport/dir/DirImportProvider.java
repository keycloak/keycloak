package org.keycloak.exportimport.dir;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.exportimport.ImportProvider;
import org.keycloak.exportimport.Strategy;
import org.keycloak.exportimport.util.ExportImportSessionTask;
import org.keycloak.exportimport.util.ImportUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.util.JsonSerialization;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DirImportProvider implements ImportProvider {

    private static final Logger logger = Logger.getLogger(DirImportProvider.class);

    private final File rootDirectory;

    public DirImportProvider() {
        // Determine system tmp directory
        String tempDir = System.getProperty("java.io.tmpdir");

        // Delete and recreate directory inside tmp
        this.rootDirectory = new File(tempDir + "/keycloak-export");
        if (!this.rootDirectory .exists()) {
            throw new IllegalStateException("Directory " + this.rootDirectory + " doesn't exists");
        }

        logger.infof("Importing from directory %s", this.rootDirectory.getAbsolutePath());
    }

    public DirImportProvider(File rootDirectory) {
        this.rootDirectory = rootDirectory;

        logger.infof("Importing from directory %s", this.rootDirectory.getAbsolutePath());
    }

    @Override
    public void importModel(KeycloakSessionFactory factory, Strategy strategy) throws IOException {
        File[] realmFiles = this.rootDirectory.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return (name.endsWith("-realm.json"));
            }
        });

        List<String> realmNames = new ArrayList<String>();
        for (File file : realmFiles) {
            String fileName = file.getName();
            // Parse "foo" from "foo-realm.json"
            String realmName = fileName.substring(0, fileName.length() - 11);

            // Ensure that master realm is imported first
            if (Config.getAdminRealm().equals(realmName)) {
                realmNames.add(0, realmName);
            } else {
                realmNames.add(realmName);
            }
        }

        for (String realmName : realmNames) {
            importRealm(factory, realmName, strategy);
        }
    }

    @Override
    public void importRealm(KeycloakSessionFactory factory, final String realmName, final Strategy strategy) throws IOException {
        File realmFile = new File(this.rootDirectory + File.separator + realmName + "-realm.json");
        File[] userFiles = this.rootDirectory.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name.matches(realmName + "-users-[0-9]+\\.json");
            }
        });

        // Import realm first
        FileInputStream is = new FileInputStream(realmFile);
        final RealmRepresentation realmRep = JsonSerialization.readValue(is, RealmRepresentation.class);

        KeycloakModelUtils.runJobInTransaction(factory, new ExportImportSessionTask() {

            @Override
            public void runExportImportTask(KeycloakSession session) throws IOException {
                ImportUtils.importRealm(session, realmRep, strategy);
            }

        });

        // Import users
        for (File userFile : userFiles) {
            final FileInputStream fis = new FileInputStream(userFile);
            KeycloakModelUtils.runJobInTransaction(factory, new ExportImportSessionTask() {

                @Override
                protected void runExportImportTask(KeycloakSession session) throws IOException {
                    ImportUtils.importUsersFromStream(session, realmName, JsonSerialization.mapper, fis);
                }
            });
        }
    }

    @Override
    public void close() {

    }
}

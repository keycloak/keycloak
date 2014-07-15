package org.keycloak.exportimport.zip;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.zip.DataFormatException;

import de.idyl.winzipaes.AesZipFileDecrypter;
import de.idyl.winzipaes.impl.AESDecrypter;
import de.idyl.winzipaes.impl.AESDecrypterBC;
import de.idyl.winzipaes.impl.ExtZipEntry;
import org.jboss.logging.Logger;
import org.keycloak.exportimport.ImportProvider;
import org.keycloak.exportimport.Strategy;
import org.keycloak.exportimport.util.ExportImportJob;
import org.keycloak.exportimport.util.ExportImportUtils;
import org.keycloak.exportimport.util.ImportUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ZipImportProvider implements ImportProvider {

    private static final Logger logger = Logger.getLogger(ZipImportProvider.class);

    private final AesZipFileDecrypter decrypter;
    private final String password;

    public ZipImportProvider(File zipFile, String password) {
        try {
            if (!zipFile.exists()) {
                throw new IllegalStateException("File " + zipFile.getAbsolutePath() + " doesn't exists");
            }

            AESDecrypter decrypter = new AESDecrypterBC();
            this.decrypter = new AesZipFileDecrypter(zipFile, decrypter);
            this.password = password;
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        logger.infof("Importing from ZIP file %s", zipFile.getAbsolutePath());
    }

    @Override
    public void importModel(KeycloakSessionFactory factory, Strategy strategy) throws IOException {
        for (ExtZipEntry entry : this.decrypter.getEntryList()) {
            String entryName = entry.getName();
            if (entryName.endsWith("-realm.json")) {
                // Parse "foo" from "foo-realm.json"
                String realmName = entryName.substring(0, entryName.length() - 11);
                importRealm(factory, realmName, strategy);
            }
        }
    }

    @Override
    public void importRealm(KeycloakSessionFactory factory, final String realmName, final Strategy strategy) throws IOException {
        try {
            // Import realm first
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            this.decrypter.extractEntry(this.decrypter.getEntry(realmName + "-realm.json"), bos, this.password);
            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            final RealmRepresentation realmRep = JsonSerialization.mapper.readValue(bis, RealmRepresentation.class);

            ExportImportUtils.runJobInTransaction(factory, new ExportImportJob() {

                @Override
                public void run(KeycloakSession session) throws IOException {
                    ImportUtils.importRealm(session, realmRep, strategy);
                }

            });


            // Import users
            for (ExtZipEntry entry : this.decrypter.getEntryList()) {
                String name = entry.getName();
                if ( (name.startsWith(realmName)) && (name.endsWith(".json")) && (name.substring(realmName.length()).contains("-users-")) ) {
                    bos = new ByteArrayOutputStream();
                    this.decrypter.extractEntry(entry, bos, this.password);
                    final ByteArrayInputStream bis2 = new ByteArrayInputStream(bos.toByteArray());

                    ExportImportUtils.runJobInTransaction(factory, new ExportImportJob() {

                        @Override
                        public void run(KeycloakSession session) throws IOException {
                            ImportUtils.importUsersFromStream(session, realmName, JsonSerialization.mapper, bis2);
                        }
                    });
                }
            }
        } catch (DataFormatException dfe) {
            throw new RuntimeException(dfe);
        }
    }

    @Override
    public void close() {
        try {
            this.decrypter.close();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
}

package org.keycloak.exportimport.zip;

import de.idyl.winzipaes.AesZipFileEncrypter;
import de.idyl.winzipaes.impl.AESEncrypter;
import de.idyl.winzipaes.impl.AESEncrypterBC;
import org.jboss.logging.Logger;
import org.keycloak.Version;
import org.keycloak.exportimport.util.ExportUtils;
import org.keycloak.exportimport.util.MultipleStepsExportProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.util.JsonSerialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ZipExportProvider extends MultipleStepsExportProvider {

    private static final Logger logger = Logger.getLogger(ZipExportProvider.class);

    private final AesZipFileEncrypter encrypter;
    private final String password;

    public ZipExportProvider(File zipFile, String password) {
        if (zipFile.exists()) {
            throw new IllegalStateException("File " + zipFile.getAbsolutePath() + " already exists");
        }
        this.password = password;

        try {
            AESEncrypter encrypter = new AESEncrypterBC();
            this.encrypter = new AesZipFileEncrypter(zipFile, encrypter);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        logger.infof("Exporting into zip file %s", zipFile.getAbsolutePath());
    }

    @Override
    protected void writeRealm(String fileName, RealmRepresentation rep) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        JsonSerialization.mapper.writeValue(stream, rep);
        writeStream(fileName, stream);
    }

    @Override
    protected void writeUsers(String fileName, KeycloakSession session, RealmModel realm, List<UserModel> users) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ExportUtils.exportUsersToStream(session, realm, users, JsonSerialization.mapper, stream);
        writeStream(fileName, stream);
    }

    @Override
    protected void writeVersion(String fileName, Version version) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        JsonSerialization.mapper.writeValue(stream, version);
        writeStream(fileName, stream);
    }

    private void writeStream(String fileName, ByteArrayOutputStream stream) throws IOException {
        byte[] byteArray = stream.toByteArray();
        ByteArrayInputStream bis = new ByteArrayInputStream(byteArray);
        this.encrypter.add(fileName, bis, this.password);
    }

    @Override
    public void close() {
        try {
            this.encrypter.close();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
}

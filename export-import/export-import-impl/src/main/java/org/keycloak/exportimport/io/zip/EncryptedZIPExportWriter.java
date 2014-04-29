package org.keycloak.exportimport.io.zip;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

import de.idyl.winzipaes.AesZipFileEncrypter;
import de.idyl.winzipaes.impl.AESEncrypter;
import de.idyl.winzipaes.impl.AESEncrypterBC;
import org.codehaus.jackson.map.ObjectMapper;
import org.keycloak.exportimport.io.ExportWriter;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class EncryptedZIPExportWriter implements ExportWriter {

    private final File zipFile;
    private final ObjectMapper objectMapper;
    private final String password;

    private final AesZipFileEncrypter encrypter;


    public EncryptedZIPExportWriter(String zipFileName, String password) {
        try {
            this.zipFile = new File(zipFileName);
            if (zipFile.exists()) {
                throw new IllegalStateException("File " + zipFileName + " already exists");
            }

            this.objectMapper = JsonSerialization.mapper;

            AESEncrypter encrypter = new AESEncrypterBC();
            this.encrypter = new AesZipFileEncrypter(this.zipFile, encrypter);
            this.password = password;
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @Override
    public <T> void writeEntities(String fileName, List<T> entities) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            this.objectMapper.writeValue(stream, entities);

            byte[] byteArray = stream.toByteArray();
            this.encrypter.add(fileName, new ByteArrayInputStream(byteArray), this.password);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @Override
    public void closeExportWriter() {
        try {
            this.encrypter.close();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
}

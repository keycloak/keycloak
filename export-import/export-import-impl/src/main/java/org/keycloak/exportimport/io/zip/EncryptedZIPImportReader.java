package org.keycloak.exportimport.io.zip;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

import de.idyl.winzipaes.AesZipFileDecrypter;
import de.idyl.winzipaes.impl.AESDecrypter;
import de.idyl.winzipaes.impl.AESDecrypterBC;
import org.codehaus.jackson.map.ObjectMapper;
import org.keycloak.exportimport.io.ImportReader;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class EncryptedZIPImportReader implements ImportReader {

    private final File zipFile;
    private final ObjectMapper objectMapper;
    private final String password;

    private final AesZipFileDecrypter decrypter;


    public EncryptedZIPImportReader(String zipFileName, String password) {
        try {
            this.zipFile = new File(zipFileName);
            if (!zipFile.exists()) {
                throw new IllegalStateException("File " + zipFileName + " doesn't exists");
            }

            this.objectMapper = JsonSerialization.mapper;

            AESDecrypter decrypter = new AESDecrypterBC();
            this.decrypter = new AesZipFileDecrypter(this.zipFile, decrypter);
            this.password = password;
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @Override
    public <T> List<T> readEntities(String fileName, Class<T> entityClass) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            this.decrypter.extractEntry(this.decrypter.getEntry(fileName), bos, this.password);

            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            T[] template = (T[]) Array.newInstance(entityClass, 0);
            T[] result = (T[])this.objectMapper.readValue(bis, template.getClass());
            return Arrays.asList(result);
        } catch (Exception ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @Override
    public void closeImportReader() {
        try {
            this.decrypter.close();
        } catch (Exception ioe) {
            throw new RuntimeException(ioe);
        }
    }
}

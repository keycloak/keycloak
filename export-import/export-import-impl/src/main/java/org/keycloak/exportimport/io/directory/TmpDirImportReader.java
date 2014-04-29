package org.keycloak.exportimport.io.directory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.jboss.logging.Logger;
import org.keycloak.exportimport.io.ImportReader;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class TmpDirImportReader implements ImportReader {

    private static final Logger logger = Logger.getLogger(TmpDirImportReader.class);

    private final ObjectMapper objectMapper;
    private final File rootDirectory;

    public TmpDirImportReader() {
        // Determine system tmp directory
        String tempDir = System.getProperty("java.io.tmpdir");

        // Delete and recreate directory inside tmp
        this.rootDirectory = new File(tempDir + "/keycloak-export");
        if (!this.rootDirectory .exists()) {
            throw new IllegalStateException("Directory " + this.rootDirectory + " doesn't exists");
        }

        logger.infof("Importing from directory %s", this.rootDirectory.getAbsolutePath());
        this.objectMapper = getObjectMapper();
    }

    public TmpDirImportReader(File rootDirectory) {
        this.rootDirectory = rootDirectory;
        this.objectMapper = getObjectMapper();

        logger.infof("Importing from directory %s", this.rootDirectory.getAbsolutePath());
    }

    private ObjectMapper getObjectMapper() {
        return JsonSerialization.prettyMapper;
    }

    @Override
    public <T> List<T> readEntities(String fileName, Class<T> entityClass) {
        try {
            File file = new File(this.rootDirectory, fileName);
            FileInputStream stream = new FileInputStream(file);
            T[] template = (T[]) Array.newInstance(entityClass, 0);
            T[] result = (T[])this.objectMapper.readValue(stream, template.getClass());
            return Arrays.asList(result);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @Override
    public void closeImportReader() {
        //TODO: Should directory be deleted after import?
    }
}

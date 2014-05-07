package org.keycloak.exportimport.io.directory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.jboss.logging.Logger;
import org.keycloak.exportimport.io.ExportWriter;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class TmpDirExportWriter implements ExportWriter {

    private static final Logger logger = Logger.getLogger(TmpDirExportWriter.class);

    private final ObjectMapper objectMapper;
    private final File rootDirectory;

    public TmpDirExportWriter() {
        // Determine system tmp directory
        String tempDir = System.getProperty("java.io.tmpdir");

        // Delete and recreate directory inside tmp
        this.rootDirectory = new File(tempDir + "/keycloak-export");
        if (this.rootDirectory .exists()) {
            recursiveDeleteDir(this.rootDirectory );
        }
        this.rootDirectory.mkdirs();

        logger.infof("Exporting into directory %s", this.rootDirectory.getAbsolutePath());
        this.objectMapper = getObjectMapper();
    }

    public TmpDirExportWriter(File rootDirectory) {
        this.rootDirectory = rootDirectory;
        this.rootDirectory.mkdirs();
        this.objectMapper = getObjectMapper();

        logger.infof("Exporting into directory %s", this.rootDirectory.getAbsolutePath());
    }

    private ObjectMapper getObjectMapper() {
        return JsonSerialization.prettyMapper;
    }

    protected boolean recursiveDeleteDir(File dirPath) {
        if (dirPath.exists()) {
            File[] files = dirPath.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    recursiveDeleteDir(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        if (dirPath.exists())
            return dirPath.delete();
        else
            return true;
    }

    @Override
    public <T> void writeEntities(String fileName, List<T> entities) {
        try {
            File file = new File(this.rootDirectory, fileName);
            FileOutputStream stream = new FileOutputStream(file);
            this.objectMapper.writeValue(stream, entities);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @Override
    public void closeExportWriter() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}

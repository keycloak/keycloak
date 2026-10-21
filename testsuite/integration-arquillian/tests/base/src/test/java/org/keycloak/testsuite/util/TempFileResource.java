package org.keycloak.testsuite.util;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class TempFileResource implements Closeable {

    private File file;

    public TempFileResource(String filepath) {
        file = new File(filepath);
    }

    public TempFileResource(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    public String getName() {
        return file.getName();
    }

    public String getAbsolutePath() {
        return file.getAbsolutePath();
    }

    public boolean isFile() {
        return file.isFile();
    }

    @Override
    public void close() throws IOException {
        // delete file if it exists
        file.delete();
    }
}

package org.keycloak.encoding;

import org.apache.commons.io.IOUtils;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.GZIPOutputStream;

public class GzipResourceEncodingProvider implements ResourceEncodingProvider {

    private static final Logger logger = Logger.getLogger(ResourceEncodingProvider.class);

    private KeycloakSession session;
    private File cacheDir;

    public GzipResourceEncodingProvider(KeycloakSession session, File cacheDir) {
        this.session = session;
        this.cacheDir = cacheDir;
    }

    public InputStream getEncodedStream(StreamSupplier producer, String... path) {
        StringBuilder sb = new StringBuilder();
        sb.append(cacheDir.getAbsolutePath());
        for (String p : path) {
            sb.append(File.separatorChar);
            sb.append(p);
        }
        sb.append(".gz");

        String filePath = sb.toString();

        try {
            File encodedFile = new File(filePath);
            if (!encodedFile.getCanonicalPath().startsWith(cacheDir.getCanonicalPath())) {
                return null;
            }

            if (!encodedFile.exists()) {
                InputStream is = producer.getInputStream();
                if (is != null) {
                    File parent = encodedFile.getParentFile();
                    if (!parent.isDirectory()) {
                        parent.mkdirs();
                    }
                    FileOutputStream fos = new FileOutputStream(encodedFile);
                    GZIPOutputStream gos = new GZIPOutputStream(fos);
                    IOUtils.copy(is, gos);
                    gos.close();
                    is.close();
                } else {
                    encodedFile = null;
                }
            }

            return encodedFile != null ? new FileInputStream(encodedFile) : null;
        } catch (Exception e) {
            logger.warn("Failed to encode resource", e);
            return null;
        }
    }

    public String getEncoding() {
        return "gzip";
    }

}

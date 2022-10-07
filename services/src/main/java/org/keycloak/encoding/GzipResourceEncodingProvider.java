package org.keycloak.encoding;

import java.io.IOException;
import java.nio.file.Files;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
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
                    File tmpEncodedFile = File.createTempFile(
                            encodedFile.getName(),
                            "tmp",
                            parent);

                    FileOutputStream fos = new FileOutputStream(tmpEncodedFile);
                    GZIPOutputStream gos = new GZIPOutputStream(fos);
                    IOUtils.copy(is, gos);
                    gos.close();
                    is.close();
                    try {
                        Files.move(
                                tmpEncodedFile.toPath(),
                                encodedFile.toPath(),
                                REPLACE_EXISTING);
                    } catch ( IOException io ) {
                        logger.warnf("Fail to move %s  %s", tmpEncodedFile.toString(), io);
                        if (!encodedFile.exists()) {
                            encodedFile = null;
                        }
                    }
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

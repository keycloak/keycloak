package org.keycloak.encoding;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.zip.GZIPOutputStream;

import org.keycloak.theme.ResourceLoader;

import org.apache.commons.io.IOUtils;
import org.jboss.logging.Logger;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class GzipResourceEncodingProvider implements ResourceEncodingProvider {

    private static final Logger logger = Logger.getLogger(ResourceEncodingProvider.class);

    private final File cacheDir;

    public GzipResourceEncodingProvider(File cacheDir) {
        this.cacheDir = cacheDir;
    }

    public InputStream getEncodedStream(StreamSupplier producer, String... path) {
        try {
            File encodedFile = ResourceLoader.getFile(cacheDir, String.join("/", path) +  ".gz");
            if (encodedFile == null) {
                return null;
            }

            if (!encodedFile.exists()) {
                encodedFile = createEncodedFile(producer, encodedFile);
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

    private File createEncodedFile(StreamSupplier producer, File target) throws IOException {
        InputStream is = producer.getInputStream();
        if (is == null) {
            return null;
        }

        File parent = target.getParentFile();
        if (!parent.isDirectory()) {
            if (parent.mkdirs() && !parent.isDirectory()) {
                logger.warnf("Fail to create cache directory %s", parent.toString());
            }
        }
        File tmpEncodedFile = File.createTempFile(target.getName(), "tmp", parent);

        try (is; GZIPOutputStream gos = new GZIPOutputStream(new FileOutputStream(tmpEncodedFile))) {
            IOUtils.copy(is, gos);
        }

        try {
            Files.move(tmpEncodedFile.toPath(), target.toPath(), REPLACE_EXISTING);
            return target;
        } catch (IOException io) {
            logger.warnf(io, "Fail to move temporary file to %s", target.toString());
            return null;
        }
    }

}

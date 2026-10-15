package org.keycloak.encoding;

import java.io.ByteArrayInputStream;
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

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;

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

            if (encodedFile.exists()) {
                return new FileInputStream(encodedFile);
            }

            return createEncodedStream(producer, encodedFile);
        } catch (Exception e) {
            logger.warn("Failed to encode resource", e);
            return null;
        }
    }

    public String getEncoding() {
        return "gzip";
    }

    private InputStream createEncodedStream(StreamSupplier producer, File target) throws IOException {
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

        // Publish only by atomic rename, so a cached file that exists is always complete
        try {
            Files.move(tmpEncodedFile.toPath(), target.toPath(), ATOMIC_MOVE);
        } catch (IOException io) {
            try {
                if (!target.isFile()) {
                    logger.warnf(io, "Failed to move temporary file to %s, serving it uncached", target.toString());
                    return new ByteArrayInputStream(Files.readAllBytes(tmpEncodedFile.toPath()));
                }
                // An existing target that could not be replaced (e.g. on Windows while another
                // request reads it) holds the same content
                logger.debugf(io, "Failed to replace %s, serving the existing file", target.toString());
            } finally {
                // File.delete() does not throw, so a failed cleanup cannot fail the request
                tmpEncodedFile.delete();
            }
        }
        return new FileInputStream(target);
    }

}

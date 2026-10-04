package org.keycloak.encoding;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
            File encodedFile = ResourceLoader.getFile(cacheDir, String.join("/", path) + ".gz");
            if (encodedFile == null) {
                return null;
            }

            // retry once: a concurrent clearCache() might remove or disrupt the file between the exists() check
            // below and opening the stream, or while it is being (re-)created
            for (int attempt = 0; attempt < 2; attempt++) {
                File file;
                try {
                    file = encodedFile.exists() ? encodedFile : createEncodedFile(producer, encodedFile);
                } catch (IOException e) {
                    logger.debugf("Failed to create encoded resource %s concurrently, retrying", encodedFile);
                    continue;
                }
                if (file == null) {
                    return null;
                }
                try {
                    return new FileInputStream(file);
                } catch (FileNotFoundException e) {
                    logger.debugf("Encoded resource %s was removed concurrently, retrying", file);
                }
            }
            return null;
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

        Files.move(tmpEncodedFile.toPath(), target.toPath(), REPLACE_EXISTING);
        return target;
    }

}

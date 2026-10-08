package org.keycloak.encoding;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.keycloak.Config;
import org.keycloak.common.Version;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.services.resources.KeycloakApplication;

import org.apache.commons.io.FileUtils;
import org.jboss.logging.Logger;

public class GzipResourceEncodingProviderFactory implements ResourceEncodingProviderFactory {

    private static final Logger logger = Logger.getLogger(GzipResourceEncodingProviderFactory.class);

    private Set<String> excludedContentTypes = new HashSet<>();

    private volatile File cacheDir;
    private volatile File previousCacheDir;
    private final AtomicLong generation = new AtomicLong();

    @Override
    public ResourceEncodingProvider create(KeycloakSession session) {
        if (cacheDir == null) {
            cacheDir = initCacheDir();
        }

        return new GzipResourceEncodingProvider(cacheDir);
    }

    @Override
    public void init(Config.Scope config) {
        String e = config.get("excludedContentTypes", "image/png image/jpeg");
        excludedContentTypes.addAll(Arrays.asList(e.split(" ")));
    }

    @Override
    public boolean encodeContentType(String contentType) {
        return !excludedContentTypes.contains(contentType);
    }

    @Override
    public String getId() {
        return "gzip";
    }

    @Override
    public synchronized void clearCache() {
        File prev = previousCacheDir;
        if (prev != null) {
            deleteDirectoryQuietly(prev);
        }

        // current dir becomes previous — in-flight providers may still write to it
        previousCacheDir = cacheDir;
        cacheDir = createCacheDir();
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name("excludedContentTypes")
                .type("string")
                .helpText("A space separated list of content-types to exclude from encoding.")
                .defaultValue("image/png image/jpeg")
                .add()
                .build();
    }

    private synchronized File initCacheDir() {
        if (cacheDir != null) {
            return cacheDir;
        }

        // clean up all directories from previous runs or clearCache() generations
        File cacheRoot = cacheRoot();
        if (cacheRoot.isDirectory()) {
            File[] files = cacheRoot.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteDirectoryQuietly(f);
                }
            }
        }

        return createCacheDir();
    }

    private File cacheRoot() {
        return new File(KeycloakApplication.getTmpDirectory(), "kc-gzip-cache");
    }

    private File createCacheDir() {
        // counter avoids directory name collisions when clearCache() is called twice within the same millisecond
        File dir = new File(cacheRoot(), Version.RESOURCES_VERSION + "-" + Time.currentTimeMillis() + "-" + generation.incrementAndGet());
        dir.mkdirs();
        if (!dir.isDirectory()) {
            logger.warn("Failed to create gzip cache directory " + dir.getAbsolutePath());
            return null;
        }
        return dir;
    }

    private void deleteDirectoryQuietly(File dir) {
        try {
            FileUtils.deleteDirectory(dir);
        } catch (IOException e) {
            logger.warn("Failed to delete gzip cache directory", e);
        }
    }
}

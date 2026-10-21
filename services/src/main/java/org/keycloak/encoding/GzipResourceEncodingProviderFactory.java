package org.keycloak.encoding;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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

    @Override
    public ResourceEncodingProvider create(KeycloakSession session) {
        if (cacheDir == null) {
            synchronized (this) {
                if (cacheDir == null) {
                    cacheDir = initCacheDir();
                }
            }
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

        File newDir = createCacheDir();
        if (newDir != null) {
            // only rotate if the new directory was actually created; otherwise keep using the existing
            // cacheDir rather than leaving it null, which would wrongly re-trigger the full-wipe startup path
            previousCacheDir = cacheDir;
            cacheDir = newDir;
        }
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

    private File initCacheDir() {
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
        // a random UUID (rather than an in-process counter) avoids collisions even across multiple instances
        // that happen to share the same cache root
        File dir = new File(cacheRoot(), Version.RESOURCES_VERSION + "-" + Time.currentTimeMillis() + "-" + UUID.randomUUID());
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

package org.keycloak.encoding;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.keycloak.Config;
import org.keycloak.common.Version;
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

    @Override
    public ResourceEncodingProvider create(KeycloakSession session) {
        File dir = cacheDir;
        if (dir == null) {
            dir = initCacheDir();
        }

        return new GzipResourceEncodingProvider(dir);
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

        File cacheRoot = new File(KeycloakApplication.getTmpDirectory(), "kc-gzip-cache");
        File dir = new File(cacheRoot, Version.RESOURCES_VERSION);

        if (cacheRoot.isDirectory()) {
            // Also clear the cache of the current resources version, as theme resources might have changed since the
            // cache was written. This matches the lifecycle of the in-memory theme cache, which is cleared on restart.
            for (File f : cacheRoot.listFiles()) {
                try {
                    FileUtils.deleteDirectory(f);
                } catch (IOException e) {
                    logger.warn("Failed to delete gzip cache directory", e);
                }
            }
        }

        if (dir.exists()) {
            // The previous cache could not be fully deleted, so it might still contain stale entries. Use a fresh
            // directory instead, which is removed together with the rest of the cache on the next startup.
            try {
                dir = Files.createTempDirectory(cacheRoot.toPath(), Version.RESOURCES_VERSION + "-").toFile();
            } catch (IOException e) {
                logger.warn("Failed to create gzip cache directory in " + cacheRoot.getAbsolutePath(), e);
                return null;
            }
        }

        dir.mkdirs();
        if (!dir.isDirectory()) {
            logger.warn("Failed to create gzip cache directory " + dir.getAbsolutePath());
            return null;
        }

        // published while holding the lock, so a concurrent first request cannot clear the directory again
        cacheDir = dir;
        return dir;
    }
}

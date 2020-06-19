package org.keycloak.services.filters;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.TeeOutputStream;
import org.jboss.resteasy.plugins.interceptors.encoding.GZIPEncodingInterceptor;
import org.jboss.resteasy.resteasy_jaxrs.i18n.LogMessages;
import org.keycloak.common.Profile;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptorContext;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.io.FileUtils.deleteDirectory;

@Provider
@Priority(Priorities.ENTITY_CODER)
public class CachingGZIPEncodingInterceptor extends GZIPEncodingInterceptor {
    private static final List<String> EXCLUDED_EXTENSIONS = Arrays.asList("jpg", "png");
    private Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"),"gzip-cache");
    @Context
    private UriInfo uriInfo;

    public CachingGZIPEncodingInterceptor() {
        try {
            if (!this.tmpDir.toFile().exists()) Files.createDirectory(this.tmpDir);
        } catch (IOException e) {
            LogMessages.LOGGER.warn("could not create temp gzip cache directory, disabling gzip support", e);
        }
    }

    public static class CacheCommittedGZIPOutputStream extends CommittedGZIPOutputStream {
        protected CacheCommittedGZIPOutputStream(final OutputStream delegate) {
            super(delegate, null);
        }
    }

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
        LogMessages.LOGGER.tracef("Interceptor : %s,  Method : aroundWriteTo", getClass().getName());

        Object encoding = context.getHeaders().getFirst(HttpHeaders.CONTENT_ENCODING);
        final boolean featureEnabled = Profile.isFeatureEnabled(Profile.Feature.GZIP);
        final String fileName = uriInfo.getPath();

        if (encoding != null
                && encoding.toString().equalsIgnoreCase("gzip")
                && featureEnabled
                && tmpDir.toFile().exists()
                && !EXCLUDED_EXTENSIONS.contains(FilenameUtils.getExtension(fileName))) {
            OutputStream old = context.getOutputStream();
            cleanOutOldVersionCache();

            final Path zipFile = Paths.get(tmpDir.toString(), fileName);

            if (zipFile.toFile().exists()) {
                context.setOutputStream(new ByteArrayOutputStream());
                context.proceed();

                final FileInputStream inputStream = new FileInputStream(zipFile.toFile());
                IOUtils.copy(inputStream, old);
                inputStream.close();
            } else {
                Files.createDirectories(zipFile.getParent());
                final FileOutputStream fileOutputStream = new FileOutputStream(Files.createFile(zipFile).toFile());
                TeeOutputStream teeOutputStream = new TeeOutputStream(old, fileOutputStream);
                // GZIPOutputStream constructor writes to underlying OS causing headers to be written.
                CacheCommittedGZIPOutputStream gzipOutputStream = new CacheCommittedGZIPOutputStream(teeOutputStream);

                context.setOutputStream(gzipOutputStream);

                try {
                    context.proceed();
                } finally {
                    if (gzipOutputStream.getGzip() != null) gzipOutputStream.getGzip().finish();
                    context.setOutputStream(old);
                }
            }
        } else {
            context.getHeaders().remove(HttpHeaders.CONTENT_ENCODING);
            context.proceed();
        }
    }

    private void cleanOutOldVersionCache() {
        final String version = uriInfo.getPathParameters().getFirst("version");
        final String pathSegment = uriInfo.getPathSegments().get(0).getPath();
        final Path path = Paths.get(tmpDir.toString(), pathSegment);
        final String[] list = path.toFile().list((file, s) -> !s.equals(version));
        if (list != null) {
            for (String oldVersion : list) {
                try {
                    synchronized (this) {
                        deleteDirectory(Paths.get(path.toString(), oldVersion).toFile());
                    }
                } catch (IOException e) {
                    LogMessages.LOGGER.warnf("Could not delete old cache gzip version: '%s'", oldVersion);
                }
            }
        }
    }
}

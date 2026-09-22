package org.keycloak.theme;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;

public class ResourceLoader {

    public static InputStream getResourceAsStream(String root, String resource) throws IOException {
        URL url = getResource(classLoader(), root, resource);
        return url != null ? url.openStream() : null;
    }

    public static URL getResource(ClassLoader classLoader, String root, String resource) {
        if (classLoader == null || root == null || resource == null) {
            return null;
        }
        Path rootPath = Path.of("/", root).normalize().toAbsolutePath();
        Path resourcePath = rootPath.resolve(resource).normalize().toAbsolutePath();
        if (resourcePath.startsWith(rootPath)) {
            String contained;
            if (File.separatorChar == '/') {
                contained = resourcePath.toString().substring(1);
            } else {
                contained = resourcePath.toString().substring(2).replace('\\', '/');
            }
            return classLoader.getResource(contained);
        } else {
            return null;
        }
    }

    public static InputStream getFileAsStream(File root, String resource) throws IOException {
        File file = getFile(root, resource);
        return file != null && file.isFile() ? file.toURI().toURL().openStream() : null;
    }

    public static File getFile(File root, String resource) throws IOException {
        if (root == null || resource == null) {
            return null;
        }
        Path rootPath = root.toPath().toAbsolutePath().normalize();
        Path resourcePath = rootPath.resolve(resource).normalize().toAbsolutePath();
        if (resourcePath.startsWith(rootPath)) {
            return resourcePath.toFile();
        } else {
            return null;
        }
    }

    private static ClassLoader classLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

}

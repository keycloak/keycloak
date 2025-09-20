package org.keycloak.theme;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;

public class ResourceLoader {

    public static InputStream getResourceAsStream(String root, String resource) throws IOException {
        if (root == null || resource == null) {
            return null;
        }
        
        // Check for encoded traversal patterns in the original resource
        if (containsEncodedTraversal(resource)) {
            return null;
        }

        // Decode resource repeatedly to catch double/triple encoded traversal attempts
        String previousResource;
        do {
            previousResource = resource;
            resource = java.net.URLDecoder.decode(resource, java.nio.charset.StandardCharsets.UTF_8);
        } while (!resource.equals(previousResource)); // Keep decoding until no change

        // Use relative paths for proper normalization without absolute paths
        Path rootPath = Path.of(root).normalize();
        Path resourcePath = rootPath.resolve(resource).normalize();

        // Check for directory traversal by ensuring the normalized path is still within root
        if (!resourcePath.startsWith(rootPath)) {
            return null;
        }

        // For classloader, use the normalized resource path
        String resourceString = resourcePath.toString().replace('\\', '/');
        URL url = classLoader().getResource(resourceString);
        return url != null ? url.openStream() : null;
    }
    
    private static boolean containsEncodedTraversal(String resource) {
        // Check for various encoded forms of "../"
        return resource.contains("%2E%2E%2F") ||  // ../
               resource.contains("%2e%2e%2f") ||  // ../ (lowercase)
               resource.contains("%252E%252E%252F") || // double-encoded ../
               resource.contains("%252e%252e%252f");   // double-encoded ../ (lowercase)
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

package org.keycloak.testsuite.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is a copy of
 * <a href="https://github.com/quarkusio/quarkus/blob/f3f55df9cab105d92525e7165cd43670d03b1ad9/core/deployment/src/main/java/io/quarkus/deployment/util/FileUtil.java">The quarkus FileUtil implementation</a>
 * so we don't need to import the whole module.
 */
public class FileUtil {

    public static void deleteIfExists(final Path path) throws IOException {
        BasicFileAttributes attributes;
        try {
            attributes = Files.readAttributes(path, BasicFileAttributes.class);
        } catch (IOException ignored) {
            // Files.isDirectory is also simply returning when any IOException occurs, same behaviour is fine
            return;
        }
        if (attributes.isDirectory()) {
            deleteDirectoryIfExists(path);
        } else if (attributes.isRegularFile()) {
            Files.deleteIfExists(path);
        }
    }

    public static void deleteDirectory(final Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return;
        }
        deleteDirectoryIfExists(directory);
    }

    private static void deleteDirectoryIfExists(final Path directory) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                try {
                    Files.delete(file);
                } catch (IOException e) {
                    // ignored
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                try {
                    Files.delete(dir);
                } catch (IOException e) {
                    // ignored
                }
                return FileVisitResult.CONTINUE;
            }

        });
    }

    public static byte[] readFileContents(InputStream inputStream) throws IOException {
        return inputStream.readAllBytes();
    }

    /**
     * Translates a file path from the Windows Style to a syntax accepted by Docker and Podman,
     * so that volumes be safely mounted in both Docker Desktop for Windows and Podman Windows.
     * <p>
     * <code>docker run -v /c/foo/bar:/somewhere (...)</code>
     * <p>
     * You should only use this method on Windows-style paths, and not Unix-style
     * paths.
     *
     * @param windowsStylePath A path formatted in Windows-style, e.g. "C:\foo\bar".
     * @return A translated path accepted by Docker, e.g. "/c/foo/bar".
     */
    public static String translateToVolumePath(String windowsStylePath) {
        String translated = windowsStylePath.replace('\\', '/');
        Pattern p = Pattern.compile("^(\\w)(?:$|:(/)?(.*))");
        Matcher m = p.matcher(translated);
        if (m.matches()) {
            String slash = Optional.ofNullable(m.group(2)).orElse("/");
            String path = Optional.ofNullable(m.group(3)).orElse("");
            return "/" + m.group(1).toLowerCase() + slash + path;
        }
        return translated;
    }
}

package org.keycloak.db.compatibility.verifier;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

record KeycloakMigrationParser(ClassLoader classLoader, String packageName) {
    Set<Migration> discoverAllMigrations() throws IOException {
        return findAllClassNamesInPackage(classLoader, packageName)
              .filter(s -> {
                  var parts = s.split("\\.");
                  var clazz = parts[parts.length - 1];
                  // Ignore anonymous/lambda/inner classes
                  return !clazz.contains("$");
              })
              .map(Migration::new)
              .collect(Collectors.toSet());
    }

    private Stream<String> findAllClassNamesInPackage(ClassLoader classLoader, String packageName) throws IOException {
         if (packageName == null) {
             return Stream.of();
         }

        List<String> classNames = new ArrayList<>();
        String path = packageName.replace('.', '/');

        Enumeration<URL> resources = classLoader.getResources(path);
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();

            if (resource.getProtocol().equals("file")) {
                URI uri;
                try {
                    uri = resource.toURI();
                } catch (URISyntaxException e) {
                    // Should never happen
                    throw new IllegalStateException(e);
                }
                classNames.addAll(findNamesInDirectory(new File(uri), packageName));
            } else if (resource.getProtocol().equals("jar")) {
                classNames.addAll(findNamesInJar(resource, path));
            }
        }
        return classNames.stream();
    }

    // Helper for file system (IDE)
    private static List<String> findNamesInDirectory(File directory, String packageName) {
        List<String> classNames = new ArrayList<>();
        if (!directory.exists()) {
            return classNames;
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // Recursive scan
                    classNames.addAll(findNamesInDirectory(file, packageName + "." + file.getName()));
                } else if (file.getName().endsWith(".class")) {
                    // Just strip the extension and append to package
                    String className = packageName + "." + file.getName().substring(0, file.getName().length() - 6);
                    classNames.add(className);
                }
            }
        }
        return classNames;
    }

    // Helper for JAR files (Maven)
    private static List<String> findNamesInJar(URL resource, String packagePath) throws IOException {
        List<String> classNames = new ArrayList<>();

        JarURLConnection jarConn = (JarURLConnection) resource.openConnection();
        try (JarFile jarFile = jarConn.getJarFile()) {
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();

                // Check if it matches the package path and is a class file
                // We add a "/" to the packagePath check to ensure we don't accidentally match "com/tester" when searching for "com/test"
                if (entryName.startsWith(packagePath + "/") && entryName.endsWith(".class")) {

                    // Convert path "com/example/MyClass.class" -> "com.example.MyClass"
                    String className = entryName.replace('/', '.').substring(0, entryName.length() - 6);
                    classNames.add(className);
                }
            }
        }
        return classNames;
    }
}

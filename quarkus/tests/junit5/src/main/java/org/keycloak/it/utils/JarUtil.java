package org.keycloak.it.utils;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.keycloak.it.TestProvider;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;


public class JarUtil {

    public static Path getProvidersTargetPath(TestProvider provider) {
        String fullPathUrl = provider.getClasses()[0].getResource(".").toString();
        String providersTargetPath = provider.getClasses()[0].getPackageName().replace('.', '/').concat("/");
        URL pathUrl;
        try {
            pathUrl = new URL(fullPathUrl.replace(providersTargetPath, ""));
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid package provider path", e);
        }

        File fileUri;
        try {
            fileUri = new File(pathUrl.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid package provider path", e);
        }
        return Paths.get(fileUri.getPath());
    }

    /**
     * Creates a Java Archive with a provider that should be able to load when placed in the <code>providers</code> dir of a kc distribution
     * @param provider
     * @param providersTargetPath The path to the "root" of the compiled provider classes
     * @param jarPath Where the JAR is supposed to be saved.
     * @return The path of the created JAR. Basically <code>jarPath</code> + <i>jarName</i>.jar
     */
    public static Path createProviderJar(TestProvider provider, Path providersTargetPath, Path jarPath) {
        String jarName =  provider.getName() + ".jar";
        JavaArchive providerJar = ShrinkWrap.create(JavaArchive.class, jarName)
                .addClasses(provider.getClasses());
        Map<String, String> manifestResources = provider.getManifestResources();

        for (Map.Entry<String, String> resource : manifestResources.entrySet()) {
            try {
                providerJar.addAsManifestResource(providersTargetPath.resolve("META-INF/" + resource.getKey()).toFile(), resource.getValue());
            } catch (Exception cause) {
                throw new RuntimeException("Failed to add manifest resource: " + resource.getKey(), cause);
            }
        }

        providerJar.as(ZipExporter.class).exportTo(jarPath.resolve(providerJar.getName()).toFile(), true);
        return jarPath.resolve(jarName);
    }
}

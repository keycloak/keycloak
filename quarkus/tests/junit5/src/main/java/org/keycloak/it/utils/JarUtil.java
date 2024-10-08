package org.keycloak.it.utils;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.keycloak.it.TestProvider;

import java.nio.file.Path;
import java.util.Map;


public class JarUtil {
    /**
     * Creates a Java Archive with a provider that should be able to load when placed in the <code>providers</code> dir of a kc distribution
     * @param provider
     * @param providerPackagePath
     * @param jarPath Where the JAR is supposed to be saved.
     * @return The path of the created JAR. Basically <code>jarPath</code> + <i>jarName</i>.jar
     */
    public static Path createProviderJar(TestProvider provider, Path providerPackagePath, Path jarPath) {
        String jarName =  provider.getName() + ".jar";
        JavaArchive providerJar = ShrinkWrap.create(JavaArchive.class, jarName)
                .addClasses(provider.getClasses());
        Map<String, String> manifestResources = provider.getManifestResources();

        for (Map.Entry<String, String> resource : manifestResources.entrySet()) {
            try {
                providerJar.addAsManifestResource(providerPackagePath.resolve("META-INF/beans.xml").toFile(), "beans.xml");
                providerJar.addAsManifestResource(providerPackagePath.resolve("META-INF/services/" + resource.getKey()).toFile(), resource.getValue());
            } catch (Exception cause) {
                throw new RuntimeException("Failed to add manifest resource: " + resource.getKey(), cause);
            }
        }

        providerJar.as(ZipExporter.class).exportTo(jarPath.resolve(providerJar.getName()).toFile(), true);
        return jarPath.resolve(jarName);
    }
}

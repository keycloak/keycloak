package org.keycloak.test.framework.server;

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
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface KeycloakTestServer {

    void start(List<String> rawOptions, Set<Class<? extends TestProvider>> providerModules);

    void stop();

    String getBaseUrl();

    default <T> T createProviderJar(TestProvider provider, Class<T> returnType) {
        String fullPathUrl = provider.getClasses()[0].getResource(".").toString();
        String packagePath = provider.getClasses()[0].getPackageName().replace('.', '/').concat("/");
        URL pathUrl;
        try {
            pathUrl = new URL(fullPathUrl.replace(packagePath, ""));
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid package provider path", e);
        }

        File fileUri;
        try {
            fileUri = new File(pathUrl.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid package provider path", e);
        }
        Path providerPackagePath = Paths.get(fileUri.getPath());
        JavaArchive providerJar = ShrinkWrap.create(JavaArchive.class, provider.getName() + ".jar")
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

        if (returnType == Path.class) {
            Path jarPath = providerPackagePath.getParent().resolve(providerJar.getName());
            providerJar.as(ZipExporter.class).exportTo(jarPath.toFile(), true);
            return returnType.cast(jarPath);
        }
        else if (returnType == JavaArchive.class) {
            return returnType.cast(providerJar);
        }
        else {
            throw new IllegalArgumentException("Invalid return type for a test provider deployment");
        }
    }

}

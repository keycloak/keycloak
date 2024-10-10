package org.keycloak.test.framework.server;

import org.keycloak.Keycloak;
import org.keycloak.common.Version;
import org.keycloak.it.TestProvider;
import org.keycloak.it.utils.JarUtil;
import org.keycloak.test.framework.util.ServerUtil;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeoutException;
public class EmbeddedKeycloakTestServer implements KeycloakTestServer {

    private Keycloak keycloak;

    @Override
    public void start(List<String> rawOptions, List<? extends TestProvider> customProviders) {
        var builder = Keycloak.builder().setVersion(Version.VERSION);
        for(var provider : customProviders) {
            Path providerJarPath = createProviderJar(provider);
            builder.addAdditionalDeploymentArchive(providerJarPath);
        }

        keycloak = builder.start(rawOptions);
    }

    @Override
    public void stop() {
        try {
            keycloak.stop();
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getBaseUrl() {
        return "http://localhost:8080";
    }

    public Path createProviderJar(TestProvider provider) {
        Path providerPackagePath = ServerUtil.getProviderPackagePath(provider);
        Path jarPath = providerPackagePath.getParent()/*.resolve(provider.getName())*/;
        return JarUtil.createProviderJar(provider, providerPackagePath, jarPath);
    }

}

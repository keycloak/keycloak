import org.keycloak.test.framework.server.ProviderModule;

public class MyProviderModule implements ProviderModule {
    @Override
    public String groupId() {
        return "org.keycloak.test";
    }

    @Override
    public String artifactId() {
        return "custom-providers";
    }

    @Override
    public String version() {
        return "999.0.0-SNAPSHOT";
    }
}

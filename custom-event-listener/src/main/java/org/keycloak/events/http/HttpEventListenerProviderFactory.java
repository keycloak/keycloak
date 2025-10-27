package org.keycloak.events.http;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;

/**
 * Factory for creating HTTP Event Listener Provider instances
 */
public class HttpEventListenerProviderFactory implements EventListenerProviderFactory {

    public static final String PROVIDER_ID = "http-event-listener";
    
    private String targetUrl;
    private String authToken;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new HttpEventListenerProvider(targetUrl, authToken);
    }

    @Override
    public void init(Config.Scope config) {
        targetUrl = config.get("target-url");
        authToken = config.get("auth-token");
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // Nothing to do
    }

    @Override
    public void close() {
        // Nothing to close
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name("target-url")
                .type("string")
                .helpText("The HTTP endpoint URL to send events to (e.g., http://localhost:3000/webhook)")
                .defaultValue("")
                .add()
                .property()
                .name("auth-token")
                .type("string")
                .helpText("Optional Bearer token for authentication")
                .defaultValue("")
                .secret(true)
                .add()
                .build();
    }
}


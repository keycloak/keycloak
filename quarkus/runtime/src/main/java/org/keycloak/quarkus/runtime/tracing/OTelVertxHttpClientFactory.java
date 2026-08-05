package org.keycloak.quarkus.runtime.tracing;

import java.util.Set;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.config.TracingOptions;
import org.keycloak.provider.Provider;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.httpclient.VertxHttpClientFactory;
import org.keycloak.tracing.TracingProvider;

/**
 * Traced variant of {@link VertxHttpClientFactory} that activates when both HTTP_CLIENT_V2
 * and OpenTelemetry tracing are enabled.
 *
 * Unlike {@link OTelHttpClientFactory} (which must explicitly wrap the Apache HTTP client),
 * Quarkus auto-instruments Vert.x HTTP client via its OpenTelemetry extension. This factory
 * exists primarily for provider ordering — ensuring TracingProvider initializes before the
 * HTTP client when both features are active.
 */
public class OTelVertxHttpClientFactory extends VertxHttpClientFactory {

    public static final String PROVIDER_ID = "opentelemetry-vertx";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public int order() {
        return 110;
    }

    @Override
    public Set<Class<? extends Provider>> dependsOn() {
        return Set.of(TracingProvider.class);
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.HTTP_CLIENT_V2)
                && Profile.isFeatureEnabled(Profile.Feature.OPENTELEMETRY)
                && Configuration.isTrue(TracingOptions.TRACING_ENABLED);
    }
}

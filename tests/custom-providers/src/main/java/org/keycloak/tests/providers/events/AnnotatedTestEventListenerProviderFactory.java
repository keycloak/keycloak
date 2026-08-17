package org.keycloak.tests.providers.events;

import org.keycloak.Config;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.KeycloakProvider;

/**
 * Event listener factory that is discovered exclusively through the {@link KeycloakProvider} annotation.
 * <p>
 * It is intentionally <b>not</b> listed in {@code META-INF/services/org.keycloak.events.EventListenerProviderFactory};
 * if this provider shows up in the running server, the build-time annotation scan and the wiring into
 * provider discovery worked end-to-end.
 */
@KeycloakProvider
public class AnnotatedTestEventListenerProviderFactory implements EventListenerProviderFactory, EventListenerProvider {

    public static final String PROVIDER_ID = "test-annotated-event-listener";

    @Override
    public void onEvent(Event event) {
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
    }

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

}

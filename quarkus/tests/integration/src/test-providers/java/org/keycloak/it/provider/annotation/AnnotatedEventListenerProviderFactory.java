package org.keycloak.it.provider.annotation;

import org.keycloak.Config;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.KeycloakProvider;

import org.jboss.logging.Logger;

/**
 * Valid {@link KeycloakProvider} factory: discovered purely through the annotation, no
 * {@code META-INF/services} entry is shipped with it.
 */
@KeycloakProvider
public class AnnotatedEventListenerProviderFactory implements EventListenerProviderFactory, EventListenerProvider {

    public static final String PROVIDER_ID = "annotated-event-listener";
    public static final String INIT_MESSAGE = "AnnotatedEventListenerProviderFactory discovered via @KeycloakProvider";

    private static final Logger LOG = Logger.getLogger(AnnotatedEventListenerProviderFactory.class);

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
        LOG.info(INIT_MESSAGE);
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

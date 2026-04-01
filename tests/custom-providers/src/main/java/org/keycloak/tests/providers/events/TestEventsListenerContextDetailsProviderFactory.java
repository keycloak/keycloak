package org.keycloak.tests.providers.events;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.keycloak.Config;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class TestEventsListenerContextDetailsProviderFactory implements EventListenerProviderFactory, EventListenerProvider {

    public static List<Details> DETAILS = Collections.synchronizedList(new LinkedList<>());

    @Override
    public void onEvent(Event event) {
        DETAILS.add(new Details(event.getRealmName(), event.getClientId()));
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
        return "event-queue-context-details";
    }

    public record Details(String realmName, String clientId) {}

}

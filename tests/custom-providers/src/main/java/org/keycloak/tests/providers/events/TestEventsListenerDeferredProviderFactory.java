package org.keycloak.tests.providers.events;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.keycloak.Config;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.EventListenerTransaction;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class TestEventsListenerDeferredProviderFactory implements EventListenerProviderFactory {

    public static final String ID = "event-queue-deferred";

    public static final List<EventType> TYPES_AT_DISPATCH = Collections.synchronizedList(new LinkedList<>());

    public static final List<EventType> TYPES_AT_COMMIT = Collections.synchronizedList(new LinkedList<>());

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new DeferredEventListenerProvider(session);
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
        return ID;
    }

    private static final class DeferredEventListenerProvider implements EventListenerProvider {

        private final EventListenerTransaction tx = new EventListenerTransaction(null, event -> TYPES_AT_COMMIT.add(event.getType()));

        DeferredEventListenerProvider(KeycloakSession session) {
            session.getTransactionManager().enlistAfterCompletion(tx);
        }

        @Override
        public void onEvent(Event event) {
            TYPES_AT_DISPATCH.add(event.getType());
            tx.addEvent(event);
        }

        @Override
        public void onEvent(AdminEvent event, boolean includeRepresentation) {
        }

        @Override
        public void close() {
        }
    }
}

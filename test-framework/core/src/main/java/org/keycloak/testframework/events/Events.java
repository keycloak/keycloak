package org.keycloak.testframework.events;

import java.util.List;

import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.testframework.realm.ManagedRealm;

import org.jboss.logging.Logger;

public class Events extends AbstractEvents<EventRepresentation> {

    private static final Logger LOGGER = Logger.getLogger(Events.class);

    public Events(ManagedRealm realm) {
        super(realm);
    }

    @Override
    protected List<EventRepresentation> getEvents(long from, long to) {
        return realm.admin().getEvents(null, null, null, from, to, null, null, null, "asc");
    }

    @Override
    protected String getEventId(EventRepresentation rep) {
        return rep.getId();
    }

    @Override
    protected String getRealmId(EventRepresentation rep) {
        return rep.getRealmId();
    }

    @Override
    protected void clearServerEvents() {
        realm.admin().clearEvents();
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}

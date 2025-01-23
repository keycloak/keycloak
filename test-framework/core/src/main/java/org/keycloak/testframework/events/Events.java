package org.keycloak.testframework.events;

import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.testframework.realm.ManagedRealm;

import java.util.List;

public class Events extends AbstractEvents<Event, EventRepresentation> {

    private static final Logger LOGGER = Logger.getLogger(Events.class);

    public Events(ManagedRealm realm) {
        super(realm);
    }

    @Override
    protected List<EventRepresentation> getEvents(long from, long to) {
        return realm.admin().getEvents(null, null, null, from, to, null, null, null, "asc");
    }

    @Override
    protected String getId(EventRepresentation rep) {
        return rep.getId();
    }

    @Override
    protected Event convert(EventRepresentation rep) {
        Event e = new Event();
        e.setId(rep.getId());
        e.setTime(rep.getTime());
        e.setType(EventType.valueOf(rep.getType()));
        e.setRealmId(rep.getRealmId());
        e.setRealmName(getRealmName(rep.getRealmId()));
        e.setClientId(rep.getClientId());
        e.setUserId(rep.getUserId());
        e.setSessionId(rep.getSessionId());
        e.setIpAddress(rep.getIpAddress());
        e.setError(rep.getError());
        e.setDetails(rep.getDetails());
        return e;
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

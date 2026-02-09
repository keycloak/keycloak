package org.keycloak.testframework.events;

import java.util.List;

import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.testframework.realm.ManagedRealm;

import org.jboss.logging.Logger;

public class AdminEvents extends AbstractEvents<AdminEventRepresentation> {

    private static final Logger LOGGER = Logger.getLogger(AdminEvents.class);

    public AdminEvents(ManagedRealm realm) {
        super(realm);
    }

    @Override
    protected List<AdminEventRepresentation> getEvents(long from, long to) {
        return realm.admin().getAdminEvents(null, null, null, null, null, null, null, from, to, null, null, "asc");
    }

    @Override
    protected String getEventId(AdminEventRepresentation rep) {
        return rep.getId();
    }

    @Override
    protected String getRealmId(AdminEventRepresentation rep) {
        return rep.getRealmId();
    }

    @Override
    protected void clearServerEvents() {
        realm.admin().clearAdminEvents();
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}

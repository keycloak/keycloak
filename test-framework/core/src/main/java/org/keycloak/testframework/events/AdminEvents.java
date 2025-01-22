package org.keycloak.testframework.events;

import org.jboss.logging.Logger;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.AuthDetails;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.AuthDetailsRepresentation;
import org.keycloak.testframework.realm.ManagedRealm;

import java.util.List;

public class AdminEvents extends AbstractEvents<AdminEvent, AdminEventRepresentation> {

    private static final Logger LOGGER = Logger.getLogger(AdminEvents.class);

    public AdminEvents(ManagedRealm realm) {
        super(realm);
    }

    @Override
    protected List<AdminEventRepresentation> getEvents(long from, long to) {
        return realm.admin().getAdminEvents(null, null, null, null, null, null, null, from, to, null, null, "asc");
    }

    @Override
    protected String getId(AdminEventRepresentation rep) {
        return rep.getId();
    }

    @Override
    protected AdminEvent convert(AdminEventRepresentation rep) {
        AdminEvent e = new AdminEvent();
        e.setId(rep.getId());
        e.setTime(rep.getTime());
        e.setRealmId(rep.getRealmId());
        e.setRealmName(getRealmName(rep.getRealmId()));
        e.setAuthDetails(convert(rep.getAuthDetails()));
        e.setResourceType(ResourceType.valueOf(rep.getResourceType()));
        e.setOperationType(OperationType.valueOf(rep.getOperationType()));
        e.setResourcePath(rep.getResourcePath());
        e.setRepresentation(rep.getRepresentation());
        e.setError(rep.getError());
        e.setDetails(rep.getDetails());
        return e;
    }

    private AuthDetails convert(AuthDetailsRepresentation rep) {
        AuthDetails d = new AuthDetails();
        d.setClientId(rep.getClientId());
        d.setIpAddress(rep.getIpAddress());
        d.setRealmId(rep.getRealmId());
        d.setRealmName(getRealmName(rep.getRealmId()));
        d.setUserId(rep.getUserId());
        return d;
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

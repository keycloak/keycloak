package org.keycloak.testsuite.updaters;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;

public class RealmAttributeUpdater implements Closeable {

    private final RealmResource realm;
    private final RealmRepresentation original;
    private final RealmRepresentation updated;

    public RealmAttributeUpdater(RealmResource realm) {
        this.realm = realm;
        this.original = realm.toRepresentation();
        this.updated = realm.toRepresentation();
    }

    public RealmAttributeUpdater addEventsListener(String listenerId) {
        if (updated.getEventsListeners() == null) {
            updated.setEventsListeners(new ArrayList<>());
        }
        if (!updated.getEventsListeners().contains(listenerId)) {
            updated.getEventsListeners().add(listenerId);
        }
        return this;
    }

    public RealmAttributeUpdater setOtpPolicyCodeReusable(boolean reusable) {
        updated.setOtpPolicyCodeReusable(reusable);
        return this;
    }

    public RealmAttributeUpdater update() {
        realm.update(updated);
        return this;
    }

    @Override
    public void close() throws IOException {
        realm.update(original);
    }
}

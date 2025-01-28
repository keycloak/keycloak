package org.keycloak.testframework.events;

import org.keycloak.testframework.annotations.InjectAdminEvents;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfigBuilder;

public class AdminEventsSupplier extends AbstractEventsSupplier<AdminEvents, InjectAdminEvents> {

    @Override
    public Class<InjectAdminEvents> getAnnotationClass() {
        return InjectAdminEvents.class;
    }

    @Override
    public Class<AdminEvents> getValueType() {
        return AdminEvents.class;
    }

    @Override
    public AdminEvents createValue(ManagedRealm realm) {
        return new AdminEvents(realm);
    }

    @Override
    public RealmConfigBuilder intercept(RealmConfigBuilder realm, InstanceContext<AdminEvents, InjectAdminEvents> instanceContext) {
        return realm.adminEventsEnabled(true).adminEventsDetailsEnabled(true);
    }

}

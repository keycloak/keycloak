package org.keycloak.testframework.events;

import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfigBuilder;

public class EventsSupplier extends AbstractEventsSupplier<Events, InjectEvents> {

    @Override
    public Events getValue(InstanceContext<Events, InjectEvents> instanceContext) {
        return super.getValue(instanceContext);
    }

    @Override
    protected Events createValue(ManagedRealm realm) {
        return new Events(realm);
    }

    @Override
    public RealmConfigBuilder intercept(RealmConfigBuilder realm, InstanceContext<Events, InjectEvents> instanceContext) {
        return realm.eventsEnabled(true);
    }

}

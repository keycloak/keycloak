package org.keycloak.testframework.events;

import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.injection.SupplierOrder;

public class EventsSupplier implements Supplier<Events, InjectEvents> {

    @Override
    public Class<InjectEvents> getAnnotationClass() {
        return InjectEvents.class;
    }

    @Override
    public Class<Events> getValueType() {
        return Events.class;
    }

    @Override
    public Events getValue(InstanceContext<Events, InjectEvents> instanceContext) {
        Events events = new Events();
        SysLogServer sysLogServer = instanceContext.getDependency(SysLogServer.class);
        instanceContext.addNote("server", sysLogServer);
        return events;
    }

    @Override
    public void onBeforeEach(InstanceContext<Events, InjectEvents> instanceContext) {
        instanceContext.getNote("server", SysLogServer.class).addListener(instanceContext.getValue());
    }

    @Override
    public LifeCycle getDefaultLifecycle() {
        return LifeCycle.METHOD;
    }

    @Override
    public void close(InstanceContext<Events, InjectEvents> instanceContext) {
        instanceContext.getNote("server", SysLogServer.class).removeListener(instanceContext.getValue());
    }

    @Override
    public boolean compatible(InstanceContext<Events, InjectEvents> a, RequestedInstance<Events, InjectEvents> b) {
        return true;
    }

    @Override
    public int order() {
        return SupplierOrder.BEFORE_KEYCLOAK_SERVER;
    }
}

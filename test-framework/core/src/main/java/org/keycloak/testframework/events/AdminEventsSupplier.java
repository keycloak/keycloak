package org.keycloak.testframework.events;

import org.keycloak.testframework.annotations.InjectAdminEvents;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.injection.SupplierOrder;

public class AdminEventsSupplier implements Supplier<AdminEvents, InjectAdminEvents> {

    @Override
    public Class<InjectAdminEvents> getAnnotationClass() {
        return InjectAdminEvents.class;
    }

    @Override
    public Class<AdminEvents> getValueType() {
        return AdminEvents.class;
    }

    @Override
    public AdminEvents getValue(InstanceContext<AdminEvents, InjectAdminEvents> instanceContext) {
        AdminEvents adminEvents = new AdminEvents();
        SysLogServer sysLogServer = instanceContext.getDependency(SysLogServer.class);
        instanceContext.addNote("server", sysLogServer);
        return adminEvents;
    }

    @Override
    public void onBeforeEach(InstanceContext<AdminEvents, InjectAdminEvents> instanceContext) {
        instanceContext.getNote("server", SysLogServer.class).addListener(instanceContext.getValue());
    }

    @Override
    public LifeCycle getDefaultLifecycle() {
        return LifeCycle.METHOD;
    }

    @Override
    public void close(InstanceContext<AdminEvents, InjectAdminEvents> instanceContext) {
        instanceContext.getNote("server", SysLogServer.class).removeListener(instanceContext.getValue());
    }

    @Override
    public boolean compatible(InstanceContext<AdminEvents, InjectAdminEvents> a, RequestedInstance<AdminEvents, InjectAdminEvents> b) {
        return true;
    }

    @Override
    public int order() {
        return SupplierOrder.BEFORE_KEYCLOAK_SERVER;
    }
}

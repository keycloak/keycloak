package org.keycloak.test.framework.events;

import org.keycloak.test.framework.annotations.InjectAdminEvents;
import org.keycloak.test.framework.injection.InstanceContext;
import org.keycloak.test.framework.injection.LifeCycle;
import org.keycloak.test.framework.injection.RequestedInstance;
import org.keycloak.test.framework.injection.Supplier;
import org.keycloak.test.framework.injection.SupplierOrder;

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

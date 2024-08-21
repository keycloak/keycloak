package org.keycloak.test.framework.events;

import org.keycloak.test.framework.annotations.InjectSysLogServer;
import org.keycloak.test.framework.injection.InstanceContext;
import org.keycloak.test.framework.injection.LifeCycle;
import org.keycloak.test.framework.injection.RequestedInstance;
import org.keycloak.test.framework.injection.Supplier;

import java.io.IOException;

public class SysLogServerSupplier implements Supplier<SysLogServer, InjectSysLogServer> {
    @Override
    public Class<InjectSysLogServer> getAnnotationClass() {
        return InjectSysLogServer.class;
    }

    @Override
    public Class<SysLogServer> getValueType() {
        return SysLogServer.class;
    }

    @Override
    public SysLogServer getValue(InstanceContext<SysLogServer, InjectSysLogServer> instanceContext) {
        try {
            SysLogServer server = new SysLogServer();
            server.start();
            return server;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public LifeCycle getDefaultLifecycle() {
        return LifeCycle.GLOBAL;
    }

    @Override
    public void close(InstanceContext<SysLogServer, InjectSysLogServer> instanceContext) {
        SysLogServer server = instanceContext.getValue();
        try {
            server.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean compatible(InstanceContext<SysLogServer, InjectSysLogServer> a, RequestedInstance<SysLogServer, InjectSysLogServer> b) {
        return true;
    }
}

package org.keycloak.test.framework.events;

import org.keycloak.test.framework.annotations.InjectSysLogServer;
import org.keycloak.test.framework.injection.InstanceContext;
import org.keycloak.test.framework.injection.LifeCycle;
import org.keycloak.test.framework.injection.RequestedInstance;
import org.keycloak.test.framework.injection.Supplier;
import org.keycloak.test.framework.injection.SupplierOrder;
import org.keycloak.test.framework.server.KeycloakServerConfigBuilder;
import org.keycloak.test.framework.server.KeycloakServerConfigInterceptor;

import java.io.IOException;

public class SysLogServerSupplier implements Supplier<SysLogServer, InjectSysLogServer>, KeycloakServerConfigInterceptor<SysLogServer, InjectSysLogServer> {

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
            return new SysLogServer();
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

    @Override
    public KeycloakServerConfigBuilder intercept(KeycloakServerConfigBuilder serverConfig, InstanceContext<SysLogServer, InjectSysLogServer> instanceContext) {
        serverConfig.log()
                .handlers(KeycloakServerConfigBuilder.LogHandlers.SYSLOG)
                .syslogEndpoint(instanceContext.getValue().getEndpoint())
                .handlerLevel(KeycloakServerConfigBuilder.LogHandlers.SYSLOG, "INFO");

        serverConfig.option("spi-events-listener-jboss-logging-success-level", "INFO")
                .log().categoryLevel("org.keycloak.events", "INFO");

        return serverConfig;
    }

    @Override
    public int order() {
        return SupplierOrder.BEFORE_KEYCLOAK_SERVER;
    }
}

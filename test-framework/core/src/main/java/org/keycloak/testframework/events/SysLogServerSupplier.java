package org.keycloak.testframework.events;

import java.io.IOException;

import org.keycloak.testframework.annotations.InjectSysLogServer;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.injection.SupplierOrder;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.server.KeycloakServerConfigInterceptor;

public class SysLogServerSupplier implements Supplier<SysLogServer, InjectSysLogServer>, KeycloakServerConfigInterceptor<SysLogServer, InjectSysLogServer> {

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

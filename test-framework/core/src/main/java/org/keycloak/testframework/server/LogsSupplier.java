package org.keycloak.testframework.server;

import java.util.List;

import org.keycloak.testframework.annotations.InjectLogs;
import org.keycloak.testframework.injection.DependenciesBuilder;
import org.keycloak.testframework.injection.Dependency;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;

public class LogsSupplier implements Supplier<Logs, InjectLogs> {

    @Override
    public Logs getValue(InstanceContext<Logs, InjectLogs> instanceContext) {
        KeycloakServer server = instanceContext.getDependency(KeycloakServer.class);
        int node = instanceContext.getAnnotation().node();
        return server.getLogs(node).createClassView();
    }

    @Override
    public List<Dependency> getDependencies(RequestedInstance<Logs, InjectLogs> requestedInstance) {
        return DependenciesBuilder.create(KeycloakServer.class).build();
    }

    @Override
    public boolean compatible(InstanceContext<Logs, InjectLogs> a, RequestedInstance<Logs, InjectLogs> b) {
        return a.getAnnotation().node() == b.getAnnotation().node();
    }

    @Override
    public String getRef(InjectLogs annotation) {
        return String.valueOf(annotation.node());
    }
}

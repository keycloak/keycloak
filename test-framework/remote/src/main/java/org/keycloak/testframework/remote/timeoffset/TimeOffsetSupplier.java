package org.keycloak.testframework.remote.timeoffset;

import org.apache.http.client.HttpClient;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.injection.SupplierOrder;
import org.keycloak.testframework.remote.RemoteProviders;
import org.keycloak.testframework.server.KeycloakUrls;

import java.util.Set;

public class TimeOffsetSupplier implements Supplier<TimeOffSet, InjectTimeOffSet> {
    @Override
    public Class<InjectTimeOffSet> getAnnotationClass() {
        return InjectTimeOffSet.class;
    }

    @Override
    public Class<TimeOffSet> getValueType() {
        return TimeOffSet.class;
    }

    @Override
    public Set<Class<?>> dependencies() {
        return Set.of(HttpClient.class, RemoteProviders.class, KeycloakUrls.class);
    }

    @Override
    public TimeOffSet getValue(InstanceContext<TimeOffSet, InjectTimeOffSet> instanceContext) {
        var httpClient = instanceContext.getDependency(HttpClient.class);
        var remoteProviders = instanceContext.getDependency(RemoteProviders.class);
        KeycloakUrls keycloakUrls = instanceContext.getDependency(KeycloakUrls.class);

        int initOffset = instanceContext.getAnnotation().offset();
        return new TimeOffSet(httpClient, keycloakUrls.getMasterRealm(), initOffset);
    }

    @Override
    public boolean compatible(InstanceContext<TimeOffSet, InjectTimeOffSet> a, RequestedInstance<TimeOffSet, InjectTimeOffSet> b) {
        return true;
    }

    @Override
    public LifeCycle getDefaultLifecycle() {
        return LifeCycle.METHOD;
    }

    @Override
    public void close(InstanceContext<TimeOffSet, InjectTimeOffSet> instanceContext) {
        TimeOffSet timeOffSet = instanceContext.getValue();
        if (timeOffSet.hasChanged()) {
            timeOffSet.set(0);
        }
    }

    @Override
    public int order() {
        return SupplierOrder.BEFORE_KEYCLOAK_SERVER;
    }

}

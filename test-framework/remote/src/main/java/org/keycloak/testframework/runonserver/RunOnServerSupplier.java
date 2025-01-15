package org.keycloak.testframework.runonserver;

import org.apache.http.client.HttpClient;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.injection.SupplierOrder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.RemoteProviders;
import org.keycloak.testframework.server.KeycloakUrls;

import java.util.Set;

public class RunOnServerSupplier implements Supplier<RunOnServerClient, InjectRunOnServer> {

    private TestClassServer server;

    @Override
    public Class<InjectRunOnServer> getAnnotationClass() {
        return InjectRunOnServer.class;
    }

    @Override
    public Class<RunOnServerClient> getValueType() {
        return RunOnServerClient.class;
    }

    @Override
    public Set<Class<?>> dependencies() {
        return Set.of(HttpClient.class, ManagedRealm.class, RemoteProviders.class, KeycloakUrls.class);
    }

    @Override
    public RunOnServerClient getValue(InstanceContext<RunOnServerClient, InjectRunOnServer> instanceContext) {
        HttpClient httpClient = instanceContext.getDependency(HttpClient.class);
        ManagedRealm realm = instanceContext.getDependency(ManagedRealm.class);
        KeycloakUrls keycloakUrls = instanceContext.getDependency(KeycloakUrls.class);
        RemoteProviders remoteProviders = instanceContext.getDependency(RemoteProviders.class);

        server = new TestClassServer();

        return new RunOnServerClient(httpClient, keycloakUrls.getBase(), realm.getName());
    }

    @Override
    public boolean compatible(InstanceContext<RunOnServerClient, InjectRunOnServer> a, RequestedInstance<RunOnServerClient, InjectRunOnServer> b) {
        return a.getAnnotation().realmRef().equals(b.getAnnotation().realmRef());
    }

    @Override
    public LifeCycle getDefaultLifecycle() {
        return LifeCycle.METHOD;
    }

    @Override
    public void close(InstanceContext<RunOnServerClient, InjectRunOnServer> instanceContext) {
        server.close();
    }

    @Override
    public int order() {
        return SupplierOrder.BEFORE_KEYCLOAK_SERVER;
    }
}

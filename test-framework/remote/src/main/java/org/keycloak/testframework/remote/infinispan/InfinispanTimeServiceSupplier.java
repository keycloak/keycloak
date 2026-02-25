package org.keycloak.testframework.remote.infinispan;


import java.util.List;

import org.keycloak.testframework.injection.DependenciesBuilder;
import org.keycloak.testframework.injection.Dependency;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.injection.SupplierOrder;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;

public class InfinispanTimeServiceSupplier implements Supplier<InfinispanTimeService, InjectInfinispanTimeService>  {

    @Override
    public List<Dependency> getDependencies(RequestedInstance<InfinispanTimeService, InjectInfinispanTimeService> instanceContext) {
        return DependenciesBuilder.create(RunOnServerClient.class)
                .build();
    }

    @Override
    public InfinispanTimeService getValue(InstanceContext<InfinispanTimeService, InjectInfinispanTimeService> instanceContext) {
        RunOnServerClient runOnServer = instanceContext.getDependency(RunOnServerClient.class);
        return new InfinispanTimeService(runOnServer);
    }

    @Override
    public boolean compatible(InstanceContext<InfinispanTimeService, InjectInfinispanTimeService> a, RequestedInstance<InfinispanTimeService, InjectInfinispanTimeService> b) {
        return true;
    }

    @Override
    public LifeCycle getDefaultLifecycle() {
        return LifeCycle.METHOD;
    }

    @Override
    public void close(InstanceContext<InfinispanTimeService, InjectInfinispanTimeService> instanceContext) {
        InfinispanTimeService timeService = instanceContext.getValue();
        timeService.revertTestingInfinispanTimeService();
    }

    @Override
    public int order() {
        return SupplierOrder.BEFORE_KEYCLOAK_SERVER;
    }
}

package org.keycloak.testframework.log;

import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;

public class LogSupplier implements Supplier<Logs, InjectLogs> {

    @Override
    public Logs getValue(InstanceContext<Logs, InjectLogs> instanceContext) {
        return new Logs();
    }


    @Override
    public boolean compatible(InstanceContext<Logs, InjectLogs> a, RequestedInstance<Logs, InjectLogs> b) {
        return true;
    }

}

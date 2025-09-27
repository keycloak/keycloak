package org.keycloak.testframework.https;

import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.injection.SupplierOrder;

public class CertificatesSupplier implements Supplier<ManagedCertificates, InjectCertificates> {

    @Override
    public ManagedCertificates getValue(InstanceContext<ManagedCertificates, InjectCertificates> instanceContext) {
        return new ManagedCertificates();
    }

    @Override
    public LifeCycle getDefaultLifecycle() {
        return LifeCycle.GLOBAL;
    }

    @Override
    public boolean compatible(InstanceContext<ManagedCertificates, InjectCertificates> a, RequestedInstance<ManagedCertificates, InjectCertificates> b) {
        return true;
    }

    @Override
    public int order() {
        return SupplierOrder.BEFORE_KEYCLOAK_SERVER;
    }
}

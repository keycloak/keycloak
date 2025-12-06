package org.keycloak.testframework.https;

import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.injection.RequestedInstance;

public class ClientCertificatesSupplier extends AbstractCertificatesSupplier {

    @Override
    public LifeCycle getDefaultLifecycle() {
        return LifeCycle.CLASS;
    }

    @Override
    public boolean compatible(InstanceContext<Certificates, InjectCertificates> a, RequestedInstance<Certificates, InjectCertificates> b) {
        return a.getAnnotation().config().equals(b.getAnnotation().config());
    }

    @Override
    public Certificates getManagedCertificatesInstance(CertificatesConfigBuilder certBuilder) {
        return new ManagedClientCertificates(certBuilder);
    }
}

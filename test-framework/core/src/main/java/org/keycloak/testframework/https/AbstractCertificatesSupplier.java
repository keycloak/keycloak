package org.keycloak.testframework.https;

import org.keycloak.testframework.config.Config;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.injection.SupplierHelpers;
import org.keycloak.testframework.injection.SupplierOrder;

public abstract class AbstractCertificatesSupplier implements Supplier<Certificates, InjectCertificates> {

    @Override
    public Certificates getValue(InstanceContext<Certificates, InjectCertificates> instanceContext) {
        CertificatesConfig certConfig = SupplierHelpers.getInstance(instanceContext.getAnnotation().config());
        CertificatesConfigBuilder certBuilder = new CertificatesConfigBuilder();
        certBuilder = certConfig.configure(certBuilder);

        String supplierConfig = Config.getSupplierConfig(ManagedServerCertificates.class);
        if (supplierConfig != null) {
            CertificatesConfig certConfigOverride = SupplierHelpers.getInstance(supplierConfig);
            certConfigOverride.configure(certBuilder);
        }
        return getManagedCertificatesInstance(certBuilder);
    }

    @Override
    public LifeCycle getDefaultLifecycle() {
        return LifeCycle.GLOBAL;
    }

    @Override
    public boolean compatible(InstanceContext<Certificates, InjectCertificates> a, RequestedInstance<Certificates, InjectCertificates> b) {
        return true;
    }

    @Override
    public int order() {
        return SupplierOrder.BEFORE_KEYCLOAK_SERVER;
    }

    public abstract Certificates getManagedCertificatesInstance(CertificatesConfigBuilder certBuilder);
}

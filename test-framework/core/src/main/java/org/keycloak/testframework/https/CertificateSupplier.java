package org.keycloak.testframework.https;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.crypto.def.DefaultCryptoProvider;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.injection.SupplierOrder;

public class CertificateSupplier implements Supplier<ManagedCertificate, InjectCertificate> {

    @Override
    public ManagedCertificate getValue(InstanceContext<ManagedCertificate, InjectCertificate> instanceContext) {
        CryptoProvider cryptoProvider = new DefaultCryptoProvider();
        CryptoIntegration.setProvider(cryptoProvider);
        return new ManagedCertificate(cryptoProvider);
    }

    @Override
    public LifeCycle getDefaultLifecycle() {
        return LifeCycle.GLOBAL;
    }

    @Override
    public boolean compatible(InstanceContext<ManagedCertificate, InjectCertificate> a, RequestedInstance<ManagedCertificate, InjectCertificate> b) {
        return true;
    }

    @Override
    public int order() {
        return SupplierOrder.BEFORE_KEYCLOAK_SERVER;
    }
}

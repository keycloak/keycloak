package org.keycloak.testframework.https;

public class ServerCertificatesSupplier extends AbstractCertificatesSupplier {

    @Override
    public Certificates getManagedCertificatesInstance(CertificatesConfigBuilder certBuilder) {
        return new ManagedServerCertificates(certBuilder);
    }
}

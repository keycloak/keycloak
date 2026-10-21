package org.keycloak.testframework.https;

/**
 * Declarative configuration for managed certificates
 */
public interface CertificatesConfig {

    CertificatesConfigBuilder configure(CertificatesConfigBuilder config);
}

package org.keycloak.jgroups.certificates;

import java.util.stream.Stream;

import org.keycloak.Config;
import org.keycloak.compatibility.AbstractCompatibilityMetadataProvider;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.spi.infinispan.JGroupsCertificateProviderSpi;

public class JGroupsCertificatesMetadataProvider extends AbstractCompatibilityMetadataProvider {

    public JGroupsCertificatesMetadataProvider() {
        super(JGroupsCertificateProviderSpi.SPI_NAME, DefaultJGroupsCertificateProviderFactory.PROVIDER_ID);
    }

    @Override
    protected boolean isEnabled(Config.Scope scope) {
        return InfinispanUtils.isEmbeddedInfinispan();
    }

    @Override
    public Stream<String> configKeys() {
        return Stream.of(DefaultJGroupsCertificateProviderFactory.ENABLED);
    }
}

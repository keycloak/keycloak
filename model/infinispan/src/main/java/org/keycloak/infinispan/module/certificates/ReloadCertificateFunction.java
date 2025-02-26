package org.keycloak.infinispan.module.certificates;

import java.util.function.Function;

import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.keycloak.marshalling.Marshalling;

/**
 * Reloads the JGroups certificate
 */
@ProtoTypeId(Marshalling.RELOAD_CERTIFICATE_FUNCTION)
public final class ReloadCertificateFunction implements Function<EmbeddedCacheManager, Void> {

    private static final ReloadCertificateFunction INSTANCE = new ReloadCertificateFunction();

    private ReloadCertificateFunction() {}

    @ProtoFactory
    public static ReloadCertificateFunction getInstance() {
        return INSTANCE;
    }

    @Override
    public Void apply(EmbeddedCacheManager embeddedCacheManager) {
        var crm = GlobalComponentRegistry.componentOf(embeddedCacheManager, CertificateReloadManager.class);
        if (crm != null) {
            crm.reloadCertificate();
        }
        return null;
    }
}

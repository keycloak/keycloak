package org.keycloak.infinispan.module;

import org.keycloak.jgroups.certificates.CertificateReloadManager;

import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.factories.annotations.InfinispanModule;
import org.infinispan.factories.impl.BasicComponentRegistry;
import org.infinispan.lifecycle.ModuleLifecycle;

@InfinispanModule(name = "keycloak", requiredModules = {"core"})
public class KeycloakModule implements ModuleLifecycle {

    @Override
    public void cacheManagerStarting(GlobalComponentRegistry gcr, GlobalConfiguration globalConfiguration) {
        // start certificate reload manager before the JGroupsTransport
        //noinspection removal
        gcr.getComponent(BasicComponentRegistry.class)
                .getComponent(CertificateReloadManager.class)
                .running();
    }
}

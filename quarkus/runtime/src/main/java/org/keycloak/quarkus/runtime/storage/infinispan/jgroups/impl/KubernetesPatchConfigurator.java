package org.keycloak.quarkus.runtime.storage.infinispan.jgroups.impl;

import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.remoting.transport.jgroups.EmbeddedJGroupsChannelConfigurator;
import org.jgroups.conf.ProtocolConfiguration;
import org.keycloak.models.KeycloakSession;
import org.keycloak.quarkus.runtime.storage.infinispan.CacheManagerFactory;
import org.keycloak.quarkus.runtime.storage.infinispan.jgroups.JGroupsStackConfigurator;

import java.util.List;
import java.util.Map;

import static org.keycloak.quarkus.runtime.storage.infinispan.jgroups.JGroupsUtil.transportOf;

/**
 * Patch for <a href="https://github.com/keycloak/keycloak/issues/39023">GHI#39023</a> and <a
 * href="https://github.com/keycloak/keycloak/issues/39454">GHI#39454</a>
 */
public class KubernetesPatchConfigurator implements JGroupsStackConfigurator {

    public static final String KUBERNETES_STACK = "kubernetes";
    private static final String KUBERNETES_PATCHED_STACK = "kubernetes-patched";

    public static final KubernetesPatchConfigurator INSTANCE = new KubernetesPatchConfigurator();

    @Override
    public boolean requiresKeycloakSession() {
        return false;
    }

    @Override
    public void configure(ConfigurationBuilderHolder holder, KeycloakSession session) {
        CacheManagerFactory.logger.info("[PATCH] Patching kubernetes stack.");
        // patch port range
        var attributes = Map.of("port_range", "0");
        var patch = List.of(new ProtocolConfiguration("TCP", attributes));
        holder.addJGroupsStack(new EmbeddedJGroupsChannelConfigurator(KUBERNETES_PATCHED_STACK, patch, null), KUBERNETES_STACK);
        transportOf(holder).stack(KUBERNETES_PATCHED_STACK);
    }
}

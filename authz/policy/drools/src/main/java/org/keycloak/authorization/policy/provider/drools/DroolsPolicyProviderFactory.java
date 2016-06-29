package org.keycloak.authorization.policy.provider.drools;

import org.keycloak.Config;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.policy.provider.PolicyProviderAdminService;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;
import org.keycloak.provider.ProviderFactory;
import org.kie.api.KieServices;
import org.kie.api.KieServices.Factory;
import org.kie.api.runtime.KieContainer;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class DroolsPolicyProviderFactory implements PolicyProviderFactory {

    private KieServices ks;
    private final Map<String, DroolsPolicy> containers = new HashMap<>();

    @Override
    public String getName() {
        return "Drools";
    }

    @Override
    public String getGroup() {
        return "Rule Based";
    }

    @Override
    public PolicyProvider create(Policy policy, AuthorizationProvider authorization) {
        if (!this.containers.containsKey(policy.getId())) {
            update(policy);
        }

        return new DroolsPolicyProvider(this.containers.get(policy.getId()));
    }

    @Override
    public PolicyProviderAdminService getAdminResource(ResourceServer resourceServer) {
        return new DroolsPolicyAdminResource(resourceServer, this);
    }

    @Override
    public PolicyProvider create(KeycloakSession session) {
        return null;
    }

    @Override
    public void init(Config.Scope config) {
        this.ks = Factory.get();
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        factory.register(new ProviderEventListener() {

            @Override
            public void onEvent(ProviderEvent event) {
                // Ensure the initialization is done after DB upgrade is finished
                if (event instanceof PostMigrationEvent) {
                    ProviderFactory<AuthorizationProvider> providerFactory = factory.getProviderFactory(AuthorizationProvider.class);
                    AuthorizationProvider authorization = providerFactory.create(factory.create());
                    authorization.getStoreFactory().getPolicyStore().findByType(getId()).forEach(DroolsPolicyProviderFactory.this::update);
                }
            }

        });
    }

    @Override
    public void close() {
        this.containers.values().forEach(DroolsPolicy::dispose);
        this.containers.clear();
    }

    @Override
    public String getId() {
        return "drools";
    }

    void update(Policy policy) {
        remove(policy);
        this.containers.put(policy.getId(), new DroolsPolicy(this.ks, policy));
    }

    void remove(Policy policy) {
        DroolsPolicy holder = this.containers.remove(policy.getId());

        if (holder != null) {
            holder.dispose();
        }
    }

    KieContainer getKieContainer(String groupId, String artifactId, String version) {
        return this.ks.newKieContainer(this.ks.newReleaseId(groupId, artifactId, version));
    }
}

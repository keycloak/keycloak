package org.keycloak.authorization.policy.provider.drools;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.Config;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.policy.provider.PolicyProviderAdminService;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.RulePolicyRepresentation;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class DroolsPolicyProviderFactory implements PolicyProviderFactory<RulePolicyRepresentation>, EnvironmentDependentProviderFactory {

    private KieServices ks;
    private final Map<String, DroolsPolicy> containers = Collections.synchronizedMap(new HashMap<>());
    private DroolsPolicyProvider provider = new DroolsPolicyProvider(policy -> {
        if (!containers.containsKey(policy.getId())) {
            synchronized (containers) {
                update(policy);
            }
        }
        return containers.get(policy.getId());
    });

    @Override
    public String getName() {
        return "Rules";
    }

    @Override
    public String getGroup() {
        return "Rule Based";
    }

    @Override
    public PolicyProvider create(AuthorizationProvider authorization) {
        return provider;
    }

    @Override
    public RulePolicyRepresentation toRepresentation(Policy policy, AuthorizationProvider authorization) {
        RulePolicyRepresentation representation = new RulePolicyRepresentation();

        representation.setArtifactGroupId(policy.getConfig().get("mavenArtifactGroupId"));
        representation.setArtifactId(policy.getConfig().get("mavenArtifactId"));
        representation.setArtifactVersion(policy.getConfig().get("mavenArtifactVersion"));
        representation.setScannerPeriod(policy.getConfig().get("scannerPeriod"));
        representation.setScannerPeriodUnit(policy.getConfig().get("scannerPeriodUnit"));
        representation.setSessionName(policy.getConfig().get("sessionName"));
        representation.setModuleName(policy.getConfig().get("moduleName"));

        return representation;
    }

    @Override
    public PolicyProviderAdminService getAdminResource(ResourceServer resourceServer, AuthorizationProvider authorization) {
        return new DroolsPolicyAdminResource(this);
    }

    @Override
    public PolicyProvider create(KeycloakSession session) {
        return null;
    }

    @Override
    public void onCreate(Policy policy, RulePolicyRepresentation representation, AuthorizationProvider authorization) {
        updateConfig(policy, representation);
        update(policy);
    }

    @Override
    public void onUpdate(Policy policy, RulePolicyRepresentation representation, AuthorizationProvider authorization) {
        updateConfig(policy, representation);
        update(policy);
    }

    @Override
    public void onImport(Policy policy, PolicyRepresentation representation, AuthorizationProvider authorization) {
        update(policy);
    }

    @Override
    public void onRemove(Policy policy, AuthorizationProvider authorization) {
        remove(policy);
    }

    @Override
    public Class<RulePolicyRepresentation> getRepresentationType() {
        return RulePolicyRepresentation.class;
    }

    @Override
    public void init(Config.Scope config) {
        this.ks = KieServices.get();
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
        this.containers.values().forEach(DroolsPolicy::dispose);
        this.containers.clear();
    }

    @Override
    public String getId() {
        return "rules";
    }

    private void updateConfig(Policy policy, RulePolicyRepresentation representation) {
        policy.putConfig("mavenArtifactGroupId", representation.getArtifactGroupId());
        policy.putConfig("mavenArtifactId", representation.getArtifactId());
        policy.putConfig("mavenArtifactVersion", representation.getArtifactVersion());
        policy.putConfig("scannerPeriod", representation.getScannerPeriod());
        policy.putConfig("scannerPeriodUnit", representation.getScannerPeriodUnit());
        policy.putConfig("sessionName", representation.getSessionName());
        policy.putConfig("moduleName", representation.getModuleName());
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

    @Override
    public boolean isSupported() {
        return Profile.isFeatureEnabled(Profile.Feature.AUTHZ_DROOLS_POLICY);
    }
}

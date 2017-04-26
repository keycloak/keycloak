package org.keycloak.authorization.policy.provider.js;

import java.util.Map;

import javax.script.ScriptEngineManager;

import org.keycloak.Config;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.representations.idm.authorization.JSPolicyRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class JSPolicyProviderFactory implements PolicyProviderFactory<JSPolicyRepresentation> {

    private static final String ENGINE = "nashorn";

    private JSPolicyProvider provider = new JSPolicyProvider(() -> new ScriptEngineManager().getEngineByName(ENGINE));

    @Override
    public String getName() {
        return "JavaScript";
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
    public PolicyProvider create(KeycloakSession session) {
        return null;
    }

    @Override
    public JSPolicyRepresentation toRepresentation(Policy policy, JSPolicyRepresentation representation) {
        representation.setCode(policy.getConfig().get("code"));
        return representation;
    }

    @Override
    public Class<JSPolicyRepresentation> getRepresentationType() {
        return JSPolicyRepresentation.class;
    }

    @Override
    public void onCreate(Policy policy, JSPolicyRepresentation representation, AuthorizationProvider authorization) {
        updatePolicy(policy, representation.getCode());
    }

    @Override
    public void onUpdate(Policy policy, JSPolicyRepresentation representation, AuthorizationProvider authorization) {
        updatePolicy(policy, representation.getCode());
    }

    @Override
    public void onImport(Policy policy, PolicyRepresentation representation, AuthorizationProvider authorization) {
        updatePolicy(policy, representation.getConfig().get("code"));
    }

    private void updatePolicy(Policy policy, String code) {
        Map<String, String> config = policy.getConfig();
        config.put("code", code);
        policy.setConfig(config);
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "js";
    }
}

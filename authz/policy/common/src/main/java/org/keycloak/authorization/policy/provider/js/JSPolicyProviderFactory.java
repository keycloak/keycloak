package org.keycloak.authorization.policy.provider.js;

import org.keycloak.Config;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.ScriptModel;
import org.keycloak.representations.idm.authorization.JSPolicyRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.scripting.EvaluatableScriptAdapter;
import org.keycloak.scripting.ScriptingProvider;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class JSPolicyProviderFactory implements PolicyProviderFactory<JSPolicyRepresentation> {

    private final JSPolicyProvider provider = new JSPolicyProvider(this::getEvaluatableScript);
    private ScriptCache scriptCache;

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
    public JSPolicyRepresentation toRepresentation(Policy policy, AuthorizationProvider authorization) {
        JSPolicyRepresentation representation = new JSPolicyRepresentation();
        representation.setCode(policy.getConfig().get("code"));
        return representation;
    }

    @Override
    public Class<JSPolicyRepresentation> getRepresentationType() {
        return JSPolicyRepresentation.class;
    }

    @Override
    public void onCreate(Policy policy, JSPolicyRepresentation representation, AuthorizationProvider authorization) {
        updatePolicy(policy, representation.getCode(), authorization);
    }

    @Override
    public void onUpdate(Policy policy, JSPolicyRepresentation representation, AuthorizationProvider authorization) {
        updatePolicy(policy, representation.getCode(), authorization);
    }

    @Override
    public void onImport(Policy policy, PolicyRepresentation representation, AuthorizationProvider authorization) {
        updatePolicy(policy, representation.getConfig().get("code"), authorization);
    }

    @Override
    public void onRemove(final Policy policy, final AuthorizationProvider authorization) {
        scriptCache.remove(policy.getId());
    }

    @Override
    public void init(Config.Scope config) {
        int maxEntries = Integer.parseInt(config.get("cache-max-entries", "100"));
        int maxAge = Integer.parseInt(config.get("cache-entry-max-age", "-1"));
        scriptCache = new ScriptCache(maxEntries, maxAge);
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

    @Override
    public boolean isInternal() {
        return !Profile.isFeatureEnabled(Profile.Feature.UPLOAD_SCRIPTS);
    }

    private EvaluatableScriptAdapter getEvaluatableScript(final AuthorizationProvider authz, final Policy policy) {
        return scriptCache.computeIfAbsent(policy.getId(), id -> {
            final ScriptingProvider scripting = authz.getKeycloakSession().getProvider(ScriptingProvider.class);
            ScriptModel script = getScriptModel(policy, authz.getRealm(), scripting);
            return scripting.prepareEvaluatableScript(script);
        });
    }

    protected ScriptModel getScriptModel(final Policy policy, final RealmModel realm, final ScriptingProvider scripting) {
        String scriptName = policy.getName();
        String scriptCode = policy.getConfig().get("code");
        String scriptDescription = policy.getDescription();

        //TODO lookup script by scriptId instead of creating it every time
        return scripting.createScript(realm.getId(), ScriptModel.TEXT_JAVASCRIPT, scriptName, scriptCode, scriptDescription);
    }

    private void updatePolicy(Policy policy, String code, AuthorizationProvider authorization) {
        scriptCache.remove(policy.getId());
        if (!Profile.isFeatureEnabled(Profile.Feature.UPLOAD_SCRIPTS) && !authorization.getKeycloakSession().getAttributeOrDefault("ALLOW_CREATE_POLICY", false) && !isDeployed()) {
            throw new RuntimeException("Script upload is disabled");
        }
        policy.putConfig("code", code);
    }

    protected boolean isDeployed() {
        return false;
    }
}

package org.keycloak.authorization.policy.provider.js;

import java.util.function.Supplier;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.keycloak.Config;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.policy.provider.PolicyProviderAdminService;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class JSPolicyProviderFactory implements PolicyProviderFactory {

    private JSPolicyProvider provider = new JSPolicyProvider(new Supplier<ScriptEngine>() {
        @Override
        public ScriptEngine get() {
            return new ScriptEngineManager().getEngineByName("nashorn");
        }
    });

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
    public PolicyProviderAdminService getAdminResource(ResourceServer resourceServer) {
        return null;
    }

    @Override
    public PolicyProvider create(KeycloakSession session) {
        return null;
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

package org.keycloak.scripting;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import javax.script.ScriptEngineManager;

/**
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
public class DefaultScriptingProviderFactory implements ScriptingProviderFactory {

    static final String ID = "script-based-auth";

    private final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();

    @Override
    public ScriptingProvider create(KeycloakSession session) {
        return new DefaultScriptingProvider(scriptEngineManager);
    }

    @Override
    public void init(Config.Scope config) {
        //NOOP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        //NOOP
    }

    @Override
    public void close() {
        //NOOP
    }

    @Override
    public String getId() {
        return ID;
    }
}

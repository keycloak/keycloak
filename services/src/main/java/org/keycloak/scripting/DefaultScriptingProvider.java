package org.keycloak.scripting;

import org.keycloak.models.ScriptModel;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * A {@link ScriptingProvider} that uses a {@link ScriptEngineManager} to evaluate scripts with a {@link ScriptEngine}.
 *
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
public class DefaultScriptingProvider implements ScriptingProvider {

    private final ScriptEngineManager scriptEngineManager;

    public DefaultScriptingProvider(ScriptEngineManager scriptEngineManager) {
        this.scriptEngineManager = scriptEngineManager;
    }

    @Override
    public InvocableScript prepareScript(ScriptModel script) {
        return prepareScript(script, ScriptBindingsConfigurer.EMPTY);
    }

    @Override
    public InvocableScript prepareScript(ScriptModel script, ScriptBindingsConfigurer bindingsConfigurer) {

        if (script == null) {
            throw new NullPointerException("script must not be null");
        }

        if (script.getCode() == null || script.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("script must not be null or empty");
        }

        if (bindingsConfigurer == null) {
            throw new NullPointerException("bindingsConfigurer must not be null");
        }

        ScriptEngine engine = lookupScriptEngineFor(script);

        if (engine == null) {
            throw new IllegalStateException("Could not find ScriptEngine for script: " + script);
        }

        configureBindings(bindingsConfigurer, engine);

        loadScriptIntoEngine(script, engine);

        return new InvocableScript(script, engine);
    }

    private void configureBindings(ScriptBindingsConfigurer bindingsConfigurer, ScriptEngine engine) {

        Bindings bindings = engine.createBindings();
        bindingsConfigurer.configureBindings(bindings);
        engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
    }

    private void loadScriptIntoEngine(ScriptModel script, ScriptEngine engine) {

        try {
            engine.eval(script.getCode());
        } catch (ScriptException se) {
            throw new ScriptExecutionException(script, se);
        }
    }

    private ScriptEngine lookupScriptEngineFor(ScriptModel script) {
        return scriptEngineManager.getEngineByMimeType(script.getMimeType());
    }

    @Override
    public void close() {
        //NOOP
    }
}

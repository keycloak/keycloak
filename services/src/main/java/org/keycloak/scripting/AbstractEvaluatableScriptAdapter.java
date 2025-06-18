package org.keycloak.scripting;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.keycloak.models.ScriptModel;

/**
 * Abstract class for wrapping a {@link ScriptModel} to make it evaluatable.
 *
 * @author <a href="mailto:jay@anslow.me.uk">Jay Anslow</a>
 */
abstract class AbstractEvaluatableScriptAdapter implements EvaluatableScriptAdapter {
    /**
     * Holds the {@link ScriptModel}.
     */
    private final ScriptModel scriptModel;

    AbstractEvaluatableScriptAdapter(final ScriptModel scriptModel) {
        if (scriptModel == null) {
            throw new IllegalArgumentException("scriptModel must not be null");
        }
        this.scriptModel = scriptModel;
    }

    @Override
    public Object eval(final ScriptBindingsConfigurer bindingsConfigurer) throws ScriptExecutionException {
        return evalUnchecked(createBindings(bindingsConfigurer));
    }

    @Override
    public ScriptModel getScriptModel() {
        return scriptModel;
    }

    /**
     * Note, calling this method modifies the underlying {@link ScriptEngine},
     * preventing concurrent use of the ScriptEngine (Nashorn's {@link ScriptEngine} and
     * {@link javax.script.CompiledScript} is thread-safe, but {@link Bindings} isn't).
     */
    InvocableScriptAdapter prepareInvokableScript(final ScriptBindingsConfigurer bindingsConfigurer) {
        final Bindings bindings = createBindings(bindingsConfigurer);
        evalUnchecked(bindings);
        final ScriptEngine engine = getEngine();
        engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        return new InvocableScriptAdapter(scriptModel, engine);
    }

    protected String getCode() {
        return scriptModel.getCode();
    }

    protected abstract ScriptEngine getEngine();

    protected abstract Object eval(Bindings bindings) throws ScriptException;

    private Object evalUnchecked(final Bindings bindings) {
        try {
            return eval(bindings);
        }
        catch (ScriptException e) {
            throw new ScriptExecutionException(scriptModel, e);
        }
    }

    private Bindings createBindings(final ScriptBindingsConfigurer bindingsConfigurer) {
        if (bindingsConfigurer == null) {
            throw new IllegalArgumentException("bindingsConfigurer must not be null");
        }
        final Bindings bindings = getEngine().createBindings();
        bindingsConfigurer.configureBindings(bindings);
        return bindings;
    }
}

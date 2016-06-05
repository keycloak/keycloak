package org.keycloak.scripting;

import org.keycloak.models.ScriptModel;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

/**
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
public class InvocableScript implements Invocable {

    /**
     * Holds the script metadata as well as the actual script.
     */
    private final ScriptModel script;

    /**
     * Holds the {@link ScriptEngine} instance initialized with the script code.
     */
    private final ScriptEngine scriptEngine;

    public InvocableScript(ScriptModel script, ScriptEngine scriptEngine) {
        this.script = script;
        this.scriptEngine =  scriptEngine;
    }

    @Override
    public Object invokeMethod(Object thiz, String name, Object... args) throws ScriptException, NoSuchMethodException {
        return getInvocableEngine().invokeMethod(thiz, name, args);
    }

    @Override
    public Object invokeFunction(String name, Object... args) throws ScriptException, NoSuchMethodException {
        return getInvocableEngine().invokeFunction(name, args);
    }

    @Override
    public <T> T getInterface(Class<T> clazz) {
        return getInvocableEngine().getInterface(clazz);
    }

    @Override
    public <T> T getInterface(Object thiz, Class<T> clazz) {
        return getInvocableEngine().getInterface(thiz, clazz);
    }

    private Invocable getInvocableEngine() {
        return (Invocable) scriptEngine;
    }

    /**
     * Returns {@literal true} iif the {@link ScriptEngine} has a function with the given {@code functionName}.
     * @param functionName
     * @return
     */
    public boolean hasFunction(String functionName){

        Object candidate = scriptEngine.getContext().getAttribute(functionName);

        return candidate != null;
    }
}

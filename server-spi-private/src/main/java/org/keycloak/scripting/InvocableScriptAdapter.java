/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.scripting;

import org.keycloak.models.ScriptModel;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

/**
 * Wraps a {@link ScriptModel} and makes it {@link Invocable}.
 *
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
public class InvocableScriptAdapter implements Invocable {

    /**
     * Holds the {@ScriptModel}
     */
    private final ScriptModel scriptModel;

    /**
     * Holds the {@link ScriptEngine} instance initialized with the script code.
     */
    private final ScriptEngine scriptEngine;

    /**
     * Creates a new {@link InvocableScriptAdapter} instance.
     *
     * @param scriptModel  must not be {@literal null}
     * @param scriptEngine must not be {@literal null}
     */
    public InvocableScriptAdapter(ScriptModel scriptModel, ScriptEngine scriptEngine) {

        if (scriptModel == null) {
            throw new IllegalArgumentException("scriptModel must not be null");
        }

        if (scriptEngine == null) {
            throw new IllegalArgumentException("scriptEngine must not be null");
        }

        this.scriptModel = scriptModel;
        this.scriptEngine = scriptEngine;
    }

    @Override
    public Object invokeMethod(Object thiz, String name, Object... args) throws ScriptExecutionException {

        try {
            return getInvocableEngine().invokeMethod(thiz, name, args);
        } catch (ScriptException | NoSuchMethodException e) {
            throw new ScriptExecutionException(scriptModel, e);
        }
    }

    @Override
    public Object invokeFunction(String name, Object... args) throws ScriptExecutionException {
        try {
            return getInvocableEngine().invokeFunction(name, args);
        } catch (ScriptException | NoSuchMethodException e) {
            throw new ScriptExecutionException(scriptModel, e);
        }
    }

    @Override
    public <T> T getInterface(Class<T> clazz) {
        return getInvocableEngine().getInterface(clazz);
    }

    @Override
    public <T> T getInterface(Object thiz, Class<T> clazz) {
        return getInvocableEngine().getInterface(thiz, clazz);
    }

    /**
     * Returns {@literal true} if the {@link ScriptEngine} has a definition with the given {@code name}.
     *
     * @param name
     * @return
     */
    public boolean isDefined(String name) {

        Object candidate = scriptEngine.getContext().getAttribute(name);

        return candidate != null;
    }

    private Invocable getInvocableEngine() {
        return (Invocable) scriptEngine;
    }
}

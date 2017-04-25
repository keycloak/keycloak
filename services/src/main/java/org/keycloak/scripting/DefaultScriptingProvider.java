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

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

/**
 * A {@link ScriptingProvider} that uses a {@link ScriptEngineManager} to evaluate scripts with a {@link ScriptEngine}.
 *
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
public class DefaultScriptingProvider implements ScriptingProvider {

    private final ScriptEngineManager scriptEngineManager;

    public DefaultScriptingProvider(ScriptEngineManager scriptEngineManager) {

        if (scriptEngineManager == null) {
            throw new IllegalStateException("scriptEngineManager must not be null!");
        }

        this.scriptEngineManager = scriptEngineManager;
    }

    /**
     * Wraps the provided {@link ScriptModel} in a {@link javax.script.Invocable} instance with bindings configured through the {@link ScriptBindingsConfigurer}.
     *
     * @param scriptModel  must not be {@literal null}
     * @param bindingsConfigurer must not be {@literal null}
     * @return
     */
    @Override
    public InvocableScriptAdapter prepareInvocableScript(ScriptModel scriptModel, ScriptBindingsConfigurer bindingsConfigurer) {

        if (scriptModel == null) {
            throw new IllegalArgumentException("script must not be null");
        }

        if (scriptModel.getCode() == null || scriptModel.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("script must not be null or empty");
        }

        if (bindingsConfigurer == null) {
            throw new IllegalArgumentException("bindingsConfigurer must not be null");
        }

        ScriptEngine engine = createPreparedScriptEngine(scriptModel, bindingsConfigurer);

        return new InvocableScriptAdapter(scriptModel, engine);
    }

    //TODO allow scripts to be maintained independently of other components, e.g. with dedicated persistence
    //TODO allow script lookup by (scriptId)
    //TODO allow script lookup by (name, realmName)

    @Override
    public ScriptModel createScript(String realmId, String mimeType, String scriptName, String scriptCode, String scriptDescription) {

        ScriptModel script = new Script(null /* scriptId */, realmId, scriptName, mimeType, scriptCode, scriptDescription);
        return script;
    }

    /**
     * Looks-up a {@link ScriptEngine} with prepared {@link Bindings} for the given {@link ScriptModel Script}.
     *
     * @param script
     * @param bindingsConfigurer
     * @return
     */
    private ScriptEngine createPreparedScriptEngine(ScriptModel script, ScriptBindingsConfigurer bindingsConfigurer) {

        ScriptEngine scriptEngine = lookupScriptEngineFor(script);

        if (scriptEngine == null) {
            throw new IllegalStateException("Could not find ScriptEngine for script: " + script);
        }

        configureBindings(bindingsConfigurer, scriptEngine);

        return scriptEngine;
    }

    private void configureBindings(ScriptBindingsConfigurer bindingsConfigurer, ScriptEngine engine) {

        Bindings bindings = engine.createBindings();
        bindingsConfigurer.configureBindings(bindings);
        engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
    }

    /**
     * Looks-up a {@link ScriptEngine} based on the MIME-type provided by the given {@link Script}.
     */
    private ScriptEngine lookupScriptEngineFor(ScriptModel script) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(DefaultScriptingProvider.class.getClassLoader());
            return scriptEngineManager.getEngineByMimeType(script.getMimeType());
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    @Override
    public void close() {
        //NOOP
    }
}

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

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.jboss.logging.Logger;
import org.keycloak.models.ScriptModel;
import org.keycloak.platform.Platform;
import org.keycloak.services.ServicesLogger;
import org.keycloak.utils.ProxyClassLoader;

/**
 * A {@link ScriptingProvider} that uses a {@link ScriptEngineManager} to evaluate scripts with a {@link ScriptEngine}.
 *
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
public class DefaultScriptingProvider implements ScriptingProvider {

    private static final Logger logger = Logger.getLogger(DefaultScriptingProvider.class);

    private final DefaultScriptingProviderFactory factory;

    DefaultScriptingProvider(DefaultScriptingProviderFactory factory) {
        this.factory = factory;
    }

    /**
     * Wraps the provided {@link ScriptModel} in a {@link javax.script.Invocable} instance with bindings configured through the {@link ScriptBindingsConfigurer}.
     *
     * @param scriptModel        must not be {@literal null}
     * @param bindingsConfigurer must not be {@literal null}
     */
    @Override
    public InvocableScriptAdapter prepareInvocableScript(ScriptModel scriptModel, ScriptBindingsConfigurer bindingsConfigurer) {
        final AbstractEvaluatableScriptAdapter evaluatable = prepareEvaluatableScript(scriptModel);
        return evaluatable.prepareInvokableScript(bindingsConfigurer);
    }

    /**
     * Wraps the provided {@link ScriptModel} in a {@link javax.script.Invocable} instance with bindings configured through the {@link ScriptBindingsConfigurer}.
     *
     * @param scriptModel must not be {@literal null}
     */
    @Override
    public AbstractEvaluatableScriptAdapter prepareEvaluatableScript(ScriptModel scriptModel) {
        if (scriptModel == null) {
            throw new IllegalArgumentException("script must not be null");
        }

        if (scriptModel.getCode() == null || scriptModel.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("script must not be null or empty");
        }

        ScriptEngine engine = getPreparedScriptEngine(scriptModel);

        if (engine instanceof Compilable) {
            return new CompiledEvaluatableScriptAdapter(scriptModel, tryCompile(scriptModel, (Compilable) engine));
        }

        return new UncompiledEvaluatableScriptAdapter(scriptModel, engine);
    }

    private CompiledScript tryCompile(ScriptModel scriptModel, Compilable engine) {
        try {
            return engine.compile(scriptModel.getCode());
        } catch (ScriptException e) {
            throw new ScriptCompilationException(scriptModel, e);
        }
    }

    @Override
    public ScriptModel createScript(String realmId, String mimeType, String scriptName, String scriptCode, String scriptDescription) {
        return new Script(null /* scriptId */, realmId, scriptName, mimeType, scriptCode, scriptDescription);
    }

    @Override
    public void close() {
        //NOOP
    }

    /**
     * Looks-up a {@link ScriptEngine} with prepared {@link Bindings} for the given {@link ScriptModel Script}.
     */
    private ScriptEngine getPreparedScriptEngine(ScriptModel script) {
        // Try to lookup shared engine in the cache first
        if (factory.isEnableScriptEngineCache()) {
            ScriptEngine scriptEngine = factory.getScriptEngineCache().get(script.getMimeType());
            if (scriptEngine != null) return scriptEngine;
        }

        ScriptEngine scriptEngine = lookupScriptEngineFor(script);

        if (scriptEngine == null) {
            throw new IllegalStateException("Could not find ScriptEngine for script: " + script);
        }

        ServicesLogger.LOGGER.scriptEngineCreated(scriptEngine.getFactory().getEngineName(), scriptEngine.getFactory().getEngineVersion(), script.getMimeType());

        // Nashorn scriptEngine is ok to cache and share across multiple threads
        if (factory.isEnableScriptEngineCache()) {
            factory.getScriptEngineCache().put(script.getMimeType(), scriptEngine);
        }

        return scriptEngine;
    }

    /**
     * Looks-up a {@link ScriptEngine} based on the MIME-type provided by the given {@link Script}.
     */
    private ScriptEngine lookupScriptEngineFor(ScriptModel script) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            ClassLoader scriptClassLoader = Platform.getPlatform().getScriptEngineClassLoader(factory.getConfig());

            // Also need to use classloader of keycloak services itself to be able to use keycloak classes in the scripts
            if (scriptClassLoader != null) {
                scriptClassLoader = new ProxyClassLoader(scriptClassLoader, DefaultScriptingProvider.class.getClassLoader());
            } else {
                scriptClassLoader = DefaultScriptingProvider.class.getClassLoader();
            }

            logger.debugf("Using classloader %s to load script engine", scriptClassLoader);

            Thread.currentThread().setContextClassLoader(scriptClassLoader);
            return new ScriptEngineManager().getEngineByMimeType(script.getMimeType());
        }
        finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }
}

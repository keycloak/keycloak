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
import org.keycloak.provider.Provider;

import javax.script.ScriptEngine;

/**
 * A {@link Provider} than provides Scripting capabilities.
 *
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
public interface ScriptingProvider extends Provider {

    /**
     * Returns an {@link InvocableScriptAdapter} based on the given {@link ScriptModel}.
     * <p>The {@code InvocableScriptAdapter} wraps a dedicated {@link ScriptEngine} that was populated with the provided {@link ScriptBindingsConfigurer}</p>
     *
     * @param scriptModel        the scriptModel to wrap
     * @param bindingsConfigurer populates the {@link javax.script.Bindings}
     * @return
     */
    InvocableScriptAdapter prepareInvocableScript(ScriptModel scriptModel, ScriptBindingsConfigurer bindingsConfigurer);

    /**
     * Returns an {@link EvaluatableScriptAdapter} based on the given {@link ScriptModel}.
     * <p>The {@code EvaluatableScriptAdapter} wraps a dedicated {@link ScriptEngine} that was populated with empty bindings.</p>
     *
     * @param scriptModel the scriptModel to wrap
     */
    EvaluatableScriptAdapter prepareEvaluatableScript(ScriptModel scriptModel);

    /**
     * Creates a new {@link ScriptModel} instance.
     *
     * @param realmId
     * @param scriptName
     * @param scriptCode
     * @param scriptDescription
     * @return
     */
    ScriptModel createScript(String realmId, String mimeType, String scriptName, String scriptCode, String scriptDescription);
}

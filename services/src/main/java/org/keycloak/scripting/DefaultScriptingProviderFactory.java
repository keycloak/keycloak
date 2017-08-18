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

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import javax.script.ScriptEngineManager;

/**
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
public class DefaultScriptingProviderFactory implements ScriptingProviderFactory {

    static final String ID = "script-based-auth";

    private ScriptEngineManager scriptEngineManager;

    @Override
    public ScriptingProvider create(KeycloakSession session) {
        lazyInit();

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

    private void lazyInit() {
        if (scriptEngineManager == null) {
            synchronized (this) {
                if (scriptEngineManager == null) {
                    scriptEngineManager = new ScriptEngineManager();
                }
            }
        }
    }

}

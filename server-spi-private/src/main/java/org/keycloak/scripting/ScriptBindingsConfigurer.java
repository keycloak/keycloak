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

/**
 * Callback interface for customization of {@link Bindings} for a {@link javax.script.ScriptEngine}.
 * <p>Used by {@link ScriptingProvider}</p>
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
@FunctionalInterface
public interface ScriptBindingsConfigurer {

    /**
     * A default {@link ScriptBindingsConfigurer} that provides no Bindings.
     */
    ScriptBindingsConfigurer EMPTY = new ScriptBindingsConfigurer() {

        @Override
        public void configureBindings(Bindings bindings) {
            //NOOP
        }
    };

    void configureBindings(Bindings bindings);
}
package org.keycloak.scripting;

import javax.script.Bindings;

/**
 * Callback interface for customization of {@link Bindings} for a {@link javax.script.ScriptEngine}.
 *
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
@FunctionalInterface
public interface ScriptBindingsConfigurer {

    /**
     * A default {@link ScriptBindingsConfigurer} leaves the Bindings empty.
     */
    ScriptBindingsConfigurer EMPTY = new ScriptBindingsConfigurer() {

        @Override
        public void configureBindings(Bindings bindings) {
            //NOOP
        }
    };

    void configureBindings(Bindings bindings);
}
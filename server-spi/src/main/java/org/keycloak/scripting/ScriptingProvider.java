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
     * Returns an {@link InvocableScript} based on the given {@link ScriptModel}.
     * <p>The {@code InvocableScript} wraps a dedicated {@link ScriptEngine} that was populated with the provided {@link ScriptBindingsConfigurer}</p>
     *
     * @param script             the script to wrap
     * @param bindingsConfigurer populates the {@link javax.script.Bindings}
     * @return
     */
    InvocableScript prepareScript(ScriptModel script, ScriptBindingsConfigurer bindingsConfigurer);

    /**
     * Returns an {@link InvocableScript} based on the given {@link ScriptModel} with an {@link ScriptBindingsConfigurer#EMPTY} {@code ScriptBindingsConfigurer}.
     * @see #prepareScript(ScriptModel, ScriptBindingsConfigurer)
     * @param script
     * @return
     */
    InvocableScript prepareScript(ScriptModel script);
}

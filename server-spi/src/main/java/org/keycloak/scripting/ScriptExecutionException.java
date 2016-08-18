package org.keycloak.scripting;

import org.keycloak.models.ScriptModel;

import javax.script.ScriptException;

/**
 * Augments a {@link ScriptException} and adds additional metadata.
 *
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
public class ScriptExecutionException extends RuntimeException {

    public ScriptExecutionException(ScriptModel script, ScriptException se) {
        super("Error executing script '" + script.getName() + "'", se);
    }
}

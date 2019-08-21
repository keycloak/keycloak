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
package org.keycloak.authentication.authenticators.browser;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.ScriptModel;
import org.keycloak.models.UserModel;
import org.keycloak.scripting.InvocableScriptAdapter;
import org.keycloak.scripting.ScriptExecutionException;
import org.keycloak.scripting.ScriptingProvider;

import java.util.Map;

/**
 * An {@link Authenticator} that can execute a configured script during authentication flow.
 * <p>
 * Scripts must at least provide one of the following functions:
 * <ol>
 * <li>{@code authenticate(..)} which is called from {@link Authenticator#authenticate(AuthenticationFlowContext)}</li>
 * <li>{@code action(..)} which is called from {@link Authenticator#action(AuthenticationFlowContext)}</li>
 * </ol>
 * </p>
 * <p>
 * Custom {@link Authenticator Authenticator's} should at least provide the {@code authenticate(..)} function.
 * The following script {@link javax.script.Bindings} are available for convenient use within script code.
 * <ol>
 * <li>{@code script} the {@link ScriptModel} to access script metadata</li>
 * <li>{@code realm} the {@link RealmModel}</li>
 * <li>{@code user} the current {@link UserModel}</li>
 * <li>{@code session} the active {@link KeycloakSession}</li>
 * <li>{@code authenticationSession} the current {@link org.keycloak.sessions.AuthenticationSessionModel}</li>
 * <li>{@code httpRequest} the current {@link org.jboss.resteasy.spi.HttpRequest}</li>
 * <li>{@code LOG} a {@link org.jboss.logging.Logger} scoped to {@link ScriptBasedAuthenticator}/li>
 * </ol>
 * </p>
 * <p>
 * Note that the {@code user} variable is only defined when the user was identified by a preceeding
 * authentication step, e.g. by the {@link UsernamePasswordForm} authenticator.
 * </p>
 * <p>
 * Additional context information can be extracted from the {@code context} argument passed to the {@code authenticate(context)}
 * or {@code action(context)} function.
 * <p>
 * An example {@link ScriptBasedAuthenticator} definition could look as follows:
 * <pre>
 * {@code
 *
 *   AuthenticationFlowError = Java.type("org.keycloak.authentication.AuthenticationFlowError");
 *
 *   function authenticate(context) {
 *
 *     var username = user ? user.username : "anonymous";
 *     LOG.info(script.name + " --> trace auth for: " + username);
 *
 *     if (   username === "tester"
 *         && user.getAttribute("someAttribute")
 *         && user.getAttribute("someAttribute").contains("someValue")) {
 *
 *         context.failure(AuthenticationFlowError.INVALID_USER);
 *         return;
 *     }
 *
 *     context.success();
 *   }
 * }
 * </pre>
 *
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
public class ScriptBasedAuthenticator implements Authenticator {

    private static final Logger LOGGER = Logger.getLogger(ScriptBasedAuthenticator.class);

    static final String SCRIPT_CODE = "scriptCode";
    static final String SCRIPT_NAME = "scriptName";
    static final String SCRIPT_DESCRIPTION = "scriptDescription";

    static final String ACTION_FUNCTION_NAME = "action";
    static final String AUTHENTICATE_FUNCTION_NAME = "authenticate";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        tryInvoke(AUTHENTICATE_FUNCTION_NAME, context);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        tryInvoke(ACTION_FUNCTION_NAME, context);
    }

    private void tryInvoke(String functionName, AuthenticationFlowContext context) {

        if (!hasAuthenticatorConfig(context)) {
            // this is an empty not yet configured script authenticator
            // we mark this execution as success to not lock out users due to incompletely configured authenticators.
            context.success();
            return;
        }

        InvocableScriptAdapter invocableScriptAdapter = getInvocableScriptAdapter(context);

        if (!invocableScriptAdapter.isDefined(functionName)) {
            return;
        }

        try {
            //should context be wrapped in a read-only wrapper?
            invocableScriptAdapter.invokeFunction(functionName, context);
        } catch (ScriptExecutionException e) {
            LOGGER.error(e);
            context.failure(AuthenticationFlowError.INTERNAL_ERROR);
        }
    }

    private boolean hasAuthenticatorConfig(AuthenticationFlowContext context) {
        if (context == null)
            return false;
        AuthenticatorConfigModel config = getAuthenticatorConfig(context);
        return config != null
                && config.getConfig() != null
                && !config.getConfig().isEmpty();
    }

    protected AuthenticatorConfigModel getAuthenticatorConfig(AuthenticationFlowContext context) {
        return context.getAuthenticatorConfig();
    }

    private InvocableScriptAdapter getInvocableScriptAdapter(AuthenticationFlowContext context) {

        Map<String, String> config = getAuthenticatorConfig(context).getConfig();

        String scriptName = config.get(SCRIPT_NAME);
        String scriptCode = config.get(SCRIPT_CODE);
        String scriptDescription = config.get(SCRIPT_DESCRIPTION);

        RealmModel realm = context.getRealm();

        ScriptingProvider scripting = context.getSession().getProvider(ScriptingProvider.class);

        //TODO lookup script by scriptId instead of creating it every time
        ScriptModel script = scripting.createScript(realm.getId(), ScriptModel.TEXT_JAVASCRIPT, scriptName, scriptCode, scriptDescription);

        //how to deal with long running scripts -> timeout?
        return scripting.prepareInvocableScript(script, bindings -> {
            bindings.put("script", script);
            bindings.put("realm", context.getRealm());
            bindings.put("user", context.getUser());
            bindings.put("session", context.getSession());
            bindings.put("httpRequest", context.getHttpRequest());
            bindings.put("authenticationSession", context.getAuthenticationSession());
            bindings.put("LOG", LOGGER);
        });
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        //TODO make RequiredActions configurable in the script
        //NOOP
    }

    @Override
    public void close() {
        //NOOP
    }
}

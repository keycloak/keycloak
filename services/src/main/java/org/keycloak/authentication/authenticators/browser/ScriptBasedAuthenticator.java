package org.keycloak.authentication.authenticators.browser;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.scripting.InvocableScript;
import org.keycloak.scripting.Script;
import org.keycloak.scripting.ScriptBindingsConfigurer;
import org.keycloak.scripting.ScriptingProvider;

import javax.script.Bindings;
import javax.script.ScriptException;
import java.util.Map;

/**
 * An {@link Authenticator} that can execute a configured script during authentication flow.
 * <p>scripts must provide </p>
 *
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
public class ScriptBasedAuthenticator implements Authenticator {

    private static final Logger LOGGER = Logger.getLogger(ScriptBasedAuthenticator.class);

    static final String SCRIPT_CODE = "scriptCode";
    static final String SCRIPT_NAME = "scriptName";
    static final String SCRIPT_DESCRIPTION = "scriptDescription";

    static final String ACTION = "action";
    static final String AUTHENTICATE = "authenticate";
    static final String TEXT_JAVASCRIPT = "text/javascript";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        tryInvoke(AUTHENTICATE, context);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        tryInvoke(ACTION, context);
    }

    private void tryInvoke(String functionName, AuthenticationFlowContext context) {

        InvocableScript script = getInvocableScript(context);

        if (!script.hasFunction(functionName)) {
            return;
        }

        try {
            //should context be wrapped in a readonly wrapper?
            script.invokeFunction(functionName, context);
        } catch (ScriptException | NoSuchMethodException e) {
            LOGGER.error(e);
        }
    }

    private InvocableScript getInvocableScript(final AuthenticationFlowContext context) {

        final Script script = createAdhocScriptFromContext(context);

        ScriptBindingsConfigurer bindingsConfigurer = new ScriptBindingsConfigurer() {

            @Override
            public void configureBindings(Bindings bindings) {

                bindings.put("script", script);
                bindings.put("LOG", LOGGER);
            }
        };

        ScriptingProvider scripting = context.getSession().scripting();

        //how to deal with long running scripts -> timeout?

        return scripting.prepareScript(script, bindingsConfigurer);
    }

    private Script createAdhocScriptFromContext(AuthenticationFlowContext context) {

        Map<String, String> config = context.getAuthenticatorConfig().getConfig();

        String scriptName = config.get(SCRIPT_NAME);
        String scriptCode = config.get(SCRIPT_CODE);
        String scriptDescription = config.get(SCRIPT_DESCRIPTION);

        RealmModel realm = context.getRealm();

        return new Script(null /* scriptId */, realm.getId(), scriptName, TEXT_JAVASCRIPT, scriptCode, scriptDescription);
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        //NOOP
    }

    @Override
    public void close() {
        //NOOP
    }
}

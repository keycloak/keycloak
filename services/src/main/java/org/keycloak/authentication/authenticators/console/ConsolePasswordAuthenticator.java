package org.keycloak.authentication.authenticators.console;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.ConsoleDisplayMode;

import javax.ws.rs.core.MultivaluedMap;

public final class ConsolePasswordAuthenticator extends ConsoleUsernamePasswordAuthenticator {

    public static ConsolePasswordAuthenticator SINGLETON = new ConsolePasswordAuthenticator();

    @Override
    protected ConsoleDisplayMode challenge(AuthenticationFlowContext context) {
        return ConsoleDisplayMode.challenge(context)
                .header()
                .param("password")
                .label("console-password")
                .mask(true)
                .challenge();
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        if (!validatePassword(context, context.getUser(), formData, false)) {
            return;
        }
        context.success();
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

}

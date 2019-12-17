package org.keycloak.authentication.authenticators.console;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.ConsoleDisplayMode;

import javax.ws.rs.core.MultivaluedMap;

public final class ConsoleUsernameAuthenticator extends ConsoleUsernamePasswordAuthenticator {
    public static ConsoleUsernameAuthenticator SINGLETON = new ConsoleUsernameAuthenticator();

    @Override
    protected ConsoleDisplayMode challenge(AuthenticationFlowContext context) {
        return ConsoleDisplayMode.challenge(context)
                .header()
                .param("username")
                .label("console-username")
                .mask(true)
                .challenge();
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        if (!validateUser(context, formData)) {
            return;
        }
        context.success();
    }
}

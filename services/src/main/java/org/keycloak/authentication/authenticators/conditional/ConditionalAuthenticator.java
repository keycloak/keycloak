package org.keycloak.authentication.authenticators.conditional;

import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.events.Errors;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.services.messages.Messages;

import javax.ws.rs.core.Response;

public interface ConditionalAuthenticator extends Authenticator {
    boolean matchCondition(AuthenticationFlowContext context);

    default boolean shouldAuthenticate() {
        return true;
    }

    default String getErrorMessage() {
        return Messages.PRECONDITION_FAILED;
    }

    default void authenticate(AuthenticationFlowContext context) {
        if (!shouldAuthenticate()) {
            return;
        }

        if (!matchCondition(context)) {
            context.getEvent().error(Errors.PRECONDITION_FAILED);
            Response challenge = context.form()
                    .setError(getErrorMessage())
                    .createErrorPage(Response.Status.UNAUTHORIZED);
            context.failure(AuthenticationFlowError.PRECONDITION_FAILED, challenge);
            return;
        }
        context.success();
    }

    default boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }
}

AuthenticationFlowError = Java.type("org.keycloak.authentication.AuthenticationFlowError");

function authenticate(context) {

    if (clientSession.getRealm().getName() != "${realm}") {
        context.failure(AuthenticationFlowError.INVALID_CLIENT_SESSION);
        return;
    }

    if (clientSession.getClient().getClientId() != "${clientId}") {
        context.failure(AuthenticationFlowError.UNKNOWN_CLIENT);
        return;
    }

    if (clientSession.getAuthMethod() != "${authMethod}") {
        context.failure(AuthenticationFlowError.INVALID_CLIENT_SESSION);
        return;
    }

    context.success();
}
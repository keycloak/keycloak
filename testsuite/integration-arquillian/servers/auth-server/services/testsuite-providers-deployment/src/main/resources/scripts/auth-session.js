AuthenticationFlowError = Java.type("org.keycloak.authentication.AuthenticationFlowError");

function authenticate(context) {

    if (authenticationSession.getRealm().getName() != "test") {
        context.failure(AuthenticationFlowError.INVALID_CLIENT_SESSION);
        return;
    }

    if (authenticationSession.getClient().getClientId() != "test-app") {
        context.failure(AuthenticationFlowError.UNKNOWN_CLIENT);
        return;
    }

    if (authenticationSession.getProtocol() != "openid-connect") {
        context.failure(AuthenticationFlowError.INVALID_CLIENT_SESSION);
        return;
    }

    context.success();
}
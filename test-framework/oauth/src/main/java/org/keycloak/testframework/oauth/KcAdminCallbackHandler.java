package org.keycloak.testframework.oauth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.keycloak.constants.AdapterConstants;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.representations.adapters.action.LogoutAction;
import org.keycloak.representations.adapters.action.PushNotBeforeAction;
import org.keycloak.representations.adapters.action.TestAvailabilityAction;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class KcAdminCallbackHandler implements HttpHandler {

    private final KcAdminInvocations invocations;

    KcAdminCallbackHandler(KcAdminInvocations kcAdminInvocations) {
        this.invocations = kcAdminInvocations;
    }

    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        try {
            JWSInput adminToken = new JWSInput(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            if (path.endsWith(AdapterConstants.K_LOGOUT)) {
                invocations.add(adminToken.readJsonContent(LogoutAction.class));
            } else if (path.endsWith(AdapterConstants.K_PUSH_NOT_BEFORE)) {
                invocations.add(adminToken.readJsonContent(PushNotBeforeAction.class));
            } else if (path.endsWith(AdapterConstants.K_TEST_AVAILABLE)) {
                invocations.add(adminToken.readJsonContent(TestAvailabilityAction.class));
            }
            exchange.sendResponseHeaders(204, 0);
            exchange.close();
        } catch (JWSInputException e) {
            throw new IOException(e);
        }
    }

}

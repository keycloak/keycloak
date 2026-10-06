package org.keycloak.testframework.oauth;

import java.io.IOException;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.UriUtils;
import org.keycloak.representations.LogoutToken;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

class FrontChannelLogoutHandler implements HttpHandler {

    private final KcAdminInvocations invocations;

    FrontChannelLogoutHandler(KcAdminInvocations invocations) {
        this.invocations = invocations;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        MultivaluedHashMap<String, String> params = UriUtils.decodeQueryString(exchange.getRequestURI().getRawQuery());

        LogoutToken token = new LogoutToken();
        token.setSid(params.getFirst("sid"));
        token.issuer(params.getFirst("iss"));
        invocations.add(token);

        exchange.sendResponseHeaders(200, 0);
        exchange.close();
    }
}

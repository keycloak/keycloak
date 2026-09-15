package org.keycloak.testframework.oauth;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.OAuth2Constants;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.protocol.oidc.grants.ciba.CibaGrantType;
import org.keycloak.protocol.oidc.grants.ciba.channel.AuthenticationChannelRequest;
import org.keycloak.protocol.oidc.grants.ciba.channel.HttpAuthenticationChannelProvider;
import org.keycloak.protocol.oidc.grants.ciba.endpoints.ClientNotificationEndpointRequest;
import org.keycloak.representations.AccessToken;
import org.keycloak.util.JsonSerialization;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 *
 * @author rmartinc
 */
public class CibaProvider implements Closeable {

    public static final String CONTEXT_REQUEST_AUTH_CHANNEL = "/ciba/request-authentication-channel";
    public static final String CONTEXT_PUSH_NOTIFICATION = "/ciba/push-ciba-client-notification";
    public static final String DUMMY_KEY = "channel_request_dummy_key";

    private final HttpServer httpServer;
    private final ConcurrentMap<String, CibaAuthenticationChannelRequest> authenticationChannelRequests = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ClientNotificationEndpointRequest> cibaClientNotifications = new ConcurrentHashMap<>();

    public CibaProvider(HttpServer httpServer) {
        this.httpServer = httpServer;
        httpServer.createContext(CONTEXT_REQUEST_AUTH_CHANNEL, (HttpExchange exchange) -> this.handleRequestAuthChannel(exchange));
        httpServer.createContext(CONTEXT_PUSH_NOTIFICATION, (HttpExchange exchange) -> this.handlePushNotification(exchange));
    }

    public CibaAuthenticationChannelRequest getAuthChannel(String bindingMessage) {
        if (bindingMessage == null) {
            bindingMessage = DUMMY_KEY;
        }
        return authenticationChannelRequests.get(bindingMessage);
    }

    public ClientNotificationEndpointRequest getPushedCibaClientNotification(String clientNotificationToken) {
        ClientNotificationEndpointRequest request = cibaClientNotifications.remove(clientNotificationToken);
        if (request == null) {
            request = new ClientNotificationEndpointRequest();
        }
        return request;
    }

    @Override
    public void close() {
        httpServer.removeContext(CONTEXT_REQUEST_AUTH_CHANNEL);
    }

    private int handleRequestAuthChannel(HttpExchange exchange) throws IOException {
        String token = extractTokenStringFromAuthHeader(exchange.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        if (token == null) {
            return errorRespose(exchange, Status.BAD_REQUEST.getStatusCode(), "Failed to parse bearer token");
        }

        AccessToken bearerToken;
        try {
            bearerToken = new JWSInput(token).readJsonContent(AccessToken.class);
        } catch (JWSInputException e) {
            return errorRespose(exchange, Status.BAD_REQUEST.getStatusCode(), "Failed to parse bearer token");
        }

        // required
        String authenticationChannelId = bearerToken.getId();
        if (authenticationChannelId == null) {
            return errorRespose(exchange, Status.BAD_REQUEST.getStatusCode(), "missing parameter : " + HttpAuthenticationChannelProvider.AUTHENTICATION_CHANNEL_ID);
        }

        // parse the content to receive the request
        AuthenticationChannelRequest request;
        try (InputStream is = exchange.getRequestBody()) {
            request = JsonSerialization.readValue(is, AuthenticationChannelRequest.class);
        } catch (IOException e) {
            return errorRespose(exchange, Status.BAD_REQUEST.getStatusCode(), "Invalid request");
        }

        String loginHint = request.getLoginHint();
        if (loginHint == null) {
            return errorRespose(exchange, Status.BAD_REQUEST.getStatusCode(), "missing parameter : " + CibaGrantType.LOGIN_HINT);
        }

        if (request.getConsentRequired() == null) {
            return errorRespose(exchange, Status.BAD_REQUEST.getStatusCode(), "missing parameter : " + CibaGrantType.IS_CONSENT_REQUIRED);
        }

        String scope = request.getScope();
        if (scope == null) {
            return errorRespose(exchange, Status.BAD_REQUEST.getStatusCode(), "missing parameter : " + OAuth2Constants.SCOPE);
        }

        // optional
        // for testing purpose
        String bindingMessage = request.getBindingMessage();
        if (bindingMessage != null && bindingMessage.equals("GODOWN")) {
            return errorRespose(exchange, Status.BAD_REQUEST.getStatusCode(), "intentional error : GODOWN");
        }

        // binding_message is optional so that it can be null .
        // only one CIBA flow without binding_message can be accepted per test method by this test mechanism.
        if (bindingMessage == null) {
            bindingMessage = DUMMY_KEY;
        }

        authenticationChannelRequests.put(bindingMessage, new CibaAuthenticationChannelRequest(request, token));

        exchange.sendResponseHeaders(Status.CREATED.getStatusCode(), -1);
        return Status.CREATED.getStatusCode();
    }

    private int handlePushNotification(HttpExchange exchange) throws IOException {
        String token = extractTokenStringFromAuthHeader(exchange.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        if (token == null) {
            return errorRespose(exchange, Status.BAD_REQUEST.getStatusCode(), "Failed to parse bearer token");
        }

        // parse the content to receive the request
        ClientNotificationEndpointRequest request;
        try (InputStream is = exchange.getRequestBody()) {
            request = JsonSerialization.readValue(is, ClientNotificationEndpointRequest.class);
        } catch (IOException e) {
            return errorRespose(exchange, Status.BAD_REQUEST.getStatusCode(), "Invalid request");
        }

        ClientNotificationEndpointRequest existing = cibaClientNotifications.putIfAbsent(token, request);
        if (existing != null) {
            return errorRespose(exchange, Status.BAD_REQUEST.getStatusCode(), "There is already entry for clientNotification "
                    + token + ". Make sure to cleanup after previous tests.");
        }

        exchange.sendResponseHeaders(Status.NO_CONTENT.getStatusCode(), -1);
        return Status.NO_CONTENT.getStatusCode();
    }

    private int errorRespose(HttpExchange exchange, int code, String message) throws IOException {
        byte[] responseBytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(HttpHeaders.CONTENT_TYPE, "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(code, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
        return code;
    }

    private String extractTokenStringFromAuthHeader(String authHeader) {
        if (authHeader == null) {
            return null;
        }

        int indexOfSpace = authHeader.indexOf(' ');

        if (indexOfSpace <= 0) {
            return null;
        }

        return authHeader.substring(indexOfSpace + 1);
    }

    public static class CibaAuthenticationChannelRequest {

        private String bearerToken;
        private AuthenticationChannelRequest request;

        public CibaAuthenticationChannelRequest(AuthenticationChannelRequest request, String bearerToken) {
            this.request = request;
            this.bearerToken = bearerToken;
        }

        public void setBearerToken(String bearerToken) {
            this.bearerToken = bearerToken;
        }

        public String getBearerToken() {
            return bearerToken;
        }

        public void setRequest(AuthenticationChannelRequest request) {
            this.request = request;
        }

        public AuthenticationChannelRequest getRequest() {
            return request;
        }
    }
}

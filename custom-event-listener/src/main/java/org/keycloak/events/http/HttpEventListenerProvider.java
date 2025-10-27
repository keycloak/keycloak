package org.keycloak.events.http;

import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * HTTP Event Listener that sends events to a configurable HTTP endpoint
 */
public class HttpEventListenerProvider implements EventListenerProvider {

    private static final Logger log = Logger.getLogger(HttpEventListenerProvider.class);
    
    private final String targetUrl;
    private final String authToken;

    public HttpEventListenerProvider(String targetUrl, String authToken) {
        this.targetUrl = targetUrl;
        this.authToken = authToken;
    }

    @Override
    public void onEvent(Event event) {
        log.infof("Received event: %s for user: %s", event.getType(), event.getUserId());
        
        // Build JSON payload
        String json = buildEventJson(event);
        
        // Send to HTTP endpoint
        sendHttpRequest(json);
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
        log.infof("Received admin event: %s", adminEvent.getOperationType());
        
        // Build JSON payload for admin event
        String json = buildAdminEventJson(adminEvent);
        
        // Send to HTTP endpoint
        sendHttpRequest(json);
    }

    private String buildEventJson(Event event) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"type\":\"").append(escapeJson(event.getType().toString())).append("\",");
        json.append("\"realmId\":\"").append(escapeJson(event.getRealmId())).append("\",");
        json.append("\"realmName\":\"").append(escapeJson(event.getRealmName())).append("\",");
        json.append("\"clientId\":\"").append(escapeJson(event.getClientId())).append("\",");
        json.append("\"userId\":\"").append(escapeJson(event.getUserId())).append("\",");
        json.append("\"ipAddress\":\"").append(escapeJson(event.getIpAddress())).append("\",");
        json.append("\"time\":").append(event.getTime());
        
        if (event.getError() != null) {
            json.append(",\"error\":\"").append(escapeJson(event.getError())).append("\"");
        }
        
        if (event.getDetails() != null && !event.getDetails().isEmpty()) {
            json.append(",\"details\":{");
            boolean first = true;
            for (var entry : event.getDetails().entrySet()) {
                if (!first) json.append(",");
                json.append("\"").append(escapeJson(entry.getKey())).append("\":\"")
                    .append(escapeJson(entry.getValue())).append("\"");
                first = false;
            }
            json.append("}");
        }
        
        json.append("}");
        return json.toString();
    }

    private String buildAdminEventJson(AdminEvent adminEvent) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"operationType\":\"").append(escapeJson(adminEvent.getOperationType().toString())).append("\",");
        json.append("\"realmId\":\"").append(escapeJson(adminEvent.getAuthDetails().getRealmId())).append("\",");
        json.append("\"resourceType\":\"").append(escapeJson(adminEvent.getResourceTypeAsString())).append("\",");
        json.append("\"resourcePath\":\"").append(escapeJson(adminEvent.getResourcePath())).append("\",");
        json.append("\"time\":").append(adminEvent.getTime());
        
        if (adminEvent.getError() != null) {
            json.append(",\"error\":\"").append(escapeJson(adminEvent.getError())).append("\"");
        }
        
        json.append("}");
        return json.toString();
    }

    private void sendHttpRequest(String jsonPayload) {
        if (targetUrl == null || targetUrl.isEmpty()) {
            log.warn("HTTP Event Listener: No target URL configured");
            return;
        }

        HttpURLConnection connection = null;
        try {
            URL url = new URL(targetUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            
            if (authToken != null && !authToken.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + authToken);
            }
            
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                log.infof("Successfully sent event to %s (response: %d)", targetUrl, responseCode);
            } else {
                log.warnf("Failed to send event to %s (response: %d)", targetUrl, responseCode);
            }

        } catch (IOException e) {
            log.errorf(e, "Error sending event to HTTP endpoint: %s", targetUrl);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    @Override
    public void close() {
        // Nothing to close
    }
}


package org.keycloak.testframework.oauth;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.keycloak.OAuth2Constants;
import org.keycloak.common.util.HtmlUtils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.http.client.methods.HttpPost;

class OAuthCallbackHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String requestMethod = exchange.getRequestMethod();
        String requestUriParam = exchange.getRequestURI().getQuery();

        byte[] bytes = htmlResponseStringBuilder(requestBody, requestMethod, requestUriParam);

        sendHttpResponse(exchange, bytes);
    }

    private void sendHttpResponse(HttpExchange exchange, byte[] bytes) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/html");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }

    private byte[] htmlResponseStringBuilder(String requestBody, String requestMethod, String requestUriParam) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        if (requestMethod.equals(HttpPost.METHOD_NAME)) {
            formPostStringBuilder(sb, requestBody);
        } else {
            if (requestUriParam != null && requestUriParam.contains(OAuth2Constants.CODE)) {
                sb.append("<b>Happy days!</b><br>");
            } else {
                sb.append("<b>Not happy days!</b><br>");
            }
        }
        sb.append("<br>");
        sb.append("</body></html>");

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void formPostStringBuilder(StringBuilder sb, String requestBody) {
        String requestBodyDecoded = URLDecoder.decode(requestBody, StandardCharsets.UTF_8);
        Map<String, String> formPostParams = Arrays.stream(requestBodyDecoded.split("&"))
                .collect(Collectors.toMap(s -> s.split("=", 2)[0], s -> s.split("=", 2)[1]));

        if (!formPostParams.containsKey(OAuth2Constants.ERROR)) {
            sb.append("<b>Happy days!</b><br>");
        } else {
            sb.append("<b>Not happy days!</b><br>");
        }
        sb.append("<br>");
        sb.append("<b>Form POST parameters: </b><br>");
        for (String paramName : formPostParams.keySet()) {
            sb.append(paramName).append(": ").append("<span id=\"")
                    .append(paramName).append("\">")
                    .append(HtmlUtils.escapeAttribute(formPostParams.get(paramName)))
                    .append("</span><br>");
        }
    }
}

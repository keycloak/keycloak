package org.keycloak.testsuite.util.javascript;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
import org.keycloak.util.JsonSerialization;
import org.openqa.selenium.JavascriptExecutor;

import java.util.HashMap;
import java.util.Map;

/**
 * @author mhajas
 */
public class XMLHttpRequest {

    private String url;
    private String method;
    private Map<String, String> headers;
    private String content;
    private boolean withCredentials;
    private boolean jsonResponse;

    public static XMLHttpRequest create() {
        return new XMLHttpRequest();
    }

    private XMLHttpRequest() {}

    public XMLHttpRequest url(String url) {
        this.url = url;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public XMLHttpRequest method(String method) {
        this.method = method;
        return this;
    }

    public XMLHttpRequest content(String content) {
        this.content = content;
        return this;
    }

    public XMLHttpRequest addHeader(String key, String value) {
        if (headers == null) {
            headers = new HashMap<>();
        }
        headers.put(key, value);
        return this;
    }

    public XMLHttpRequest includeBearerToken() {
        addHeader("Authorization", "Bearer ' + keycloak.token + '");
        return this;
    }

    public XMLHttpRequest includeRpt() {
        addHeader("Authorization", "Bearer ' + authorization.rpt + '");
        return this;
    }

    public XMLHttpRequest withCredentials() {
        this.withCredentials = true;
        return this;
    }

    public XMLHttpRequest jsonResponse() {
        this.jsonResponse = true;
        addHeader("Accept", "application/json");
        return this;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> send(JavascriptExecutor jsExecutor) {

        String requestCode = "var callback = arguments[arguments.length - 1];" +
                "var req = new XMLHttpRequest();" +
                "        req.open('" + method + "', '" + url + "', true);" +
                buildHeadersString() +
                buildWithCredentialsString() +
                "        req.onreadystatechange = function () {" +
                "            if (req.readyState == 4) {" +
                "                callback({\"status\" : req.status, \"responseHeaders\" : req.getAllResponseHeaders(), \"res\" : req.response})" +
                "            }" +
                "        };" +
                "        req.send(" + content + ");";

        Map<String, Object> res = (Map<String, Object>) jsExecutor.executeAsyncScript(requestCode);
        return jsonResponse ? parseJsonResponse(res) : res;

    }

    private String buildHeadersString() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            builder.append("req.setRequestHeader('")
                    .append(entry.getKey())
                    .append("', '")
                    .append(entry.getValue())
                    .append("');");
        }
        return builder.toString();
    }

    private String buildWithCredentialsString() {
        return withCredentials ? "req.withCredentials = true;" : "";
    }

    private Map<String, Object> parseJsonResponse(Map<String, Object> res) {
        try {
            String jsonString = (String) res.get("res");
            Map<String, Object> json = JsonSerialization.readValue(jsonString, new TypeReference<Map<String, Object>>() {});
            // create new immutable map because input map is also immutable and cannot be modified directly
            ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
            return builder.putAll(res).put("json", json).build();
        } catch (Exception e) {
            return res;
        }
    }

}

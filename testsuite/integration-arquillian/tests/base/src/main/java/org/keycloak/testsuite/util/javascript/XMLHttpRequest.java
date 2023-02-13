package org.keycloak.testsuite.util.javascript;

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

    public Map<String, Object> send(JavascriptExecutor jsExecutor) {
        String requestCode = "var callback = arguments[arguments.length - 1];" +
                        "var req = new XMLHttpRequest();" +
                        "        req.open('" + method + "', '" + url + "', true);" +
                        getHeadersString() +
                        "        req.onreadystatechange = function () {" +
                        "            if (req.readyState == 4) {" +
                        "                callback({\"status\" : req.status, \"responseHeaders\" : req.getAllResponseHeaders(), \"res\" : req.response})" +
                        "            }" +
                        "        };" +
                        "        req.send(" + content + ");";

        return (Map<String, Object>) jsExecutor.executeAsyncScript(requestCode);
    }

    private String getHeadersString() {
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

}

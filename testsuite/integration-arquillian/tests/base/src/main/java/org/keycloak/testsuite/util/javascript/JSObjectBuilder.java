package org.keycloak.testsuite.util.javascript;

import java.util.HashMap;
import java.util.Map;

/**
 * @author mhajas
 */
public class JSObjectBuilder {

    private Map<String, Object> arguments;


    public static JSObjectBuilder create() {
        return new JSObjectBuilder();
    }

    private JSObjectBuilder() {
        arguments = new HashMap<>();
    }

    public JSObjectBuilder defaultSettings() {
        standardFlow();
        fragmentResponse();
        return this;
    }

    public JSObjectBuilder standardFlow() {
        arguments.put("flow", "standard");
        return this;
    }

    public JSObjectBuilder implicitFlow() {
        arguments.put("flow", "implicit");
        return this;
    }

    public JSObjectBuilder fragmentResponse() {
        arguments.put("responseMode", "fragment");
        return this;
    }

    public JSObjectBuilder queryResponse() {
        arguments.put("responseMode", "query");
        return this;
    }

    public JSObjectBuilder checkSSOOnLoad() {
        arguments.put("onLoad", "check-sso");
        return this;
    }

    public JSObjectBuilder disableCheckLoginIframe() {
        arguments.put("checkLoginIframe", false);
        return this;
    }

    public JSObjectBuilder loginRequiredOnLoad() {
        arguments.put("onLoad", "login-required");
        return this;
    }

    public boolean contains(String key, Object value) {
       return arguments.containsKey(key) && arguments.get(key).equals(value);
    }

    public JSObjectBuilder add(String key, Object value) {
        arguments.put(key, value);
        return this;
    }

    public boolean isLoginRequired() {
        return arguments.get("onLoad").equals("login-required");
    }


    public JSObjectBuilder pkceS256() {
        return pkceMethod("S256");
    }

    private JSObjectBuilder pkceMethod(String method) {
        arguments.put("pkceMethod", method);
        return this;
    }

    private boolean skipQuotes(Object o) {
        return (o instanceof Integer || o instanceof Boolean);
    }

    public String build() {
        StringBuilder argument = new StringBuilder("{");
        String comma = "";
        for (Map.Entry<String, Object> option : arguments.entrySet()) {
            argument.append(comma)
                    .append(option.getKey())
                    .append(" : ");

            if (!skipQuotes(option.getValue())) argument.append("\"");

            argument.append(option.getValue());

            if (!skipQuotes(option.getValue())) argument.append("\"");
            comma = ",";
        }

        argument.append("}");

        return argument.toString();
    }


}

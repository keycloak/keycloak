package org.keycloak.testsuite;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.ws.rs.core.UriBuilder;

public class ActionURIUtils {

    private static final Pattern ACTION_URI_PATTERN = Pattern.compile("action=\"([^\"]+)\"");
    private static final Pattern QUERY_STRING_PATTERN = Pattern.compile("[^\\?]+\\?([^#]+).*");
    private static final Pattern PARAMS_PATTERN = Pattern.compile("[=\\&]");

    public static String getActionURIFromPageSource(String htmlPageSource) {
        Matcher m = ACTION_URI_PATTERN.matcher(htmlPageSource);
        if (m.find()) {
            return m.group(1).replaceAll("&amp;", "&");
        }
        return null;
    }

    public static Map<String, String> parseQueryParamsFromActionURI(String actionURI) {
        Matcher m = QUERY_STRING_PATTERN.matcher(actionURI);
        if (m.find()) {
            String queryString = m.group(1);

            String[] params = PARAMS_PATTERN.split(queryString, 0);
            Map<String, String> result = new HashMap<>();

            for (int i = 0; i < params.length; i += 2) {
                String paramName = params[i];
                String paramValue = params[i + 1];
                result.put(paramName, paramValue);
            }
            return result;
        }
        return Collections.emptyMap();
    }

    public static String removeQueryParamFromURI(String actionURI, String paramName) {
        return UriBuilder.fromUri(actionURI)
                .replaceQueryParam(paramName, (Object[]) null)
                .build()
                .toString();
    }
}

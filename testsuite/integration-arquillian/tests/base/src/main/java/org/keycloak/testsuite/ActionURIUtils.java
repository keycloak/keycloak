/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testsuite;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.ws.rs.core.UriBuilder;

/**
 * Helper for parse action-uri from the HTML login page and do something with it (eg. open in new browser, parse code parameter and use it somewhere else etc)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ActionURIUtils {

    private static final Pattern ACTION_URI_PATTERN = Pattern.compile("action=\"([^\"]+)\"");

    private static final Pattern QUERY_STRING_PATTERN = Pattern.compile("[^\\?]+\\?([^#]+).*");

    private static final Pattern PARAMS_PATTERN = Pattern.compile("[=\\&]");

    public static String getActionURIFromPageSource(String htmlPageSource) {
        Matcher m = ACTION_URI_PATTERN.matcher(htmlPageSource);
        if (m.find()) {
            return m.group(1).replaceAll("&amp;", "&");
        } else {
            return null;
        }
    }

    public static Map<String, String> parseQueryParamsFromActionURI(String actionURI) {
        Matcher m = QUERY_STRING_PATTERN.matcher(actionURI);
        if (m.find()) {
            String queryString = m.group(1);

            String[] params = PARAMS_PATTERN.split(queryString, 0);
            Map<String, String> result = new HashMap<>(); // Don't take multivalued into account for now

            for (int i=0 ; i<params.length ; i+=2) {
                String paramName = params[i];
                String paramValue = params[i+1];
                result.put(paramName, paramValue);
            }
            return result;
        } else {
            return Collections.emptyMap();
        }
    }

    public static String removeQueryParamFromURI(String actionURI, String paramName) {
        return UriBuilder.fromUri(actionURI)
                .replaceQueryParam(paramName, (Object[]) null)
                .build().toString();
    }


    /*
    private static final String TEST = "<form id=\"kc-form-login\" class=\"form-horizontal\" action=\"http://localhost:8180/auth/realms/child/login-actions/authenticate?code=1WnqOmapgo0cj3mpRQ-vbleIKUJdwFzonzy1fjvnWQQ&amp;execution=3ac92a20-9c31-49de-a3c8-f2a4fff80986&amp;client_id=client-linking\" method=\"post\">";

    public static void main(String[] args) {
        String actionURI = getActionURIFromPageSource(TEST);
        System.out.println("action uri: " + actionURI);

        Map<String, String> params = parseQueryParamsFromActionURI(actionURI);
        System.out.println("params: " + params);

        String actionURI2 = removeQueryParamFromURI(actionURI, "execution");
        System.out.println("action uri 2: " + actionURI2);
    }*/
}

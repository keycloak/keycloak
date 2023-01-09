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

package org.keycloak.adapters.springsecurity.filter;

import org.keycloak.constants.AdapterConstants;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * CSRF protection matcher that allows administrative POST requests from the Keycloak server.
 *
 * @author <a href="mailto:srossillo@smartling.com">Scott Rossillo</a>
 */
public class KeycloakCsrfRequestMatcher implements RequestMatcher {

    private static final List<String> ALLOWED_ENDPOINTS = Arrays.asList(
            AdapterConstants.K_LOGOUT,
            AdapterConstants.K_PUSH_NOT_BEFORE,
            AdapterConstants.K_QUERY_BEARER_TOKEN,
            AdapterConstants.K_TEST_AVAILABLE
    );

    private Pattern allowedMethods = Pattern.compile("^(GET|HEAD|TRACE|OPTIONS)$");
    private Pattern allowedEndpoints = Pattern.compile(String.format("^\\/(%s)$", StringUtils.arrayToDelimitedString(ALLOWED_ENDPOINTS.toArray(), "|")));

    /* (non-Javadoc)
     * @see org.springframework.security.web.util.matcher.RequestMatcher#matches(javax.servlet.http.HttpServletRequest)
     */
    public boolean matches(HttpServletRequest request) {
        String uri = request.getRequestURI().replaceFirst(request.getContextPath(), "");
        return !allowedEndpoints.matcher(uri).matches() && !allowedMethods.matcher(request.getMethod()).matches();
    }
}

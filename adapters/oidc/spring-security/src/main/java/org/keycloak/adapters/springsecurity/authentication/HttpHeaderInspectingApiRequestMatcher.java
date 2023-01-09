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

package org.keycloak.adapters.springsecurity.authentication;

import org.apache.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.http.HttpServletRequest;

/**
 * {@link RequestMatcher} that determines if a given request is an API request or an
 * interactive login request.
 *
 * @author <a href="mailto:srossillo@smartling.com">Scott Rossillo</a>
 * @see RequestMatcher
 */
public class HttpHeaderInspectingApiRequestMatcher implements RequestMatcher {

    protected static final String X_REQUESTED_WITH_HEADER = "X-Requested-With";
    protected static final String X_REQUESTED_WITH_HEADER_AJAX_VALUE = "XMLHttpRequest";

    /**
     * Returns true if the given request is an API request or false if it's an interactive
     * login request.
     *
     * @param request the <code>HttpServletRequest</code>
     * @return <code>true</code> if the given <code>request</code> is an API request;
     * <code>false</code> otherwise
     */
    @Override
    public boolean matches(HttpServletRequest request) {
        return X_REQUESTED_WITH_HEADER_AJAX_VALUE.equals(request.getHeader(X_REQUESTED_WITH_HEADER));
    }

}

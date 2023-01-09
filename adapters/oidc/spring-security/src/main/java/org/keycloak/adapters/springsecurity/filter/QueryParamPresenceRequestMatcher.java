/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Spring RequestMatcher that checks for the presence of a query parameter.
 *
 * @author <a href="mailto:glavoie@gmail.com">Gabriel Lavoie</a>
 */
public class QueryParamPresenceRequestMatcher implements RequestMatcher {
    private String param;

    public QueryParamPresenceRequestMatcher(String param) {
        this.param = param;
    }

    @Override
    public boolean matches(HttpServletRequest httpServletRequest) {
        return param != null && httpServletRequest.getParameter(param) != null;
    }
}
